package com.baz.controller.protocol

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecurityManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("baz_security_prefs", Context.MODE_PRIVATE)

    private val secureRandom = SecureRandom()
    private var sessionKey: SecretKey? = null

    var pairingPin: String = getOrGeneratePin()
        private set

    var isPaired: Boolean
        get() = prefs.getBoolean("is_paired", false)
        set(value) = prefs.edit().putBoolean("is_paired", value).apply()

    init {
        val storedKeyBytes = prefs.getString("session_key_b64", null)
        if (storedKeyBytes != null) {
            val decoded = android.util.Base64.decode(storedKeyBytes, android.util.Base64.NO_WRAP)
            sessionKey = SecretKeySpec(decoded, "AES")
        } else {
            generateNewSessionKey()
        }
    }

    private fun getOrGeneratePin(): String {
        var pin = prefs.getString("pairing_pin", null)
        if (pin == null) {
            pin = String.format("%06d", secureRandom.nextInt(1_000_000))
            prefs.edit().putString("pairing_pin", pin).apply()
        }
        return pin
    }

    fun regeneratePin(): String {
        val pin = String.format("%06d", secureRandom.nextInt(1_000_000))
        pairingPin = pin
        prefs.edit().putString("pairing_pin", pin).apply()
        return pin
    }

    fun generateNewSessionKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(128, secureRandom)
        val key = keyGen.generateKey()
        sessionKey = key

        val b64 = android.util.Base64.encodeToString(key.encoded, android.util.Base64.NO_WRAP)
        prefs.edit().putString("session_key_b64", b64).apply()
        return key
    }

    fun encryptPayload(plainBytes: ByteArray): Pair<ByteArray, ByteArray> {
        val key = sessionKey ?: generateNewSessionKey()
        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val cipherText = cipher.doFinal(plainBytes)
        return Pair(cipherText, iv)
    }

    fun decryptPayload(cipherBytes: ByteArray, iv: ByteArray): ByteArray? {
        return try {
            val key = sessionKey ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            cipher.doFinal(cipherBytes)
        } catch (e: Exception) {
            null
        }
    }
}
