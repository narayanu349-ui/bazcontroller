# BazController Windows Companion (.NET 8 WPF)

High-performance Windows Companion Software for the **BazController** Android application. It receives 64-byte high-frequency game controller packets over USB Direct (AOA/ADB), Bluetooth Classic (SPP RFCOMM), Bluetooth Low Energy (BLE), and Wi-Fi (TCP socket), converts them into a hardware-recognized virtual Xbox 360 controller via **ViGEmBus**, and injects them directly into games with **<8ms ultra-low latency**.

---

## Features
- **Plug & Play Virtual Gamepad**: Emulates official Xbox 360 controller recognized by Windows, Steam, Euro Truck Simulator 2, American Truck Simulator, Assetto Corsa, Forza Horizon, BeamNG.drive.
- **Transports Supported**:
  - **USB Direct (AOA / ADB reverse)**: Port `8992` -> `8990` (<1ms latency)
  - **Bluetooth Classic (SPP RFCOMM)**: Auto-binds with paired Android phone
  - **Wi-Fi TCP Socket**: Port `8990` with UDP discovery beacon on `8991`
- **Profiles**: Pre-configured sensitivity curves and deadzones for ETS2, ATS, Sim Racing, and Arcade.
- **Force Feedback**: Bi-directional rumble telemetry forwarder from game back to Android phone vibrator.
- **Security**: PIN authentication and AES-128 GCM encryption payload decoding.

---

## Prerequisites
1. **Windows 10 / Windows 11 (64-bit)**
2. **.NET 8.0 SDK** or **Visual Studio 2022** (v17.8+) with ".NET desktop development" workload.
3. **ViGEmBus Driver**:
   - Download and install ViGEmBus from official releases:
     `https://github.com/nefarius/ViGEmBus/releases`
   - Run `ViGEmBus_Setup_x64.exe` to register the virtual controller kernel bus.

---

## How to Build & Run

### Method 1: Using .NET CLI
```bash
# Navigate to the solution folder
cd windows

# Restore NuGet dependencies
dotnet restore

# Build release binary
dotnet build -c Release

# Run the companion app
dotnet run --project BazController.Windows/BazController.Windows.csproj
```

### Method 2: Using Visual Studio 2022
1. Double click `windows/BazController.Windows.sln`.
2. Ensure build target is set to `Debug` or `Release` and `Any CPU` or `x64`.
3. Press `F5` to build and run.

---

## Game Setup Instructions

### Euro Truck Simulator 2 (ETS2) / American Truck Simulator (ATS)
1. Start `BazController.Windows.exe` first. The app will create a virtual Xbox 360 controller.
2. Launch ETS2 / ATS.
3. Go to **Options -> Controls**.
4. At the top dropdown, select **Keyboard + Controller (Xbox 360 for Windows)**.
5. Set Controller Subtype to **Wheel** or **Gamepad**.
6. Assign:
   - Steering Axis: Steer left/right on your Android phone.
   - Acceleration Axis: Press accelerator pedal.
   - Brake Axis: Press brake pedal.
   - Transmission: Select **Simple Automatic**, **Real Automatic**, or **H-Shifter** (map gears 1-6 and R).
7. Enjoy ultra-smooth 120 FPS control!
