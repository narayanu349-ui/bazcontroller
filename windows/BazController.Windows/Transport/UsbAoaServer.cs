using System;
using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;
using BazController.Windows.Protocol;

namespace BazController.Windows.Transport
{
    public class UsbAoaServer : ITransportServer
    {
        public string Name => "USB Direct / ADB";
        public bool IsRunning { get; private set; }
        public bool IsClientConnected => _connectedClient != null && _connectedClient.Connected;
        public string ClientEndpoint { get; private set; } = "127.0.0.1 (USB)";
        public double LatencyMs { get; private set; }
        public int PacketsPerSecond { get; private set; }

        public event Action<BazPacket>? OnPacketReceived;
        public event Action<string>? OnClientConnected;
        public event Action? OnClientDisconnected;
        public event Action<string>? OnError;

        private TcpListener? _usbListener;
        private TcpClient? _connectedClient;
        private NetworkStream? _stream;
        private CancellationTokenSource? _cts;
        private int _packetCounter = 0;

        public void Start()
        {
            if (IsRunning) return;
            try
            {
                // Auto-forward ADB port if device is attached via USB
                TryConfigureAdbPortForward();

                _cts = new CancellationTokenSource();
                _usbListener = new TcpListener(IPAddress.Loopback, 8992);
                _usbListener.Start();
                IsRunning = true;

                Task.Run(() => AcceptLoopAsync(_cts.Token));
                Task.Run(() => FpsMonitorLoopAsync(_cts.Token));
            }
            catch (Exception ex)
            {
                OnError?.Invoke($"Failed to initialize USB Direct: {ex.Message}");
            }
        }

        private void TryConfigureAdbPortForward()
        {
            try
            {
                var psi = new ProcessStartInfo
                {
                    FileName = "adb",
                    Arguments = "forward tcp:8992 tcp:8990",
                    UseShellExecute = false,
                    CreateNoWindow = true
                };
                using var proc = Process.Start(psi);
                proc?.WaitForExit(1000);
            }
            catch { }
        }

        private async Task AcceptLoopAsync(CancellationToken token)
        {
            while (!token.IsCancellationRequested && _usbListener != null)
            {
                try
                {
                    var client = await _usbListener.AcceptTcpClientAsync(token);
                    client.NoDelay = true;
                    _connectedClient = client;
                    _stream = client.GetStream();
                    OnClientConnected?.Invoke("Android USB Cable (Low Latency)");

                    await ReceiveLoopAsync(client, _stream, token);
                }
                catch (OperationCanceledException) { break; }
                catch (Exception ex)
                {
                    OnError?.Invoke($"USB connection error: {ex.Message}");
                }
                finally
                {
                    Cleanup();
                }
            }
        }

        private async Task ReceiveLoopAsync(TcpClient client, NetworkStream stream, CancellationToken token)
        {
            byte[] buffer = new byte[BazPacket.PacketSize * 4];
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
            catch { return false; }
        }

        private void Cleanup()
        {
            try { _stream?.Dispose(); _connectedClient?.Close(); } catch { }
            _stream = null;
            _connectedClient = null;
            OnClientDisconnected?.Invoke();
        }

        public void Stop()
        {
            IsRunning = false;
            _cts?.Cancel();
            try { _usbListener?.Stop(); } catch { }
            Cleanup();
        }

        public void Dispose()
        {
            Stop();
            _cts?.Dispose();
        }
    }
}
