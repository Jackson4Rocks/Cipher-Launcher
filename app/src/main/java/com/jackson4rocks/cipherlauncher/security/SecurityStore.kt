package com.jackson4rocks.cipherlauncher.security

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class SecurityStore(context: Context) {

    private val prefs = context.getSharedPreferences("cipher_security", Context.MODE_PRIVATE)
    private val random = SecureRandom()

    fun isConfigured(): Boolean =
        prefs.contains(KEY_WATCH_HASH) && prefs.contains(KEY_HOME_HASH)

    fun setup(watchPin: String, homePin: String) {
        require(validPin(watchPin))
        require(validPin(homePin))
        require(watchPin != homePin)

        val watch = createRecord(watchPin)
        val home = createRecord(homePin)

        prefs.edit()
            .putString(KEY_WATCH_SALT, watch.first)
            .putString(KEY_WATCH_HASH, watch.second)
            .putString(KEY_HOME_SALT, home.first)
            .putString(KEY_HOME_HASH, home.second)
            .apply()
    }

    fun verifyWatchPin(pin: String): Boolean =
        verify(pin, KEY_WATCH_SALT, KEY_WATCH_HASH)

    fun verifyHomePin(pin: String): Boolean =
        verify(pin, KEY_HOME_SALT, KEY_HOME_HASH)

    fun validPin(pin: String): Boolean =
        pin.length in 4..12 && pin.all(Char::isDigit)

    fun changeWatchPin(current: String, next: String): Boolean {
        if (!verifyWatchPin(current) || !validPin(next)) return false
        val record = createRecord(next)
        prefs.edit()
            .putString(KEY_WATCH_SALT, record.first)
            .putString(KEY_WATCH_HASH, record.second)
            .apply()
        return true
    }

    fun changeHomePin(current: String, next: String): Boolean {
        if (!verifyHomePin(current) || !validPin(next)) return false
        val record = createRecord(next)
        prefs.edit()
            .putString(KEY_HOME_SALT, record.first)
            .putString(KEY_HOME_HASH, record.second)
            .apply()
        return true
    }

    private fun createRecord(pin: String): Pair<String, String> {
        val salt = ByteArray(16).also(random::nextBytes)
        val hash = hash(pin, salt)
        return salt.toHex() to hash.toHex()
    }

    private fun verify(pin: String, saltKey: String, hashKey: String): Boolean {
        if (!validPin(pin)) return false

        val saltHex = prefs.getString(saltKey, null) ?: return false
        val expectedHex = prefs.getString(hashKey, null) ?: return false

        val salt = saltHex.hexToBytes()
        val expected = expectedHex.hexToBytes()
        val actual = hash(pin, salt)

        return MessageDigest.isEqual(actual, expected)
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private companion object {
        const val KEY_WATCH_SALT = "watch_salt"
        const val KEY_WATCH_HASH = "watch_hash"
        const val KEY_HOME_SALT = "home_salt"
        const val KEY_HOME_HASH = "home_hash"
    }
}
