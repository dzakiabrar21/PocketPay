package com.example.pocketpay.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
abstract class BankingDao {

    @Query("SELECT * FROM accounts WHERE id = 'user_primary_account'")
    abstract fun getAccountFlow(): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = 'user_primary_account' LIMIT 1")
    abstract suspend fun getAccountOnce(): AccountEntity?

    @Query("SELECT * FROM pockets")
    abstract fun getAllPocketsFlow(): Flow<List<PocketEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    abstract fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Update
    abstract suspend fun updateAccount(account: AccountEntity): Int

    @Update
    abstract suspend fun updatePocket(pocket: PocketEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPocket(pocket: PocketEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAccount(account: AccountEntity): Long

    /**
     * Executes QRIS payment from a spending pocket.
     */
    @Transaction
    open suspend fun executePaymentFromPocket(
        pocket: PocketEntity,
        paymentAmount: Long,
        transactionRecord: TransactionEntity
    ) {
        if (pocket.pocketType == PocketType.LOCKED_DEPOSIT) {
            throw IllegalStateException("Locked deposit pockets cannot be used for QRIS payments.")
        }
        if (pocket.balance < paymentAmount) {
            throw IllegalStateException("Insufficient pocket balance.")
        }

        val updatedPocket = pocket.copy(balance = pocket.balance - paymentAmount)
        updatePocket(updatedPocket)
        insertTransaction(transactionRecord)
    }

    /**
     * Executes QRIS payment from the primary account.
     */
    @Transaction
    open suspend fun executePaymentFromAccount(
        account: AccountEntity,
        paymentAmount: Long,
        transactionRecord: TransactionEntity
    ) {
        if (account.balance < paymentAmount) {
            throw IllegalStateException("Insufficient primary account balance.")
        }

        val updatedAccount = account.copy(balance = account.balance - paymentAmount)
        updateAccount(updatedAccount)
        insertTransaction(transactionRecord)
    }

    /**
     * Allocates funds from primary account to create a new pocket.
     */
    @Transaction
    open suspend fun allocateToPocket(
        currentAccount: AccountEntity,
        newPocket: PocketEntity
    ) {
        if (currentAccount.balance < newPocket.balance) {
            throw IllegalStateException("Insufficient primary account balance.")
        }

        updateAccount(currentAccount.copy(balance = currentAccount.balance - newPocket.balance))
        insertPocket(newPocket)
        insertTransaction(
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                title = "Alokasi ke ${newPocket.name}",
                amount = newPocket.balance,
                sourceOfFund = "Rekening Utama",
                type = TxType.REALLOCATION
            )
        )
    }

    /**
     * Tops up a pocket using funds from the primary account.
     */
    @Transaction
    open suspend fun topUpPocket(
        pocket: PocketEntity,
        amount: Long,
        account: AccountEntity
    ) {
        if (account.balance < amount) {
            throw IllegalStateException("Insufficient primary account balance for top-up.")
        }

        updateAccount(account.copy(balance = account.balance - amount))
        updatePocket(pocket.copy(balance = pocket.balance + amount))
        insertTransaction(
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                title = "Top-up ke ${pocket.name}",
                amount = amount,
                sourceOfFund = "Rekening Utama",
                type = TxType.REALLOCATION
            )
        )
    }

