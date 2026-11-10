package com.example.pocketpay.domain.repository

import com.example.pocketpay.data.AccountEntity
import com.example.pocketpay.data.PocketEntity
import com.example.pocketpay.data.PocketType
import com.example.pocketpay.data.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface BankingRepository {
    fun getAccount(): Flow<AccountEntity?>
    fun getPockets(): Flow<List<PocketEntity>>
    fun getTransactions(): Flow<List<TransactionEntity>>

    suspend fun payFromPocket(pocket: PocketEntity, amount: Long, merchant: String, roundUpAmount: Long = 0L, authMethod: String = "Biometrik / PIN 6-Digit"): Result<Unit>
    suspend fun payFromAccount(amount: Long, merchant: String, roundUpAmount: Long = 0L, authMethod: String = "Biometrik / PIN 6-Digit"): Result<Unit>
    suspend fun payQrisCascading(pocket: PocketEntity?, amount: Long, merchant: String, roundUpAmount: Long = 0L, authMethod: String = "Biometrik / PIN 6-Digit", idempotencyKey: String): Result<Unit>
    suspend fun createAndAllocatePocket(name: String, amount: Long, type: PocketType, tenorMonths: Int? = null, targetAmount: Long? = null): Result<Unit>

    suspend fun topUpPocket(pocket: PocketEntity, amount: Long): Result<Unit>
    suspend fun withdrawFromPocket(pocket: PocketEntity, amount: Long): Result<Unit>

    suspend fun transferBetweenPockets(fromPocket: PocketEntity, toPocket: PocketEntity, amount: Long): Result<Unit>

    suspend fun closePocket(pocket: PocketEntity): Result<Unit>

    /** Credits any interest earned by pockets since the last accrual. Returns pockets credited. */
    suspend fun accrueInterest(): Result<Int>
}