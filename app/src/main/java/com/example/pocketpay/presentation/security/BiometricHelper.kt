package com.example.pocketpay.presentation.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Biometric authentication that is cryptographically bound to the Keystore.
 *
 * A plain `BiometricPrompt` only returns a boolean that malware could fake with a hooking
 * framework. Here the prompt is created with a [BiometricPrompt.CryptoObject] wrapping a key that
 * the Keystore only releases after a real biometric match, and success is accepted only if that
 * key actually performs a cryptographic operation.
 *
 * The key is invalidated by the OS when a new fingerprint/face is enrolled. It is only
 * (re)created after the user proves knowledge of the PIN ([arm]), so someone who adds their own
 * fingerprint cannot silently unlock the app.
 */
object BiometricHelper {
    private const val PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "pocketpay_biometric_key"
    private val CHALLENGE = "pocketpay-biometric-challenge".toByteArray()

    private fun keyStore() = KeyStore.getInstance(PROVIDER).apply { load(null) }

    private fun existingKey(): SecretKey? = try {
        keyStore().getKey(KEY_ALIAS, null) as? SecretKey
    } catch (e: Exception) {
        null
    }

    private fun deleteKey() {
        try { keyStore().deleteEntry(KEY_ALIAS) } catch (_: Exception) {}
    }

    /** Creates the biometric-bound key if it does not exist yet. Call after the PIN was verified. */
    fun arm() {
        if (existingKey() != null) return
        try {
            val builder = KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(-1)
            }
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
                .apply { init(builder.build()) }
                .generateKey()
        } catch (_: Exception) {
            // No secure lock screen / no enrolled biometrics yet: biometrics simply stay unavailable.
        }
    }

    /** True when hardware + enrolment exist AND the bound key has been armed by a PIN entry. */
    fun isBiometricAvailable(context: Context): Boolean {
        val ok = BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        return ok && existingKey() != null
    }

    /**
     * Displays the system biometric prompt. [onSuccess] runs only after the Keystore key has
     * been used successfully, not merely because the prompt reported success.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Authentication Required",
        subtitle: String = "Use fingerprint or Face ID",
        onSuccess: () -> Unit,
        onFallbackToPin: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cipher = try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE, existingKey() ?: throw IllegalStateException("not armed"))
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            deleteKey() // biometrics changed: force PIN entry, which re-arms a fresh key
            onError("Biometrik berubah. Masukkan PIN untuk mengaktifkan kembali.")
            onFallbackToPin()
            return
        } catch (e: Exception) {
            onFallbackToPin()
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setNegativeButtonText("Use PIN")
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val unlockedCipher = result.cryptoObject?.cipher
                    val proven = try {
                        unlockedCipher != null && unlockedCipher.doFinal(CHALLENGE).isNotEmpty()
                    } catch (e: Exception) {
                        false
                    }
                    if (proven) onSuccess() else onError("Verifikasi biometrik gagal.")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        onFallbackToPin()
                    } else {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Biometric not recognized, please try again.")
                }
            }
        )

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }
}
