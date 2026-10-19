using System;
using BazController.Windows.Protocol;
using Nefarius.ViGEm.Client;
using Nefarius.ViGEm.Client.Targets;
using Nefarius.ViGEm.Client.Targets.Xbox360;

namespace BazController.Windows.VirtualController
{
    public class ViGEmControllerManager : IDisposable
    {
        private ViGEmClient? _client;
        private IXbox360Controller? _controller;
        public bool IsInitialized { get; private set; }
        public bool IsPluggedIn { get; private set; }

        public event Action<byte, byte>? OnForceFeedbackReceived;

        public bool Initialize()
        {
            try
            {
                _client = new ViGEmClient();
                _controller = _client.CreateXbox360Controller();

                // Hook force feedback / rumble events to transmit back to phone
                _controller.FeedbackReceived += (sender, args) =>
                {
                    OnForceFeedbackReceived?.Invoke(args.LargeMotor, args.SmallMotor);
                };

                _controller.Connect();
                IsInitialized = true;
                IsPluggedIn = true;
                return true;
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"ViGEmBus init failed: {ex.Message}. Virtual driver might not be installed.");
                IsInitialized = false;
                IsPluggedIn = false;
                return false;
            }
        }

        public void UpdateControllerState(in BazPacket packet, float steeringSensitivity = 1.0f, float deadzone = 0.02f)
        {
            if (_controller == null || !IsPluggedIn) return;

            // 1. Steering Axis -> Left Thumb X (-32768 to 32767)
            float normSteering = packet.Steering / 32768.0f;
            float absSteering = Math.Abs(normSteering);
            short finalSteering = 0;

            if (absSteering > deadzone)
            {
                float remapped = (absSteering - deadzone) / (1.0f - deadzone);
                remapped = Math.Clamp(remapped * steeringSensitivity, 0.0f, 1.0f);
                float signed = (normSteering >= 0 ? 1 : -1) * remapped;
                finalSteering = (short)(signed * 32767);
            }
            _controller.SetAxisValue(Xbox360Axis.LeftThumbX, finalSteering);

            // 2. Throttle -> Right Trigger (0 to 255)
            byte rt = (byte)(packet.Throttle >> 8);
            _controller.SetSliderValue(Xbox360Slider.RightTrigger, rt);

            // 3. Brake -> Left Trigger (0 to 255)
            byte lt = (byte)(packet.Brake >> 8);
            _controller.SetSliderValue(Xbox360Slider.LeftTrigger, lt);

            // 4. Analog Stick (Camera look or steer)
            _controller.SetAxisValue(Xbox360Axis.RightThumbX, packet.JoyX);
            _controller.SetAxisValue(Xbox360Axis.RightThumbY, (short)-packet.JoyY);

            // 5. Digital Buttons Mapping
            ulong b = packet.ButtonsMask;
            _controller.SetButtonState(Xbox360Button.Up, (b & BazPacket.BtnDpadUp) != 0);
            _controller.SetButtonState(Xbox360Button.Down, (b & BazPacket.BtnDpadDown) != 0);
            _controller.SetButtonState(Xbox360Button.Left, (b & BazPacket.BtnDpadLeft) != 0);
            _controller.SetButtonState(Xbox360Button.Right, (b & BazPacket.BtnDpadRight) != 0);

            // Shift Up/Down or A/B/X/Y
            _controller.SetButtonState(Xbox360Button.RightShoulder, (b & BazPacket.BtnShiftUp) != 0);
            _controller.SetButtonState(Xbox360Button.LeftShoulder, (b & BazPacket.BtnShiftDown) != 0);

            _controller.SetButtonState(Xbox360Button.A, (b & BazPacket.BtnHorn) != 0);
            _controller.SetButtonState(Xbox360Button.B, (b & BazPacket.BtnHandbrake) != 0);
            _controller.SetButtonState(Xbox360Button.X, (b & BazPacket.BtnWipers) != 0);
            _controller.SetButtonState(Xbox360Button.Y, (b & BazPacket.BtnHeadlights) != 0);

            _controller.SetButtonState(Xbox360Button.Start, (b & BazPacket.BtnEngineStart) != 0);
            _controller.SetButtonState(Xbox360Button.Back, (b & BazPacket.BtnCamCockpit) != 0);

            // Push report immediately to the OS kernel
            _controller.SubmitReport();
        }

        public void Dispose()
        {
            if (_controller != null)
            {
                try
                {
                    _controller.Disconnect();
                }
                catch { }
                _controller = null;
            }
            _client?.Dispose();
            _client = null;
            IsPluggedIn = false;
            IsInitialized = false;
        }
    }
}