    /**
     * Withdraws funds from a pocket back to the primary account with maturity checks.
     */
    @Transaction
    open suspend fun withdrawFromPocket(
        pocket: PocketEntity,
        amount: Long,
        account: AccountEntity
    ) {
        val currentTime = System.currentTimeMillis()

        if (pocket.pocketType == PocketType.LOCKED_DEPOSIT && (pocket.maturityDate ?: 0L) > currentTime) {
            throw IllegalStateException("Deposit pocket is locked until maturity date.")
        }

        if (pocket.balance < amount) {
            throw IllegalStateException("Insufficient pocket balance.")
        }

        updatePocket(pocket.copy(balance = pocket.balance - amount))
        updateAccount(account.copy(balance = account.balance + amount))
        insertTransaction(
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                title = "Tarik Saldo dari ${pocket.name}",
                amount = amount,
                sourceOfFund = "Kantong: ${pocket.name}",
                type = TxType.CREDIT
            )
        )
    }

    @Delete
    abstract suspend fun deletePocket(pocket: PocketEntity): Int

    /**
     * Closes a pocket and refunds all remaining balance to the primary account.
     */
    @Transaction
    open suspend fun closeAndRefundPocket(
        pocket: PocketEntity,
        account: AccountEntity
    ) {
        val currentTime = System.currentTimeMillis()

        if (pocket.pocketType == PocketType.LOCKED_DEPOSIT && (pocket.maturityDate ?: 0L) > currentTime) {
            throw IllegalStateException("Deposit pocket is locked until maturity date and cannot be closed.")
        }

        if (pocket.balance > 0) {
            updateAccount(account.copy(balance = account.balance + pocket.balance))
            insertTransaction(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Penutupan Kantong: ${pocket.name}",
                    amount = pocket.balance,
                    sourceOfFund = "Sisa Saldo Kantong",
                    type = TxType.CREDIT
                )
            )
        }

        deletePocket(pocket)
    }

    /**
     * Executes direct transfer between two spending pockets.
     */
    @Transaction
    open suspend fun transferBetweenPockets(
        fromPocket: PocketEntity,
        toPocket: PocketEntity,
        amount: Long
    ) {
        if (fromPocket.pocketType == PocketType.LOCKED_DEPOSIT) {
            throw IllegalStateException("Source pocket is locked and cannot transfer funds.")
        }
        if (toPocket.pocketType == PocketType.LOCKED_DEPOSIT) {
            throw IllegalStateException("Destination pocket is locked and cannot receive direct transfers.")
        }
        if (fromPocket.balance < amount) {
            throw IllegalStateException("Insufficient balance in ${fromPocket.name}.")
        }

        updatePocket(fromPocket.copy(balance = fromPocket.balance - amount))
        updatePocket(toPocket.copy(balance = toPocket.balance + amount))
        insertTransaction(
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                title = "Transfer: ${fromPocket.name} ➔ ${toPocket.name}",
                amount = amount,
                sourceOfFund = "Kantong: ${fromPocket.name}",
                type = TxType.REALLOCATION
            )
        )
    }

    @Query("SELECT * FROM pockets")
    abstract suspend fun getPocketsOnce(): List<PocketEntity>

    /**
     * Executes QRIS payment with round-up micro-savings.
     */
    @Transaction
    open suspend fun executePaymentWithRoundUp(
        account: AccountEntity?,
        pocket: PocketEntity?,
        paymentAmount: Long,
        roundUpAmount: Long,
        paymentTxRecord: TransactionEntity
    ) {
        if (pocket != null) {
            if (pocket.pocketType == PocketType.LOCKED_DEPOSIT) {
                throw IllegalStateException("Locked deposit pockets cannot be used for QRIS payments.")
            }
            if (pocket.balance < paymentAmount) {
                throw IllegalStateException("Insufficient pocket balance.")
            }
            updatePocket(pocket.copy(balance = pocket.balance - paymentAmount))
            insertTransaction(paymentTxRecord)
        } else if (account != null) {
            if (account.balance < paymentAmount + roundUpAmount) {
                throw IllegalStateException("Insufficient primary account balance.")
            }
            updateAccount(account.copy(balance = account.balance - paymentAmount))
            insertTransaction(paymentTxRecord)
        } else {
            throw IllegalStateException("Invalid payment source.")
        }

        if (roundUpAmount > 0) {
            val currentAccount = getAccountOnce()
                ?: throw IllegalStateException("Primary account not found.")

            if (currentAccount.balance < roundUpAmount) {
                throw IllegalStateException("Insufficient primary account balance for round-up.")
            }

            updateAccount(currentAccount.copy(balance = currentAccount.balance - roundUpAmount))

            val allPockets = getPocketsOnce()
            var celenganPocket = allPockets.find { it.name.equals("Celengan Receh", ignoreCase = true) }

            if (celenganPocket == null) {
                celenganPocket = PocketEntity(
                    id = "pocket_celengan_receh",
                    name = "Celengan Receh",
                    balance = roundUpAmount,
                    pocketType = PocketType.SPENDING,
                    interestRatePercent = 3.5,
                    tenorMonths = null,
                    maturityDate = null
                )
                insertPocket(celenganPocket)
            } else {
                updatePocket(celenganPocket.copy(balance = celenganPocket.balance + roundUpAmount))
            }

            insertTransaction(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Celengan Receh (Pembulatan QRIS)",
                    amount = roundUpAmount,
                    sourceOfFund = "Rekening Utama",
                    type = TxType.REALLOCATION,
                    authMethod = paymentTxRecord.authMethod
                )
            )
        }
    }

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    abstract suspend fun getTransactionById(id: String): TransactionEntity?

    /**
     * Atomic Smart Cascading Payment with multi-source fallback, round-up savings, idempotency check, and state machine logging.
     */
    @Transaction
    open suspend fun executeCascadingQrisPayment(
        pocket: PocketEntity?,
        account: AccountEntity,
        paymentAmount: Long,
        roundUpAmount: Long,
        merchant: String,
        authMethod: String,
        idempotencyKey: String
    ) {
        val existingTx = getTransactionById(idempotencyKey)
        if (existingTx != null) {
            throw IllegalStateException("Duplicate transaction detected (Idempotency Key: $idempotencyKey). Payment rejected.")
        }

        var sourceOfFundText: String

        if (pocket != null) {
            if (pocket.pocketType == PocketType.LOCKED_DEPOSIT) {
                throw IllegalStateException("Locked deposit pockets cannot be used for QRIS payments.")
            }

            if (pocket.balance >= paymentAmount) {
                updatePocket(pocket.copy(balance = pocket.balance - paymentAmount))
                sourceOfFundText = "Kantong: ${pocket.name}"
            } else {
                val pocketBalance = pocket.balance
                val deficit = paymentAmount - pocketBalance

                if (account.balance < deficit + roundUpAmount) {
                    throw IllegalStateException("Combined balance of Pocket (${pocket.name}) & Primary Account is insufficient.")
                }

                updatePocket(pocket.copy(balance = 0L))
                updateAccount(account.copy(balance = account.balance - deficit))
                sourceOfFundText = "Kantong: ${pocket.name} (Rp ${pocketBalance}) + Rekening Utama (Rp ${deficit})"
            }
        } else {
            if (account.balance < paymentAmount + roundUpAmount) {
                throw IllegalStateException("Primary account balance is insufficient.")
            }
            updateAccount(account.copy(balance = account.balance - paymentAmount))
            sourceOfFundText = "Rekening Utama"
        }

        val mainTx = TransactionEntity(
            id = idempotencyKey,
            title = "QRIS - $merchant",
            amount = paymentAmount,
            sourceOfFund = sourceOfFundText,
            type = TxType.DEBIT,
            status = TxStatus.SUCCESS,
            authMethod = authMethod,
            idempotencyKey = idempotencyKey
        )
        insertTransaction(mainTx)

        if (roundUpAmount > 0) {
            val updatedAccount = getAccountOnce()
                ?: throw IllegalStateException("Primary account not found.")

            if (updatedAccount.balance < roundUpAmount) {
                throw IllegalStateException("Primary account balance is insufficient for round-up micro-savings.")
            }

            updateAccount(updatedAccount.copy(balance = updatedAccount.balance - roundUpAmount))

            val allPockets = getPocketsOnce()
            var celenganPocket = allPockets.find { it.name.equals("Celengan Receh", ignoreCase = true) }

            if (celenganPocket == null) {
                celenganPocket = PocketEntity(
                    id = "pocket_celengan_receh",
                    name = "Celengan Receh",
                    balance = roundUpAmount,
                    pocketType = PocketType.SPENDING,
                    interestRatePercent = 3.5,
                    tenorMonths = null,
                    maturityDate = null
                )
                insertPocket(celenganPocket)
            } else {
                updatePocket(celenganPocket.copy(balance = celenganPocket.balance + roundUpAmount))
            }

            val roundUpKey = "${idempotencyKey}_roundup"
            insertTransaction(
                TransactionEntity(
                    id = roundUpKey,
                    title = "Celengan Receh (Round-Up QRIS)",
                    amount = roundUpAmount,
                    sourceOfFund = "Rekening Utama",
                    type = TxType.REALLOCATION,
                    status = TxStatus.SUCCESS,
                    authMethod = authMethod,
                    idempotencyKey = roundUpKey
                )
            )
        }
    }
}