using System;
using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;
using BazController.Windows.Protocol;

namespace BazController.Windows.Transport
{
    public class WifiTcpServer : ITransportServer
    {
        public string Name => "Wi-Fi TCP Socket";
        public bool IsRunning { get; private set; }
        public bool IsClientConnected => _connectedClient != null && _connectedClient.Connected;
        public string ClientEndpoint { get; private set; } = "None";
        public double LatencyMs { get; private set; }
        public int PacketsPerSecond { get; private set; }

        public event Action<BazPacket>? OnPacketReceived;
        public event Action<string>? OnClientConnected;
        public event Action? OnClientDisconnected;
        public event Action<string>? OnError;

        private TcpListener? _listener;
        private TcpClient? _connectedClient;
        private NetworkStream? _networkStream;
        private CancellationTokenSource? _cts;

        private int _packetCounter = 0;
        private Stopwatch _rateStopwatch = new();
        private readonly int _port;

        public WifiTcpServer(int port = 8990)
        {
            _port = port;
        }

        public void Start()
        {
            if (IsRunning) return;
            try
            {
                _cts = new CancellationTokenSource();
                _listener = new TcpListener(IPAddress.Any, _port);
                _listener.Server.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
                _listener.Start(10);
                IsRunning = true;
                _rateStopwatch.Restart();

                Task.Run(() => AcceptLoopAsync(_cts.Token));
                Task.Run(() => FpsMonitorLoopAsync(_cts.Token));
            }
            catch (Exception ex)
            {
                OnError?.Invoke($"Failed to start TCP server on port {_port}: {ex.Message}");
            }
        }

        private async Task AcceptLoopAsync(CancellationToken token)
        {
            while (!token.IsCancellationRequested && _listener != null)
            {
                try
                {
                    var client = await _listener.AcceptTcpClientAsync(token);
                    client.NoDelay = true;
                    client.ReceiveBufferSize = 64 * 1024;
                    client.SendBufferSize = 64 * 1024;

                    _connectedClient = client;
                    _networkStream = client.GetStream();
                    ClientEndpoint = client.Client.RemoteEndPoint?.ToString() ?? "Unknown";
                    OnClientConnected?.Invoke(ClientEndpoint);

                    await ReceiveLoopAsync(client, _networkStream, token);
                }
                catch (OperationCanceledException)
                {
                    break;
                }
                catch (Exception ex)
                {
                    OnError?.Invoke($"Connection exception: {ex.Message}");
                }
                finally
                {
                    CleanupClient();
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
                if (read == 0) break; // Client disconnected

                bufferOffset += read;

                // Process 64-byte packets
                while (bufferOffset >= BazPacket.PacketSize)
                {
                    // Check magic
                    if (buffer[0] == BazPacket.Magic0 && buffer[1] == BazPacket.Magic1)
                    {
                        if (BazPacket.TryParse(buffer.AsSpan(0, BazPacket.PacketSize), out var packet))
                        {
                            _packetCounter++;

                            // Calculate latency from phone timestamp
                            long nowUs = (long)(Stopwatch.GetTimestamp() * (1_000_000.0 / Stopwatch.Frequency));
                            long deltaUs = nowUs - (long)packet.TimestampUs;
                            if (deltaUs > 0 && deltaUs < 500_000)
                            {
                                LatencyMs = deltaUs / 1000.0;
                            }

                            OnPacketReceived?.Invoke(packet);
                        }

                        // Shift remaining bytes
                        bufferOffset -= BazPacket.PacketSize;
                        if (bufferOffset > 0)
                        {
                            Buffer.BlockCopy(buffer, BazPacket.PacketSize, buffer, 0, bufferOffset);
                        }
                    }
                    else
                    {
                        // Resync: advance 1 byte
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
            if (_networkStream == null || !IsClientConnected) return false;
            try
            {
                byte[] bytes = packet.Serialize();
                _networkStream.Write(bytes, 0, bytes.Length);
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
                _networkStream?.Dispose();
                _connectedClient?.Close();
            }
            catch { }
            _networkStream = null;
            _connectedClient = null;
            ClientEndpoint = "None";
            OnClientDisconnected?.Invoke();
        }

        public void Stop()
        {
            IsRunning = false;
            _cts?.Cancel();
            try
            {
                _listener?.Stop();
            }
            catch { }
            CleanupClient();
        }

        public void Dispose()
        {
            Stop();
            _cts?.Dispose();
        }
    }
}
