package com.example.pocketpay

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.pocketpay.data.AccountEntity
import com.example.pocketpay.data.AppDatabase
import com.example.pocketpay.data.BankingError
import com.example.pocketpay.data.BankingException
import com.example.pocketpay.data.PocketEntity
import com.example.pocketpay.data.PocketType
import com.example.pocketpay.presentation.home.userMessage
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ErrorMessagesTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val fallback = R.string.vm_gagal_memproses_transaksi

    private fun messagesFor(qualifiers: String): List<String> {
        RuntimeEnvironment.setQualifiers(qualifiers)
        return BankingError.entries.map { BankingException(it, "Jajan").userMessage(context, fallback) }
    }

    @Test
    fun everyBankingError_hasADistinctMessage_inBothLanguages() {
        for (q in listOf("id", "en")) {
            val msgs = messagesFor(q)
            assertTrue("$q: blank message", msgs.none { it.isBlank() })
            assertEquals("$q: two errors share a message", msgs.size, msgs.toSet().size)
        }
    }

    @Test
    fun englishAndIndonesianMessagesDiffer() {
        assertNotEquals(messagesFor("id"), messagesFor("en"))
    }

    @Test
    fun detailIsInsertedIntoNamedMessages() {
        RuntimeEnvironment.setQualifiers("en")
        val msg = BankingException(BankingError.INSUFFICIENT_NAMED_POCKET, "Jajan Harian").userMessage(context, fallback)
        assertTrue(msg, msg.contains("Jajan Harian"))
    }

    @Test
    fun unknownExceptionsShowFallback_notInternals() {
        RuntimeEnvironment.setQualifiers("en")
        val msg = RuntimeException("SQLITE_CONSTRAINT: secret table name").userMessage(context, fallback)
        assertEquals(context.getString(fallback), msg)
        assertTrue(!msg.contains("SQLITE"))
    }

    // ---- the DAO raises the right typed error ----

    private lateinit var db: AppDatabase

    @Before
    fun openDb() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        runBlocking {
            db.bankingDao().insertAccount(AccountEntity(balance = 100_000L))
            db.bankingDao().insertPocket(
                PocketEntity("p", "Jajan", 10_000L, PocketType.SPENDING, 3.5)
            )
        }
    }

    @After
    fun closeDb() = db.close()

    private fun errorOf(block: suspend () -> Unit): BankingError {
        try {
            runBlocking { block() }
        } catch (e: BankingException) {
            return e.error
        }
        fail("expected a BankingException")
        error("unreachable")
    }

    @Test
    fun dao_reportsTypedErrors() {
        val dao = db.bankingDao()
        val pocket = runBlocking { dao.getPocketById("p")!! }
        val account = runBlocking { dao.getAccountOnce()!! }
        assertEquals(BankingError.AMOUNT_NOT_POSITIVE, errorOf { dao.topUpPocket(pocket, 0L, account) })
        assertEquals(BankingError.INSUFFICIENT_ACCOUNT_TOPUP, errorOf { dao.topUpPocket(pocket, 9_999_999L, account) })
        assertEquals(BankingError.SAME_POCKET, errorOf { dao.transferBetweenPockets(pocket, pocket, 1L) })
        assertEquals(
            BankingError.INSUFFICIENT_COMBINED,
            errorOf { dao.executeCascadingQrisPayment(pocket, account, 5_000_000L, 0L, "X", "PIN", "k1") }
        )
        runBlocking { dao.executeCascadingQrisPayment(pocket, account, 1_000L, 0L, "X", "PIN", "dup") }
        assertEquals(
            BankingError.DUPLICATE,
            errorOf { dao.executeCascadingQrisPayment(pocket, account, 1_000L, 0L, "X", "PIN", "dup") }
        )
    }
}
