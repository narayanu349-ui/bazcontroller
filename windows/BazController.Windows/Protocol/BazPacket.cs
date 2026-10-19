using System;
using System.IO;
using System.IO.Hashing;
using System.Runtime.InteropServices;

namespace BazController.Windows.Protocol
{
    [StructLayout(LayoutKind.Sequential, Pack = 1)]
    public struct BazPacket
    {
        public const int PacketSize = 64;
        public const byte Magic0 = 0xBA;
        public const byte Magic1 = 0x5C;
        public const byte ProtocolVersion = 0x01;

        // Packet Types
        public const byte TypeInputFrame = 0x01;
        public const byte TypeHandshakeReq = 0x02;
        public const byte TypeHandshakeAck = 0x03;
        public const byte TypeHeartbeat = 0x04;
        public const byte TypeFeedbackForce = 0x05;
        public const byte TypeDisconnect = 0x06;

        // Button Bitmasks
        public const ulong BtnDpadUp = 1UL << 0;
        public const ulong BtnDpadDown = 1UL << 1;
        public const ulong BtnDpadLeft = 1UL << 2;
        public const ulong BtnDpadRight = 1UL << 3;
        public const ulong BtnGearR = 1UL << 4;
        public const ulong BtnGear1 = 1UL << 5;
        public const ulong BtnGear2 = 1UL << 6;
        public const ulong BtnGear3 = 1UL << 7;
        public const ulong BtnGear4 = 1UL << 8;
        public const ulong BtnGear5 = 1UL << 9;
        public const ulong BtnGear6 = 1UL << 10;
        public const ulong BtnShiftUp = 1UL << 11;
        public const ulong BtnShiftDown = 1UL << 12;
        public const ulong BtnIndicatorLeft = 1UL << 13;
        public const ulong BtnIndicatorRight = 1UL << 14;
        public const ulong BtnHazards = 1UL << 15;
        public const ulong BtnHeadlights = 1UL << 16;
        public const ulong BtnHighBeam = 1UL << 17;
        public const ulong BtnHorn = 1UL << 18;
        public const ulong BtnWipers = 1UL << 19;
        public const ulong BtnCruiseSet = 1UL << 20;
        public const ulong BtnCruiseRes = 1UL << 21;
        public const ulong BtnCruisePlus = 1UL << 22;
        public const ulong BtnCruiseMinus = 1UL << 23;
        public const ulong BtnCamCockpit = 1UL << 24;
        public const ulong BtnCamChase = 1UL << 25;
        public const ulong BtnCamFree = 1UL << 26;
        public const ulong BtnCamTop = 1UL << 27;
        public const ulong BtnDiffLock = 1UL << 28;
        public const ulong BtnEngineStart = 1UL << 29;
        public const ulong BtnBeacon = 1UL << 30;
        public const ulong BtnRetarder = 1UL << 31;
        public const ulong BtnCustom1 = 1UL << 32;
        public const ulong BtnCustom2 = 1UL << 33;
        public const ulong BtnCustom3 = 1UL << 34;
        public const ulong BtnCustom4 = 1UL << 35;

        // Header
        public byte M0;
        public byte M1;
        public byte Version;
        public byte PacketType;

        // Sequence & Timing
        public uint SequenceNumber;
        public ulong TimestampUs;

        // Analog Axes
        public short Steering;       // -32768 to 32767
        public ushort Throttle;      // 0 to 65535
        public ushort Brake;         // 0 to 65535
        public ushort Clutch;        // 0 to 65535
        public ushort Handbrake;     // 0 to 65535
        public short JoyX;          // -32768 to 32767
        public short JoyY;          // -32768 to 32767

        // Buttons
        public ulong ButtonsMask;

        // Vehicle Aux
        public sbyte Gear;
        public byte WiperMode;
        public byte LightMode;
        public byte AuxiliaryFlags;

        // Auth Tag (12 bytes)
        public fixed byte AuthTag[12];

        // Checksum
        public uint Crc32;

        public static bool TryParse(ReadOnlySpan<byte> buffer, out BazPacket packet)
        {
            packet = default;
            if (buffer.Length < PacketSize) return false;

            if (buffer[0] != Magic0 || buffer[1] != Magic1) return false;

            // Verify CRC32
            uint packetCrc = BitConverter.ToUInt32(buffer.Slice(60, 4));
            uint computedCrc = Crc32Algorithm.Compute(buffer.Slice(0, 60));
            if (packetCrc != computedCrc) return false;

            packet = MemoryMarshal.Read<BazPacket>(buffer);
            return true;
        }

        public byte[] Serialize()
        {
            byte[] bytes = new byte[PacketSize];
            M0 = Magic0;
            M1 = Magic1;
            Version = ProtocolVersion;

            MemoryMarshal.Write(bytes, in this);
            uint checksum = Crc32Algorithm.Compute(bytes.AsSpan(0, 60));
            BitConverter.TryWriteBytes(bytes.AsSpan(60, 4), checksum);
            return bytes;
        }
    }

    public static class Crc32Algorithm
    {
        public static uint Compute(ReadOnlySpan<byte> data)
        {
            // Standard IEEE 802.3 32-bit CRC
            uint crc = 0xFFFFFFFF;
            foreach (byte b in data)
            {
                crc ^= b;
                for (int i = 0; i < 8; i++)
                {
                    crc = (crc & 1) != 0 ? (crc >> 1) ^ 0xEDB88320 : (crc >> 1);
                }
            }
            return ~crc;
        }
    }
}
