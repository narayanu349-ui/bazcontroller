using System;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace BazController.Windows.Transport
{
    public class DiscoveryBeacon : IDisposable
    {
        private UdpClient? _udpClient;
        private CancellationTokenSource? _cts;
        private readonly int _beaconPort;
        private readonly int _tcpServerPort;
        private readonly string _pcName;

        public DiscoveryBeacon(int tcpServerPort = 8990, int beaconPort = 8991)
        {
            _tcpServerPort = tcpServerPort;
            _beaconPort = beaconPort;
            _pcName = Environment.MachineName;
        }

        public void Start()
        {
            _cts = new CancellationTokenSource();
            _udpClient = new UdpClient { EnableBroadcast = true };

            Task.Run(async () =>
            {
                var broadcastEp = new IPEndPoint(IPAddress.Broadcast, _beaconPort);
                string message = $"BAZ_COMPANION_BEACON;{_pcName};{_tcpServerPort}";
                byte[] bytes = Encoding.UTF8.GetBytes(message);

                while (!_cts.Token.IsCancellationRequested)
                {
                    try
                    {
                        await _udpClient.SendAsync(bytes, bytes.Length, broadcastEp);
                        await Task.Delay(2000, _cts.Token);
                    }
                    catch (OperationCanceledException) { break; }
                    catch { }
                }
            });
        }

        public void Stop()
        {
            _cts?.Cancel();
            _udpClient?.Close();
            _udpClient = null;
        }

        public void Dispose()
        {
            Stop();
            _cts?.Dispose();
        }
    }
}
