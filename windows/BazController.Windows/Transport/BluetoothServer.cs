using System;
using System.Diagnostics;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using BazController.Windows.Protocol;
using InTheHand.Net.Bluetooth;
using InTheHand.Net.Sockets;

namespace BazController.Windows.Transport
{
    public class BluetoothServer : ITransportServer
    {
        public string Name => "Bluetooth Classic SPP";
        public bool IsRunning { get; private set; }
        public bool IsClientConnected => _connectedClient != null && _connectedClient.Connected;
        public string ClientEndpoint { get; private set; } = "None";
        public double LatencyMs { get; private set; }
        public int PacketsPerSecond { get; private set; }

        public event Action<BazPacket>? OnPacketReceived;
        public event Action<string>? OnClientConnected;
        public event Action? OnClientDisconnected;
        public event Action<string>? OnError;

        private BluetoothListener? _listener;
        private BluetoothClient? _connectedClient;
        private Stream? _stream;
        private CancellationTokenSource? _cts;

        private int _packetCounter = 0;
        private static readonly Guid SppServiceGuid = new("00001101-0000-1000-8000-00805F9B34FB");

        public void Start()
        {
            if (IsRunning) return;
            try
            {
                _cts = new CancellationTokenSource();
                _listener = new BluetoothListener(SppServiceGuid)
                {
                    ServiceName = "BazController SPP Server"
                };
                _listener.Start();
                IsRunning = true;

                Task.Run(() => AcceptLoopAsync(_cts.Token));
                Task.Run(() => FpsMonitorLoopAsync(_cts.Token));
            }
            catch (Exception ex)
            {
                OnError?.Invoke($"Failed to start Bluetooth SPP: {ex.Message}");
            }
        }

        private async Task AcceptLoopAsync(CancellationToken token)
        {
            while (!token.IsCancellationRequested && _listener != null)
            {
                try
                {
                    var client = _listener.AcceptBluetoothClient();
                    _connectedClient = client;
                    _stream = client.GetStream();
                    ClientEndpoint = client.RemoteMachineName ?? "Android Device";
                    OnClientConnected?.Invoke(ClientEndpoint);

                    await ReceiveLoopAsync(client, _stream, token);
                }
                catch (Exception ex)
                {
                    if (!token.IsCancellationRequested)
                    {
                        OnError?.Invoke($"Bluetooth connection exception: {ex.Message}");
                    }
                }
                finally
                {
                    CleanupClient();
                }
            }
        }

        private async Task ReceiveLoopAsync(BluetoothClient client, Stream stream, CancellationToken token)
        {
            byte[] buffer = new byte[BazPacket.PacketSize * 2];
            int bufferOffset = 0;

            while (!token.IsCancellationRequested && client.Connected)
            {
                int read = await stream.ReadAsync(buffer.AsMemory(bufferOffset, buffer.Length - bufferOffset), token);
                if (read == 0) break;

                bufferOffset += read;

                while (bufferOffset >= BazPacket.PacketSize)
                {
                    if (buffer[0] == BazPacket.Magic0 && buffer[1] == BazPacket.Magic1)
                    {
                        if (BazPacket.TryParse(buffer.AsSpan(0, BazPacket.PacketSize), out var packet))
                        {
                            _packetCounter++;
                            OnPacketReceived?.Invoke(packet);
                        }

                        bufferOffset -= BazPacket.PacketSize;
                        if (bufferOffset > 0)
                        {
                            Buffer.BlockCopy(buffer, BazPacket.PacketSize, buffer, 0, bufferOffset);
                        }
                    }
                    else
                    {
                        bufferOffset--;
                        Buffer.BlockCopy(buffer, 1, buffer, 0, bufferOffset);
                    }
                }
            }
        }

        private async Task FpsMonitorLoopAsync(CancellationToken token)
        {
            while (!token.IsCancellationRequested)
            {
                await Task.Delay(1000, token);
                PacketsPerSecond = _packetCounter;
                _packetCounter = 0;
            }
        }

        public bool SendPacket(in BazPacket packet)
        {
            if (_stream == null || !IsClientConnected) return false;
            try
            {
                byte[] bytes = packet.Serialize();
                _stream.Write(bytes, 0, bytes.Length);
                return true;
            }
            catch
            {
                return false;
            }
        }

        private void CleanupClient()
        {
            try
            {
                _stream?.Dispose();
                _connectedClient?.Close();
            }
            catch { }
            _stream = null;
            _connectedClient = null;
            ClientEndpoint = "None";
            OnClientDisconnected?.Invoke();
        }

        public void Stop()
        {
            IsRunning = false;
            _cts?.Cancel();
            try { _listener?.Stop(); } catch { }
            CleanupClient();
        }

        public void Dispose()
        {
            Stop();
            _cts?.Dispose();
        }
    }
}
