package com.example.pocketpay.presentation.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts small secrets so they are only readable on this device by this app. */
interface SecretCipher {
    fun encrypt(plain: ByteArray): String
    /** @throws Exception if the token is corrupt or the key no longer exists. */
    fun decrypt(token: String): ByteArray
}

/**
 * AES-256-GCM with a non-exportable key held in the Android Keystore (hardware-backed when the
 * device supports it). Output format: base64(iv[12] + ciphertext+tag).
 */
object KeystoreSecretCipher : SecretCipher {
    private const val PROVIDER = "AndroidKeyStore"
    private const val ALIAS = "pocketpay_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LEN = 12

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        gen.init(
            KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    override fun encrypt(plain: ByteArray): String {
        val c = Cipher.getInstance(TRANSFORMATION)
        c.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(c.iv + c.doFinal(plain), Base64.NO_WRAP)
    }

    override fun decrypt(token: String): ByteArray {
        val raw = Base64.decode(token, Base64.NO_WRAP)
        require(raw.size > IV_LEN) { "Corrupt token" }
        val c = Cipher.getInstance(TRANSFORMATION)
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw, 0, IV_LEN))
        return c.doFinal(raw, IV_LEN, raw.size - IV_LEN)
    }
}

/** Process-wide cipher. Unit tests (no Keystore under Robolectric) swap in a fake. */
object Vault {
    @Volatile
    var cipher: SecretCipher = KeystoreSecretCipher
}
