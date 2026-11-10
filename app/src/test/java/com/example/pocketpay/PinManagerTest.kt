package com.example.pocketpay

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.pocketpay.presentation.security.PinManager
import com.example.pocketpay.presentation.security.PinResult
import com.example.pocketpay.presentation.security.SecretCipher
import com.example.pocketpay.presentation.security.Vault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PinManagerTest {

    private lateinit var context: Context

    /** Robolectric has no AndroidKeyStore, so use a reversible stand-in for the Keystore cipher. */
    private object FakeCipher : SecretCipher {
        override fun encrypt(plain: ByteArray) = "enc:" + java.util.Base64.getEncoder().encodeToString(plain)
        override fun decrypt(token: String): ByteArray {
            require(token.startsWith("enc:")) { "not encrypted" }
            return java.util.Base64.getDecoder().decode(token.removePrefix("enc:"))
        }
    }

    @Before
    fun setUp() {
        Vault.cipher = FakeCipher
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("pocketpay_pin_store", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun noPinSetInitially_andVerifyReportsNotSet() {
        assertFalse(PinManager.isPinSet(context))
        assertEquals(PinResult.NotSet, PinManager.verify(context, "135790"))
    }

    @Test
    fun weakPinsAreRejected() {
        assertNotNull(PinManager.validateNewPin(context, "111111"))
        assertNotNull(PinManager.validateNewPin(context, "123456"))
        assertNotNull(PinManager.validateNewPin(context, "654321"))
        assertNotNull(PinManager.validateNewPin(context, "12345"))
        assertNotNull(PinManager.validateNewPin(context, "12ab56"))
        assertNull(PinManager.validateNewPin(context, "135790"))
    }

    @Test
    fun setInitialPin_thenCorrectPinVerifies() {
        assertNull(PinManager.setInitialPin(context, "135790"))
        assertTrue(PinManager.isPinSet(context))
        assertEquals(PinResult.Success, PinManager.verify(context, "135790"))
    }

    @Test
    fun pinIsNotStoredInPlaintext() {
        PinManager.setInitialPin(context, "135790")
        val raw = context.getSharedPreferences("pocketpay_pin_store", Context.MODE_PRIVATE)
            .all.values.joinToString("|")
        assertFalse(raw.contains("135790"))
    }

    @Test
    fun setInitialPin_cannotOverwriteExistingPin() {
        PinManager.setInitialPin(context, "135790")
        assertNotNull(PinManager.setInitialPin(context, "246802"))
        assertEquals(PinResult.Success, PinManager.verify(context, "135790"))
    }

    @Test
    fun wrongPin_reportsRemainingAttempts() {
        PinManager.setInitialPin(context, "135790")
        assertEquals(PinResult.Wrong(4), PinManager.verify(context, "000001"))
        assertEquals(PinResult.Wrong(3), PinManager.verify(context, "000001"))
    }

    @Test
    fun fiveWrongAttempts_lockOutEvenTheCorrectPin() {
        PinManager.setInitialPin(context, "135790")
        repeat(4) { PinManager.verify(context, "000001") }
        assertTrue(PinManager.verify(context, "000001") is PinResult.LockedOut)
        assertTrue(PinManager.verify(context, "135790") is PinResult.LockedOut)
    }

    @Test
    fun successfulVerify_resetsFailureCounter() {
        PinManager.setInitialPin(context, "135790")
        repeat(3) { PinManager.verify(context, "000001") }
        assertEquals(PinResult.Success, PinManager.verify(context, "135790"))
        assertEquals(PinResult.Wrong(4), PinManager.verify(context, "000001"))
    }

    @Test
    fun changePin_requiresCorrectOldPin() {
        PinManager.setInitialPin(context, "135790")
        assertNotNull(PinManager.changePin(context, "000001", "246802"))
        assertEquals(PinResult.Success, PinManager.verify(context, "135790"))
    }

    @Test
    fun changePin_success_replacesOldPin() {
        PinManager.setInitialPin(context, "135790")
        assertNull(PinManager.changePin(context, "135790", "246802"))
        assertEquals(PinResult.Success, PinManager.verify(context, "246802"))
        assertEquals(PinResult.Wrong(4), PinManager.verify(context, "135790"))
    }

    @Test
    fun changePin_rejectsSamePinAndWeakPin() {
        PinManager.setInitialPin(context, "135790")
        assertNotNull(PinManager.changePin(context, "135790", "135790"))
        assertNotNull(PinManager.changePin(context, "135790", "111111"))
    }

    @Test
    fun legacyPlaintextOrTamperedStore_isTreatedAsNoPin() {
        // A store written by an older version (unencrypted values) must not be trusted.
        context.getSharedPreferences("pocketpay_pin_store", Context.MODE_PRIVATE).edit()
            .putString("pin_salt", "c2FsdA==").putString("pin_hash", "aGFzaA==").commit()
        assertFalse(PinManager.isPinSet(context))
        assertEquals(PinResult.NotSet, PinManager.verify(context, "135790"))
    }

    @Test
    fun hashCannotBeSwappedBetweenPins() {
        PinManager.setInitialPin(context, "135790")
        val prefs = context.getSharedPreferences("pocketpay_pin_store", Context.MODE_PRIVATE)
        val oldHash = prefs.getString("pin_hash", null)!!
        PinManager.changePin(context, "135790", "246802")
        prefs.edit().putString("pin_hash", oldHash).commit() // roll back only the hash
        assertTrue(PinManager.verify(context, "135790") !is PinResult.Success)
    }
}
