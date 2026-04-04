package com.example.calculator.utils

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class PassManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "passkey_prefs"
        private const val KEY_IS_SETUP = "is_setup"
        private const val KEY_IV = "iv"
        private const val KEY_ENCRYPTED_PIN = "encrypted_pin"
        private const val KEY_SECRET = "secret_key"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    private fun getOrCreateSecretKey(): SecretKey {
        val savedKey = prefs.getString(KEY_SECRET, null)
        return if (savedKey != null) {
            val keyBytes = android.util.Base64.decode(savedKey, android.util.Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256, secureRandom)
            val newKey = keyGenerator.generateKey()
            val encodedKey = android.util.Base64.encodeToString(newKey.encoded, android.util.Base64.DEFAULT)
            prefs.edit().putString(KEY_SECRET, encodedKey).apply()
            newKey
        }
    }

    fun initPassKey(pin: String): Boolean {
        return try {
            if (isPassKeySetup()) return false
            if (pin.length < 4) return false

            val iv = ByteArray(16)
            secureRandom.nextBytes(iv)

            val secretKey = getOrCreateSecretKey()
            val encryptedPin = encryptPin(pin, iv, secretKey)

            prefs.edit()
                .putBoolean(KEY_IS_SETUP, true)
                .putString(KEY_IV, android.util.Base64.encodeToString(iv, android.util.Base64.DEFAULT))
                .putString(KEY_ENCRYPTED_PIN, encryptedPin)
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun validatePassKey(pin: String): Boolean {
        val encryptedPin = prefs.getString(KEY_ENCRYPTED_PIN, null) ?: return false
        val ivString = prefs.getString(KEY_IV, null) ?: return false

        return try {
            val iv = android.util.Base64.decode(ivString, android.util.Base64.DEFAULT)
            val secretKey = getOrCreateSecretKey()
            val decryptedPin = decryptPin(encryptedPin, iv, secretKey)
            decryptedPin == pin
        } catch (e: Exception) {
            false
        }
    }

    fun resetPassKey(oldPin: String, newPin: String): Boolean {
        if (!validatePassKey(oldPin)) return false

        val iv = ByteArray(16)
        secureRandom.nextBytes(iv)
        val secretKey = getOrCreateSecretKey()
        val encryptedPin = encryptPin(newPin, iv, secretKey)

        prefs.edit()
            .putString(KEY_IV, android.util.Base64.encodeToString(iv, android.util.Base64.DEFAULT))
            .putString(KEY_ENCRYPTED_PIN, encryptedPin)
            .apply()
        return true
    }

    fun isPassKeySetup(): Boolean = prefs.getBoolean(KEY_IS_SETUP, false)

    fun clearPassKey() {
        prefs.edit().clear().apply()
    }

    private fun encryptPin(pin: String, iv: ByteArray, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(pin.toByteArray())
        return android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
    }

    private fun decryptPin(encryptedPin: String, iv: ByteArray, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        val decrypted = cipher.doFinal(android.util.Base64.decode(encryptedPin, android.util.Base64.DEFAULT))
        return String(decrypted)
    }
}