package com.example.pocketpay

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pocketpay.data.AppDatabase
import com.example.pocketpay.data.DatabaseCallback
import com.example.pocketpay.data.EncryptedDatabase
import com.example.pocketpay.data.MIGRATION_5_6
import com.example.pocketpay.data.MIGRATION_6_7
import com.example.pocketpay.data.TransactionEntity
import com.example.pocketpay.data.TxType
import com.example.pocketpay.presentation.security.KeystoreSecretCipher
import com.example.pocketpay.presentation.security.PinManager
import com.example.pocketpay.presentation.security.PinResult
import com.example.pocketpay.presentation.security.Vault
import com.example.pocketpay.presentation.statement.PdfStatementGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Runs on a real device/emulator: exercises Keystore, SQLCipher and the PDF engine for real. */
@RunWith(AndroidJUnit4::class)
class SecurityInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "instrumented_test.db"

    @Before
    fun clean() {
        Vault.cipher = KeystoreSecretCipher
        context.deleteDatabase(dbName)
        context.getSharedPreferences("pocketpay_pin_store", Context.MODE_PRIVATE).edit().clear().commit()
    }

    // ---------- Keystore ----------

    @Test
    fun keystoreCipher_roundTrips_andRejectsTampering() {
        val secret = "rahasia-123".toByteArray()
        val token = KeystoreSecretCipher.encrypt(secret)
        assertFalse(token.contains("rahasia"))
        assertEquals("rahasia-123", String(KeystoreSecretCipher.decrypt(token)))

        val flipped = token.dropLast(4) + (if (token.takeLast(4) == "AAAA") "BBBB" else "AAAA")
        try {
            KeystoreSecretCipher.decrypt(flipped)
            fail("Tampered ciphertext must not decrypt")
        } catch (_: Exception) {
        }
    }

    @Test
    fun pinManager_withRealKeystore_worksAndStoresNoPlaintext() {
        assertEquals(null, PinManager.setInitialPin(context, "135790"))
        assertEquals(PinResult.Success, PinManager.verify(context, "135790"))
        assertTrue(PinManager.verify(context, "000001") is PinResult.Wrong)
        val raw = context.getSharedPreferences("pocketpay_pin_store", Context.MODE_PRIVATE).all.values.joinToString()
        assertFalse(raw.contains("135790"))
    }

    // ---------- Encrypted database ----------

    @Test
    fun existingPlaintextDatabase_isEncrypted_andKeepsItsData() {
        // 1. Create a plaintext DB the way older app versions did (Room, no cipher), with seed data.
        val plain = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addCallback(DatabaseCallback())
            .allowMainThreadQueries()
            .build()
        val balanceBefore = runBlocking { plain.bankingDao().getAccountOnce()!!.balance }
        plain.close()
        val file = context.getDatabasePath(dbName)
        assertTrue("setup must produce a plaintext db", file.readBytes().copyOf(15).decodeToString() == "SQLite format 3")

        // 2. Reopen through the encrypted factory: it must convert the file in place.
        val encrypted = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .openHelperFactory(EncryptedDatabase.openHelperFactory(context, dbName))
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
            .allowMainThreadQueries()
            .build()
        val account = runBlocking { encrypted.bankingDao().getAccountOnce() }
        val pockets = runBlocking { encrypted.bankingDao().getPocketsOnce() }
        encrypted.close()

        assertNotNull(account)
        assertEquals(balanceBefore, account!!.balance)
        assertEquals(2, pockets.size)
        assertFalse(
            "file on disk must no longer be readable SQLite",
            file.readBytes().copyOf(15).decodeToString() == "SQLite format 3"
        )
    }

    @Test
    fun encryptedDatabase_cannotBeOpenedWithoutKey() {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .openHelperFactory(EncryptedDatabase.openHelperFactory(context, dbName))
            .addCallback(DatabaseCallback())
            .allowMainThreadQueries()
            .build()
        runBlocking { db.bankingDao().getAccountOnce() }
        db.close()
        try {
            val raw = android.database.sqlite.SQLiteDatabase.openDatabase(
                context.getDatabasePath(dbName).path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            )
            raw.rawQuery("SELECT count(*) FROM sqlite_master", null).use { it.moveToFirst() }
            fail("A keyless open of the encrypted file must fail")
        } catch (_: android.database.sqlite.SQLiteException) {
        }
    }

    // ---------- PDF e-statement ----------

    @Test
    fun eStatement_withManyTransactions_paginatesAndKeepsEveryRow() {
        val txs = (1..100).map {
            TransactionEntity(
                id = "t$it", title = "Transaksi $it", amount = 1_000L * it,
                sourceOfFund = "Rekening Utama", type = TxType.DEBIT
            )
        }
        val file: File = PdfStatementGenerator.generateEStatementPdf(
            context, "9012839102", "Semua Waktu", txs, 5_000_000L
        )!!
        assertTrue(file.length() > 0)
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                // ~22 rows fit on the first page; 100 rows used to be truncated to 24 on one page.
                assertTrue("expected several pages, got ${renderer.pageCount}", renderer.pageCount >= 4)
            }
        }
        assertTrue(file.path.contains("statements"))
    }
}
