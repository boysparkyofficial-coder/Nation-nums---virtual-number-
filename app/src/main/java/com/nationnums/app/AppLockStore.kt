package com.nationnums.app

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Stores only a salted password hash for the app-only local unlock gate.
 * This credential is separate from the Nation Nums/Base44 account password.
 */
internal class AppLockStore(context: Context) {
    private val prefs = context.getSharedPreferences("nation_nums_app_lock", Context.MODE_PRIVATE)

    fun isConfigured(): Boolean =
        !prefs.getString(KEY_SALT, null).isNullOrBlank() &&
            !prefs.getString(KEY_HASH, null).isNullOrBlank()

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)

    /** A biometric-only lock is valid even when no separate app passcode was configured. */
    fun isLockEnabled(): Boolean = isConfigured() || isBiometricEnabled()

    /** Persist the biometric preference only after a successful system biometric prompt. */
    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    fun isLockedOut(): Boolean = System.currentTimeMillis() < prefs.getLong(KEY_LOCKED_UNTIL, 0L)

    fun remainingLockoutSeconds(): Long =
        ((prefs.getLong(KEY_LOCKED_UNTIL, 0L) - System.currentTimeMillis()).coerceAtLeast(0L) + 999L) / 1000L

    /** Records a failed unlock and returns the number of attempts remaining before cooldown. */
    fun recordFailedAttempt(): Int {
        if (isLockedOut()) return 0
        val failures = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        if (failures >= MAX_FAILED_ATTEMPTS) {
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKED_UNTIL, System.currentTimeMillis() + LOCKOUT_MILLIS)
                .apply()
            return 0
        }
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, failures).apply()
        return MAX_FAILED_ATTEMPTS - failures
    }

    fun clearFailedAttempts() {
        prefs.edit().remove(KEY_FAILED_ATTEMPTS).remove(KEY_LOCKED_UNTIL).apply()
    }

    fun configurePassword(password: String, biometricEnabled: Boolean) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = deriveHash(password, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putBoolean(KEY_BIOMETRIC, biometricEnabled)
            .apply()
    }

    fun verifyPassword(password: String): Boolean {
        val saltText = prefs.getString(KEY_SALT, null) ?: return false
        val hashText = prefs.getString(KEY_HASH, null) ?: return false
        return try {
            val salt = Base64.decode(saltText, Base64.NO_WRAP)
            val expected = Base64.decode(hashText, Base64.NO_WRAP)
            MessageDigest.isEqual(expected, deriveHash(password, salt))
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun deriveHash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private companion object {
        const val KEY_SALT = "password_salt"
        const val KEY_HASH = "password_hash"
        const val KEY_BIOMETRIC = "biometric_enabled"
        const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        const val KEY_LOCKED_UNTIL = "locked_until"
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_MILLIS = 30_000L
        const val PBKDF2_ITERATIONS = 120_000
        const val HASH_BITS = 256
    }
}
