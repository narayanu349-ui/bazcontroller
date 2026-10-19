using System;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows;
using BazController.Windows.Profiles;
using BazController.Windows.Protocol;
using BazController.Windows.Transport;
using BazController.Windows.VirtualController;

namespace BazController.Windows.ViewModels
{
    public class MainViewModel : INotifyPropertyChanged
    {
        private readonly ViGEmControllerManager _virtualController;
        private readonly PacketSecurity _security;
        private ITransportServer _activeTransport;
        private readonly DiscoveryBeacon _beacon;

        public WifiTcpServer WifiServer { get; }
        public BluetoothServer BtServer { get; }
        public UsbAoaServer UsbServer { get; }

        public ObservableCollection<GameProfile> Profiles { get; }
        private GameProfile _selectedProfile;

        public GameProfile SelectedProfile
        {
            get => _selectedProfile;
            set { _selectedProfile = value; OnPropertyChanged(); }
        }

        private string _connectionStatusText = "Listening for Android Phone...";
        public string ConnectionStatusText
        {
            get => _connectionStatusText;
            set { _connectionStatusText = value; OnPropertyChanged(); }
        }

        private string _activeTransportName = "Wi-Fi TCP (8990)";
        public string ActiveTransportName
        {
            get => _activeTransportName;
            set { _activeTransportName = value; OnPropertyChanged(); }
        }

        private double _currentLatencyMs = 0.0;
        public double CurrentLatencyMs
        {
            get => _currentLatencyMs;
            set { _currentLatencyMs = value; OnPropertyChanged(); }
        }

        private int _fps = 0;
        public int Fps
        {
            get => _fps;
            set { _fps = value; OnPropertyChanged(); }
        }

        private float _steeringValue = 0f;
        public float SteeringValue
        {
            get => _steeringValue;
            set { _steeringValue = value; OnPropertyChanged(); }
        }

        private float _throttleValue = 0f;
        public float ThrottleValue
        {
            get => _throttleValue;
            set { _throttleValue = value; OnPropertyChanged(); }
        }

        private float _brakeValue = 0f;
        public float BrakeValue
        {
            get => _brakeValue;
            set { _brakeValue = value; OnPropertyChanged(); }
        }

        private float _clutchValue = 0f;
        public float ClutchValue
        {
            get => _clutchValue;
            set { _clutchValue = value; OnPropertyChanged(); }
        }

        private string _activeGearText = "N";
        public string ActiveGearText
        {
            get => _activeGearText;
            set { _activeGearText = value; OnPropertyChanged(); }
        }

        private bool _isVirtualControllerReady;
        public bool IsVirtualControllerReady
        {
            get => _isVirtualControllerReady;
            set { _isVirtualControllerReady = value; OnPropertyChanged(); }
        }

        public MainViewModel()
        {
            _virtualController = new ViGEmControllerManager();
            _security = new PacketSecurity();

            WifiServer = new WifiTcpServer(8990);
            BtServer = new BluetoothServer();
            UsbServer = new UsbAoaServer();
            _beacon = new DiscoveryBeacon(8990, 8991);

            Profiles = new ObservableCollection<GameProfile>(GameProfileManager.GetPredefinedProfiles());
            _selectedProfile = Profiles[0]; // ETS2 default

            _activeTransport = WifiServer;
            HookTransportEvents(WifiServer);
            HookTransportEvents(BtServer);
            HookTransportEvents(UsbServer);
        }

        public void Start()
        {
            IsVirtualControllerReady = _virtualController.Initialize();
            WifiServer.Start();
            BtServer.Start();
            UsbServer.Start();
            _beacon.Start();
            ConnectionStatusText = "All Listeners Active (Wi-Fi, Bluetooth, USB Direct)";
        }

        private void HookTransportEvents(ITransportServer transport)
        {
            transport.OnPacketReceived += OnPacketReceived;
            transport.OnClientConnected += endpoint =>
            {
                Application.Current.Dispatcher.Invoke(() =>
                {
                    _activeTransport = transport;
                    ActiveTransportName = transport.Name;
                    ConnectionStatusText = $"Connected to {endpoint} via {transport.Name}";
                });
            };
            transport.OnClientDisconnected += () =>
            {
                Application.Current.Dispatcher.Invoke(() =>
                {
                    ConnectionStatusText = "Client disconnected. Listening for reconnect...";
                });
            };
        }

        private void OnPacketReceived(BazPacket packet)
        {
            // Feed to ViGEm virtual Xbox 360 controller
            _virtualController.UpdateControllerState(
                in packet,
                _selectedProfile.SteeringSensitivity,
                _selectedProfile.Deadzone
            );

            // Update UI metrics on Dispatcher
            Application.Current.Dispatcher.InvokeAsync(() =>
            {
                SteeringValue = packet.Steering / 32768.0f;
                ThrottleValue = packet.Throttle / 65535.0f;
                BrakeValue = packet.Brake / 65535.0f;
                ClutchValue = packet.Clutch / 65535.0f;
                CurrentLatencyMs = _activeTransport.LatencyMs;
                Fps = _activeTransport.PacketsPerSecond;
                ActiveGearText = packet.Gear switch
                {
                    -1 => "R",
                    0 => "N",
                    _ => packet.Gear.ToString()
                };
            });
        }

        public void Stop()
        {
            _beacon.Stop();
            WifiServer.Stop();
            BtServer.Stop();
            UsbServer.Stop();
            _virtualController.Dispose();
        }

        public event PropertyChangedEventHandler? PropertyChanged;
        protected void OnPropertyChanged([CallerMemberName] string? propName = null)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propName));
        }
    }
}
