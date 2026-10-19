using System;
using BazController.Windows.Protocol;

namespace BazController.Windows.Transport
{
    public interface ITransportServer : IDisposable
    {
        string Name { get; }
        bool IsRunning { get; }
        bool IsClientConnected { get; }
        string ClientEndpoint { get; }
        double LatencyMs { get; }
        int PacketsPerSecond { get; }

        event Action<BazPacket> OnPacketReceived;
        event Action<string> OnClientConnected;
        event Action OnClientDisconnected;
        event Action<string> OnError;

        void Start();
        void Stop();
        bool SendPacket(in BazPacket packet);
    }
}
