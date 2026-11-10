package com.example.pocketpay.data.repository

import com.example.pocketpay.data.AccountEntity
import com.example.pocketpay.data.BankingDao
import com.example.pocketpay.data.BankingError
import com.example.pocketpay.data.BankingException
import com.example.pocketpay.data.PocketEntity
import com.example.pocketpay.data.PocketType
import com.example.pocketpay.data.TransactionEntity
import com.example.pocketpay.data.TxType
import com.example.pocketpay.domain.repository.BankingRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankingRepositoryImpl @Inject constructor(
    private val dao: BankingDao
) : BankingRepository {

    override fun getAccount(): Flow<AccountEntity?> = dao.getAccountFlow()

    override fun getPockets(): Flow<List<PocketEntity>> = dao.getAllPocketsFlow()

    override fun getTransactions(): Flow<List<TransactionEntity>> = dao.getAllTransactionsFlow()

    override suspend fun payFromPocket(
        pocket: PocketEntity,
        amount: Long,
        merchant: String,
        roundUpAmount: Long,
        authMethod: String
    ): Result<Unit> = runCatching {
        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            title = "QRIS - $merchant",
            amount = amount,
            sourceOfFund = "Kantong: ${pocket.name}",
            type = TxType.DEBIT,
            authMethod = authMethod
        )
        dao.executePaymentWithRoundUp(
            account = null,
            pocket = pocket,
            paymentAmount = amount,
            roundUpAmount = roundUpAmount,
            paymentTxRecord = tx
        )
    }

    override suspend fun payFromAccount(
        amount: Long,
        merchant: String,
        roundUpAmount: Long,
        authMethod: String
    ): Result<Unit> = runCatching {
        val account = dao.getAccountOnce()
            ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND)
        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            title = "QRIS - $merchant",
            amount = amount,
            sourceOfFund = "Rekening Utama",
            type = TxType.DEBIT,
            authMethod = authMethod
        )
        dao.executePaymentWithRoundUp(
            account = account,
            pocket = null,
            paymentAmount = amount,
            roundUpAmount = roundUpAmount,
            paymentTxRecord = tx
        )
    }

    override suspend fun payQrisCascading(
        pocket: PocketEntity?,
        amount: Long,
        merchant: String,
        roundUpAmount: Long,
        authMethod: String,
        idempotencyKey: String
    ): Result<Unit> = runCatching {
        val account = dao.getAccountOnce()
            ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND)
        dao.executeCascadingQrisPayment(
            pocket = pocket,
            account = account,
            paymentAmount = amount,
            roundUpAmount = roundUpAmount,
            merchant = merchant,
            authMethod = authMethod,
            idempotencyKey = idempotencyKey
        )
    }

    override suspend fun createAndAllocatePocket(
        name: String,
        amount: Long,
        type: PocketType,
        tenorMonths: Int?,
        targetAmount: Long?
    ): Result<Unit> = runCatching {
        val account = dao.getAccountOnce()
            ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND)

        val interestRate = if (type == PocketType.LOCKED_DEPOSIT) 6.0 else 3.5

        val maturityDate = if (type == PocketType.LOCKED_DEPOSIT && tenorMonths != null) {
            val millisInMonth = 30L * 24 * 60 * 60 * 1000
            System.currentTimeMillis() + (tenorMonths * millisInMonth)
        } else {
            null
        }

        val newPocket = PocketEntity(
            id = "pocket_${UUID.randomUUID().toString().take(8)}",
            name = name,
            balance = amount,
            pocketType = type,
            interestRatePercent = interestRate,
            tenorMonths = tenorMonths,
            maturityDate = maturityDate,
            targetAmount = targetAmount?.takeIf { it > 0 && type == PocketType.SPENDING }
        )

        dao.allocateToPocket(account, newPocket)
    }

    override suspend fun topUpPocket(
        pocket: PocketEntity,
        amount: Long
    ): Result<Unit> = runCatching {
        val account = dao.getAccountOnce()
            ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND)
        dao.topUpPocket(pocket, amount, account)
    }

    override suspend fun withdrawFromPocket(
        pocket: PocketEntity,
        amount: Long
    ): Result<Unit> = runCatching {
        val account = dao.getAccountOnce()
            ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND)
        dao.withdrawFromPocket(pocket, amount, account)
    }

    override suspend fun transferBetweenPockets(
        fromPocket: PocketEntity,
        toPocket: PocketEntity,
        amount: Long
    ): Result<Unit> = runCatching {
        dao.transferBetweenPockets(fromPocket, toPocket, amount)
    }

    override suspend fun refundTransaction(transaction: TransactionEntity): Result<Unit> = runCatching {
        dao.refundTransaction(transaction.id, System.currentTimeMillis())
    }

    override suspend fun accrueInterest(): Result<Int> = runCatching {
        dao.accrueInterest(System.currentTimeMillis())
    }

    override suspend fun closePocket(pocket: PocketEntity): Result<Unit> = runCatching {
        val account = dao.getAccountOnce()
            ?: throw BankingException(BankingError.ACCOUNT_NOT_FOUND)
        dao.closeAndRefundPocket(pocket, account)
    }
}