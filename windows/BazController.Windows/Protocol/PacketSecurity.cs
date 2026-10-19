using System;
using System.Security.Cryptography;
using System.Text;

namespace BazController.Windows.Protocol
{
    public class PacketSecurity
    {
        private byte[]? _sessionKey;
        public string ConfiguredPin { get; set; } = "123456";
        public bool IsAuthenticated { get; private set; }

        public bool Authenticate(string incomingPin, byte[] sessionKeyCandidate)
        {
            if (incomingPin == ConfiguredPin)
            {
                _sessionKey = sessionKeyCandidate;
                IsAuthenticated = true;
                return true;
            }
            return false;
        }

        public byte[]? DecryptPayload(byte[] cipherText, byte[] iv, byte[] tag)
        {
            if (_sessionKey == null || !IsAuthenticated) return null;

            try
            {
                using var aesGcm = new AesGcm(_sessionKey, 16);
                byte[] plainText = new byte[cipherText.Length];
                aesGcm.Decrypt(iv, cipherText, tag, plainText);
                return plainText;
            }
            catch
            {
                return null;
            }
        }
    }
}
