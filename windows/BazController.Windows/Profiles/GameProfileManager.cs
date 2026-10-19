using System;
using System.Collections.Generic;

namespace BazController.Windows.Profiles
{
    public class GameProfile
    {
        public string Name { get; set; } = "";
        public string Description { get; set; } = "";
        public float SteeringSensitivity { get; set; } = 1.0f;
        public float Deadzone { get; set; } = 0.02f;
        public float NonLinearity { get; set; } = 1.0f;
        public bool InvertThrottle { get; set; } = false;
        public bool InvertBrake { get; set; } = false;
    }

    public static class GameProfileManager
    {
        public static List<GameProfile> GetPredefinedProfiles()
        {
            return new List<GameProfile>
            {
                new GameProfile
                {
                    Name = "Euro Truck Simulator 2 (ETS2)",
                    Description = "900° steering curve, linear throttle, progressive brake pedal, H-shifter support",
                    SteeringSensitivity = 1.0f,
                    Deadzone = 0.015f,
                    NonLinearity = 1.2f
                },
                new GameProfile
                {
                    Name = "American Truck Simulator (ATS)",
                    Description = "Highway cruiser calibration with Jake-brake retarder and smooth auto-center",
                    SteeringSensitivity = 0.95f,
                    Deadzone = 0.02f,
                    NonLinearity = 1.25f
                },
                new GameProfile
                {
                    Name = "Assetto Corsa / Sim Racing",
                    Description = "1:1 direct input, zero deadzone, rapid response for racing wheels",
                    SteeringSensitivity = 1.0f,
                    Deadzone = 0.005f,
                    NonLinearity = 1.0f
                },
                new GameProfile
                {
                    Name = "Forza Horizon 5 / Arcade",
                    Description = "High sensitivity steering, forgiving deadzone for drift and high-speed cornering",
                    SteeringSensitivity = 1.25f,
                    Deadzone = 0.03f,
                    NonLinearity = 1.4f
                },
                new GameProfile
                {
                    Name = "Generic XInput Gamepad",
                    Description = "Standard Xbox 360 controller mapping compatible with all PC titles",
                    SteeringSensitivity = 1.0f,
                    Deadzone = 0.02f,
                    NonLinearity = 1.0f
                }
            };
        }
    }
}
