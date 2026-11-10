package com.example.pocketpay.presentation.security

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed interface PinResult {
    data object Success : PinResult
    data object NotSet : PinResult
    data class Wrong(val attemptsLeft: Int) : PinResult
    data class LockedOut(val secondsLeft: Long) : PinResult
}

/**
 * Stores the transaction PIN as a salted PBKDF2 hash, additionally encrypted with a Keystore key
 * (see [Vault]) so the stored material is useless off-device, and enforces
 * a limited number of attempts followed by a temporary lockout.
 *
 * The user creates the PIN on first launch ([setInitialPin]) and can change it later ([changePin]).
 */
object PinManager {
    private const val PREFS = "pocketpay_pin_store"
    private const val KEY_HASH = "pin_hash"
    private const val KEY_SALT = "pin_salt"
    private const val KEY_FAILS = "fail_count"
    private const val KEY_LOCK_UNTIL = "lock_until"
    private const val ITERATIONS = 120_000
    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_MS = 30_000L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    /** Reads (salt, hash) decrypted from the Keystore-protected store, or null if absent/unreadable. */
    private fun readSecrets(context: Context): Pair<ByteArray, ByteArray>? {
        val p = prefs(context)
        val saltToken = p.getString(KEY_SALT, null) ?: return null
        val hashToken = p.getString(KEY_HASH, null) ?: return null
        return try {
            Vault.cipher.decrypt(saltToken) to Vault.cipher.decrypt(hashToken)
        } catch (e: Exception) {
            null // corrupt, tampered, or Keystore key lost: treat as "no PIN" so the user can re-create it
        }
    }

    fun isPinSet(context: Context): Boolean = readSecrets(context) != null

    /** Returns an error message if the PIN is unacceptable, or null if it is fine. */
    fun validateNewPin(pin: String): String? {
        if (pin.length != 6 || !pin.all { it.isDigit() }) return "PIN harus 6 digit angka."
        if (pin.all { it == pin[0] }) return "PIN tidak boleh angka yang sama semua."
        val asc = "0123456789"
        if (asc.contains(pin) || asc.reversed().contains(pin)) return "PIN tidak boleh berurutan."
        return null
    }

    /** Sets the PIN for the first time. Returns an error message or null on success. */
    @Synchronized
    fun setInitialPin(context: Context, pin: String): String? {
        if (isPinSet(context)) return "PIN sudah pernah dibuat."
        validateNewPin(pin)?.let { return it }
        storePin(context, pin)
        runCatching { BiometricHelper.arm() }
        return null
    }

    /** Changes the PIN after verifying the old one. Returns an error message or null on success. */
    @Synchronized
    fun changePin(context: Context, oldPin: String, newPin: String): String? {
        val result = verify(context, oldPin)
        if (result != PinResult.Success) return describe(result)
        validateNewPin(newPin)?.let { return it }
        if (oldPin == newPin) return "PIN baru harus berbeda dari PIN lama."
        storePin(context, newPin)
        return null
    }

    private fun storePin(context: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs(context).edit()
            .putString(KEY_SALT, Vault.cipher.encrypt(salt))
            .putString(KEY_HASH, Vault.cipher.encrypt(hash(pin, salt)))
            .putInt(KEY_FAILS, 0)
            .putLong(KEY_LOCK_UNTIL, 0L)
            .apply()
    }

    @Synchronized
    fun verify(context: Context, pin: String): PinResult {
        val (salt, expected) = readSecrets(context) ?: return PinResult.NotSet
        val p = prefs(context)
        val now = System.currentTimeMillis()

        val lockUntil = p.getLong(KEY_LOCK_UNTIL, 0L)
        if (lockUntil > now) {
            return PinResult.LockedOut((lockUntil - now + 999) / 1000)
        }

        val ok = MessageDigest.isEqual(expected, hash(pin, salt))

        if (ok) {
            p.edit().putInt(KEY_FAILS, 0).putLong(KEY_LOCK_UNTIL, 0L).apply()
            // Proof of PIN knowledge is the moment it is safe to (re)create the biometric key.
            runCatching { BiometricHelper.arm() }
            return PinResult.Success
        }

        val fails = p.getInt(KEY_FAILS, 0) + 1
        return if (fails >= MAX_ATTEMPTS) {
            p.edit().putInt(KEY_FAILS, 0).putLong(KEY_LOCK_UNTIL, now + LOCKOUT_MS).apply()
            PinResult.LockedOut(LOCKOUT_MS / 1000)
        } else {
            p.edit().putInt(KEY_FAILS, fails).apply()
            PinResult.Wrong(MAX_ATTEMPTS - fails)
        }
    }

    fun describe(result: PinResult): String? = when (result) {
        PinResult.Success -> null
        PinResult.NotSet -> "PIN belum dibuat."
        is PinResult.Wrong -> "PIN salah. Sisa percobaan: ${result.attemptsLeft}"
        is PinResult.LockedOut -> "Terlalu banyak percobaan. Coba lagi dalam ${result.secondsLeft} detik."
    }
}
