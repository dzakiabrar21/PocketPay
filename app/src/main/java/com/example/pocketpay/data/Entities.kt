package com.example.pocketpay.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Primary user bank account entity.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String = "user_primary_account",
    val accountNumber: String = "9012839102",
    val balance: Long // Balance stored in exact integer amount (Long) to prevent floating-point precision errors
)

/**
 * Categories for financial pockets.
 */
enum class PocketType {
    SPENDING,       // Spending pocket for QRIS payments
    LOCKED_DEPOSIT  // Fixed deposit pocket with locked tenor and higher yield
}

@Entity(tableName = "pockets")
data class PocketEntity(
    @PrimaryKey val id: String,
    val name: String,
    val balance: Long,
    val pocketType: PocketType,
    val interestRatePercent: Double,
    val tenorMonths: Int? = null,           // Tenor duration in months (e.g., 1, 3, 6; null for SPENDING)
    val maturityDate: Long? = null,         // Epoch timestamp in milliseconds for maturity
    val lastInterestAt: Long = System.currentTimeMillis(), // Last moment up to which interest was credited
    val targetAmount: Long? = null          // Optional savings goal (SPENDING pockets only)
)

/**
 * Transaction classification types.
 */
enum class TxType {
    DEBIT,          // Outgoing payment (e.g., QRIS)
    CREDIT,         // Incoming funds
    REALLOCATION    // Internal transfer between pockets
}

/**
 * Transaction payment state machine statuses.
 */
enum class TxStatus {
    SUCCESS,
    PENDING,
    REVERSED
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String, // Unique UUID acting as Idempotency Key
    val title: String,
    val amount: Long,
    val sourceOfFund: String,
    val type: TxType,
    val status: TxStatus = TxStatus.SUCCESS,
    val timestamp: Long = System.currentTimeMillis(),
    val referenceNo: String = "REF-${System.currentTimeMillis().toString().takeLast(8)}",
    val authMethod: String = "Biometrik / PIN 6-Digit",
    val idempotencyKey: String = id
)