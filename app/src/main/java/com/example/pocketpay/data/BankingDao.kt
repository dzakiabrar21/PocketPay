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

@Suppress("NAME_SHADOWING")
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
        if (paymentAmount <= 0) throw BankingException(BankingError.AMOUNT_NOT_POSITIVE)
        val pocket = getPocketById(pocket.id)
            ?: throw BankingException(BankingError.POCKET_NOT_FOUND)
        if (pocket.pocketType == PocketType.LOCKED_DEPOSIT) {
            throw BankingException(BankingError.DEPOSIT_NOT_FOR_QRIS)
        }
        if (pocket.balance < paymentAmount) {
            throw BankingException(BankingError.INSUFFICIENT_POCKET)
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
        if (paymentAmount <= 0) throw BankingException(BankingError.AMOUNT_NOT_POSITIVE)
        val account = getAccountOnce()
            ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND)
        if (account.balance < paymentAmount) {
            throw BankingException(BankingError.INSUFFICIENT_ACCOUNT)
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
        if (newPocket.balance <= 0) throw BankingException(BankingError.AMOUNT_NOT_POSITIVE)
        val account = getAccountOnce()
            ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND)
        if (currentAccount.balance < newPocket.balance) {
            throw BankingException(BankingError.INSUFFICIENT_ACCOUNT)
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
        if (amount <= 0) throw BankingException(BankingError.AMOUNT_NOT_POSITIVE)
        val account = getAccountOnce()
            ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND)
        val pocket = getPocketById(pocket.id)
            ?: throw BankingException(BankingError.POCKET_NOT_FOUND)
        if (account.balance < amount) {
            throw BankingException(BankingError.INSUFFICIENT_ACCOUNT_TOPUP)
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
        if (amount <= 0) throw BankingException(BankingError.AMOUNT_NOT_POSITIVE)
        val account = getAccountOnce()
            ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND)
        val pocket = getPocketById(pocket.id)
            ?: throw BankingException(BankingError.POCKET_NOT_FOUND)
        val currentTime = System.currentTimeMillis()

        if (pocket.pocketType == PocketType.LOCKED_DEPOSIT && (pocket.maturityDate ?: 0L) > currentTime) {
            throw BankingException(BankingError.DEPOSIT_LOCKED)
        }

        if (pocket.balance < amount) {
            throw BankingException(BankingError.INSUFFICIENT_POCKET)
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
        val account = getAccountOnce()
            ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND)
        val pocket = getPocketById(pocket.id)
            ?: throw BankingException(BankingError.POCKET_NOT_FOUND)
        val currentTime = System.currentTimeMillis()

        if (pocket.pocketType == PocketType.LOCKED_DEPOSIT && (pocket.maturityDate ?: 0L) > currentTime) {
            throw BankingException(BankingError.DEPOSIT_LOCKED_CLOSE)
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
        if (amount <= 0) throw BankingException(BankingError.AMOUNT_NOT_POSITIVE)
        if (fromPocket.id == toPocket.id) {
            throw BankingException(BankingError.SAME_POCKET)
        }
        val fromPocket = getPocketById(fromPocket.id)
            ?: throw BankingException(BankingError.SOURCE_POCKET_NOT_FOUND)
        val toPocket = getPocketById(toPocket.id)
            ?: throw BankingException(BankingError.DEST_POCKET_NOT_FOUND)
        if (fromPocket.pocketType == PocketType.LOCKED_DEPOSIT) {
            throw BankingException(BankingError.SOURCE_LOCKED)
        }
        if (toPocket.pocketType == PocketType.LOCKED_DEPOSIT) {
            throw BankingException(BankingError.DEST_LOCKED)
        }
        if (fromPocket.balance < amount) {
            throw BankingException(BankingError.INSUFFICIENT_NAMED_POCKET, fromPocket.name)
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
        if (paymentAmount <= 0) throw BankingException(BankingError.AMOUNT_NOT_POSITIVE)
        val account = account?.let { getAccountOnce() ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND) }
        val pocket = pocket?.let { getPocketById(it.id) ?: throw BankingException(BankingError.POCKET_NOT_FOUND) }
        if (pocket != null) {
            if (pocket.pocketType == PocketType.LOCKED_DEPOSIT) {
                throw BankingException(BankingError.DEPOSIT_NOT_FOR_QRIS)
            }
            if (pocket.balance < paymentAmount) {
                throw BankingException(BankingError.INSUFFICIENT_POCKET)
            }
            updatePocket(pocket.copy(balance = pocket.balance - paymentAmount))
            insertTransaction(paymentTxRecord)
        } else if (account != null) {
            if (account.balance < paymentAmount + roundUpAmount) {
                throw BankingException(BankingError.INSUFFICIENT_ACCOUNT)
            }
            updateAccount(account.copy(balance = account.balance - paymentAmount))
            insertTransaction(paymentTxRecord)
        } else {
            throw BankingException(BankingError.INVALID_SOURCE)
        }

        if (roundUpAmount > 0) {
            val currentAccount = getAccountOnce()
                ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND)

            if (currentAccount.balance < roundUpAmount) {
                throw BankingException(BankingError.INSUFFICIENT_ROUNDUP)
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

    /**
     * Credits simple daily interest (balance x rate / 365) for every full day elapsed since the
     * pocket was last credited. Only whole days are consumed, so the remainder carries over and
     * calling this repeatedly never pays twice for the same day. Returns the number of pockets credited.
     */
    @Transaction
    open suspend fun accrueInterest(now: Long): Int {
        val dayMs = 24L * 60 * 60 * 1000
        var credited = 0
        for (pocket in getPocketsOnce()) {
            if (pocket.lastInterestAt <= 0L) {
                updatePocket(pocket.copy(lastInterestAt = now))
                continue
            }
            val days = ((now - pocket.lastInterestAt) / dayMs).coerceAtMost(365L)
            if (days < 1L) continue
            val newMarker = pocket.lastInterestAt + ((now - pocket.lastInterestAt) / dayMs) * dayMs
            // BigDecimal avoids floating-point drift (e.g. 29999.9999 instead of 30000) on money.
            val interest = java.math.BigDecimal.valueOf(pocket.balance)
                .multiply(java.math.BigDecimal.valueOf(pocket.interestRatePercent))
                .multiply(java.math.BigDecimal.valueOf(days))
                .divide(java.math.BigDecimal.valueOf(36_500L), 0, java.math.RoundingMode.DOWN)
                .toLong()
            if (interest > 0L) {
                updatePocket(pocket.copy(balance = pocket.balance + interest, lastInterestAt = newMarker))
                insertTransaction(
                    TransactionEntity(
                        id = "interest_${pocket.id}_$newMarker",
                        title = "Bunga ${pocket.name} ($days hari)",
                        amount = interest,
                        sourceOfFund = "Bunga ${pocket.interestRatePercent}% p.a.",
                        type = TxType.CREDIT,
                        timestamp = now,
                        authMethod = "Otomatis"
                    )
                )
                credited++
            } else {
                updatePocket(pocket.copy(lastInterestAt = newMarker))
            }
        }
        return credited
    }

    @Query("SELECT * FROM pockets WHERE id = :id LIMIT 1")
    abstract suspend fun getPocketById(id: String): PocketEntity?

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    abstract suspend fun updateTransactionStatus(id: String, status: TxStatus): Int

    /**
     * Reverses a successful payment: the amount returns to the Main Account, the original becomes
     * REVERSED and a CREDIT "Refund" entry is recorded. Atomic, and idempotent because the refund
     * entry has a deterministic id (a second attempt hits the not-refundable check).
     * The round-up that accompanied the payment is intentionally kept in the savings pocket.
     */
    @Transaction
    open suspend fun refundTransaction(txId: String, now: Long) {
        val tx = getTransactionById(txId) ?: throw BankingException(BankingError.TX_NOT_FOUND)
        if (tx.type != TxType.DEBIT || tx.status != TxStatus.SUCCESS) {
            throw BankingException(BankingError.NOT_REFUNDABLE)
        }
        if (now - tx.timestamp > REFUND_WINDOW_MS) throw BankingException(BankingError.REFUND_EXPIRED)
        val account = getAccountOnce() ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND)

        updateAccount(account.copy(balance = account.balance + tx.amount))
        updateTransactionStatus(txId, TxStatus.REVERSED)
        insertTransaction(
            TransactionEntity(
                id = "refund_$txId",
                title = "Refund: ${tx.title}",
                amount = tx.amount,
                sourceOfFund = "Pengembalian ke Rekening Utama",
                type = TxType.CREDIT,
                status = TxStatus.SUCCESS,
                timestamp = now,
                authMethod = "Otomatis"
            )
        )
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
        if (paymentAmount <= 0) throw BankingException(BankingError.AMOUNT_NOT_POSITIVE)
        val account = getAccountOnce()
            ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND)
        val pocket = pocket?.let { getPocketById(it.id) ?: throw BankingException(BankingError.POCKET_NOT_FOUND) }
        val existingTx = getTransactionById(idempotencyKey)
        if (existingTx != null) {
            throw BankingException(BankingError.DUPLICATE, idempotencyKey)
        }

        var sourceOfFundText: String

        if (pocket != null) {
            if (pocket.pocketType == PocketType.LOCKED_DEPOSIT) {
                throw BankingException(BankingError.DEPOSIT_NOT_FOR_QRIS)
            }

            if (pocket.balance >= paymentAmount) {
                updatePocket(pocket.copy(balance = pocket.balance - paymentAmount))
                sourceOfFundText = "Kantong: ${pocket.name}"
            } else {
                val pocketBalance = pocket.balance
                val deficit = paymentAmount - pocketBalance

                if (account.balance < deficit + roundUpAmount) {
                    throw BankingException(BankingError.INSUFFICIENT_COMBINED, pocket.name)
                }

                updatePocket(pocket.copy(balance = 0L))
                updateAccount(account.copy(balance = account.balance - deficit))
                sourceOfFundText = "Kantong: ${pocket.name} (Rp ${pocketBalance}) + Rekening Utama (Rp ${deficit})"
            }
        } else {
            if (account.balance < paymentAmount + roundUpAmount) {
                throw BankingException(BankingError.INSUFFICIENT_ACCOUNT)
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
                ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND)

            if (updatedAccount.balance < roundUpAmount) {
                throw BankingException(BankingError.INSUFFICIENT_ROUNDUP)
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