package com.example.pocketpay

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.pocketpay.data.AccountEntity
import com.example.pocketpay.data.AppDatabase
import com.example.pocketpay.data.BankingDao
import com.example.pocketpay.data.PocketEntity
import com.example.pocketpay.data.PocketType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BankingDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BankingDao

    private val spending = PocketEntity(
        id = "p_spend", name = "Jajan", balance = 100_000L,
        pocketType = PocketType.SPENDING, interestRatePercent = 3.5
    )
    private val spending2 = PocketEntity(
        id = "p_spend2", name = "Belanja", balance = 50_000L,
        pocketType = PocketType.SPENDING, interestRatePercent = 3.5
    )
    private val lockedDeposit = PocketEntity(
        id = "p_dep", name = "Deposito", balance = 500_000L,
        pocketType = PocketType.LOCKED_DEPOSIT, interestRatePercent = 6.0,
        tenorMonths = 3, maturityDate = System.currentTimeMillis() + 90L * 24 * 3600 * 1000
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.bankingDao()
        runBlocking {
            dao.insertAccount(AccountEntity(balance = 1_000_000L))
            dao.insertPocket(spending)
            dao.insertPocket(spending2)
            dao.insertPocket(lockedDeposit)
        }
    }

    @After
    fun tearDown() = db.close()

    private fun account() = runBlocking { dao.getAccountOnce()!! }
    private fun pocket(id: String) = runBlocking { dao.getPocketById(id) }
    private fun txCount(): Int =
        db.query("SELECT COUNT(*) FROM transactions", null).use { it.moveToFirst(); it.getInt(0) }

    private fun expectFailure(block: suspend () -> Unit) {
        try {
            runBlocking { block() }
            fail("Expected the operation to be rejected")
        } catch (e: IllegalStateException) {
            // expected: business rule violated
        } catch (e: IllegalArgumentException) {
            // expected: invalid input (e.g. non-positive amount)
        }
    }

    private fun pay(
        pocket: PocketEntity?, amount: Long, roundUp: Long = 0L, key: String = "key-1"
    ) = runBlocking {
        dao.executeCascadingQrisPayment(
            pocket = pocket, account = account(), paymentAmount = amount,
            roundUpAmount = roundUp, merchant = "Kantin", authMethod = "PIN",
            idempotencyKey = key
        )
    }

    // ---------- Cascading payment ----------

    @Test
    fun pay_fromPocketWithEnoughBalance_onlyDebitsPocket() {
        pay(spending, 40_000L)
        assertEquals(60_000L, pocket("p_spend")!!.balance)
        assertEquals(1_000_000L, account().balance)
    }

    @Test
    fun pay_withInsufficientPocket_cascadesDeficitToAccount() {
        pay(spending, 150_000L)
        assertEquals(0L, pocket("p_spend")!!.balance)
        assertEquals(950_000L, account().balance)
    }

    @Test
    fun pay_withRoundUp_movesChangeToCelenganReceh() {
        pay(null, 23_500L, roundUp = 1_500L)
        assertEquals(1_000_000L - 23_500L - 1_500L, account().balance)
        assertEquals(1_500L, pocket("pocket_celengan_receh")!!.balance)
    }

    @Test
    fun pay_roundUpTwice_accumulatesInSameCelengan() {
        pay(null, 23_500L, roundUp = 1_500L, key = "a")
        pay(null, 12_000L, roundUp = 3_000L, key = "b")
        assertEquals(4_500L, pocket("pocket_celengan_receh")!!.balance)
    }

    @Test
    fun pay_whenCombinedBalanceInsufficient_changesNothing() {
        val before = txCount()
        expectFailure { pay(spending, 5_000_000L) }
        assertEquals(100_000L, pocket("p_spend")!!.balance)
        assertEquals(1_000_000L, account().balance)
        assertEquals(before, txCount())
    }

    @Test
    fun pay_duplicateIdempotencyKey_isRejectedAndChargedOnce() {
        pay(null, 10_000L, key = "same")
        expectFailure { pay(null, 10_000L, key = "same") }
        assertEquals(990_000L, account().balance)
    }

    @Test
    fun pay_fromLockedDeposit_isRejected() {
        expectFailure { pay(lockedDeposit, 10_000L) }
        assertEquals(500_000L, pocket("p_dep")!!.balance)
    }

    @Test
    fun pay_staleObjectWithInflatedBalance_cannotOverspend() {
        // Caller holds an outdated copy claiming 10M, but the real balance is 100k.
        val stale = spending.copy(balance = 10_000_000L)
        pay(stale, 150_000L)
        // The real (fresh) balance was used, so the deficit was cascaded to the account.
        assertEquals(0L, pocket("p_spend")!!.balance)
        assertEquals(950_000L, account().balance)
    }

    @Test
    fun pay_zeroOrNegativeAmount_isRejected() {
        expectFailure { pay(null, 0L, key = "z") }
        expectFailure { pay(null, -5_000L, key = "n") }
        assertEquals(1_000_000L, account().balance)
    }

    // ---------- Pocket operations ----------

    @Test
    fun topUp_movesMoneyFromAccountToPocket() {
        runBlocking { dao.topUpPocket(spending, 25_000L, account()) }
        assertEquals(125_000L, pocket("p_spend")!!.balance)
        assertEquals(975_000L, account().balance)
    }

    @Test
    fun topUp_negativeAmount_isRejected() {
        expectFailure { dao.topUpPocket(spending, -50_000L, account()) }
        assertEquals(100_000L, pocket("p_spend")!!.balance)
        assertEquals(1_000_000L, account().balance)
    }

    @Test
    fun topUp_moreThanAccountBalance_isRejected() {
        expectFailure { dao.topUpPocket(spending, 2_000_000L, account()) }
        assertEquals(1_000_000L, account().balance)
    }

    @Test
    fun withdraw_fromLockedDepositBeforeMaturity_isRejected() {
        expectFailure { dao.withdrawFromPocket(lockedDeposit, 1_000L, account()) }
        assertEquals(500_000L, pocket("p_dep")!!.balance)
    }

    @Test
    fun withdraw_fromSpending_returnsMoneyToAccount() {
        runBlocking { dao.withdrawFromPocket(spending, 30_000L, account()) }
        assertEquals(70_000L, pocket("p_spend")!!.balance)
        assertEquals(1_030_000L, account().balance)
    }

    @Test
    fun transfer_betweenSpendingPockets_preservesTotal() {
        runBlocking { dao.transferBetweenPockets(spending, spending2, 20_000L) }
        assertEquals(80_000L, pocket("p_spend")!!.balance)
        assertEquals(70_000L, pocket("p_spend2")!!.balance)
    }

    @Test
    fun transfer_toSamePocket_isRejected() {
        expectFailure { dao.transferBetweenPockets(spending, spending, 1_000L) }
        assertEquals(100_000L, pocket("p_spend")!!.balance)
    }

    @Test
    fun transfer_involvingLockedDeposit_isRejected() {
        expectFailure { dao.transferBetweenPockets(spending, lockedDeposit, 1_000L) }
        expectFailure { dao.transferBetweenPockets(lockedDeposit, spending, 1_000L) }
    }

    @Test
    fun closePocket_refundsBalanceAndDeletesPocket() {
        runBlocking { dao.closeAndRefundPocket(spending, account()) }
        assertNull(pocket("p_spend"))
        assertEquals(1_100_000L, account().balance)
    }

    @Test
    fun closePocket_lockedDepositBeforeMaturity_isRejected() {
        expectFailure { dao.closeAndRefundPocket(lockedDeposit, account()) }
        assertNotNull(pocket("p_dep"))
    }

    @Test
    fun allocate_newPocket_debitsAccountAndRecordsReallocation() {
        val newPocket = PocketEntity(
            id = "p_new", name = "Liburan", balance = 200_000L,
            pocketType = PocketType.SPENDING, interestRatePercent = 3.5
        )
        runBlocking { dao.allocateToPocket(account(), newPocket) }
        assertEquals(800_000L, account().balance)
        assertEquals(200_000L, pocket("p_new")!!.balance)
        assertTrue(txCount() > 0)
    }
}
