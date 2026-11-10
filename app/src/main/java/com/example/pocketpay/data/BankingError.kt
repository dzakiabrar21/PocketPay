package com.example.pocketpay.data

/** Business-rule failures of banking operations. The UI maps each code to a localized message. */
enum class BankingError {
    AMOUNT_NOT_POSITIVE,
    ACCOUNT_NOT_FOUND,
    POCKET_NOT_FOUND,
    SOURCE_POCKET_NOT_FOUND,
    DEST_POCKET_NOT_FOUND,
    INSUFFICIENT_ACCOUNT,
    INSUFFICIENT_ACCOUNT_TOPUP,
    INSUFFICIENT_ROUNDUP,
    INSUFFICIENT_POCKET,
    INSUFFICIENT_NAMED_POCKET,
    INSUFFICIENT_COMBINED,
    DEPOSIT_LOCKED,
    DEPOSIT_LOCKED_CLOSE,
    DEPOSIT_NOT_FOR_QRIS,
    SOURCE_LOCKED,
    DEST_LOCKED,
    SAME_POCKET,
    INVALID_SOURCE,
    DUPLICATE,
    TX_NOT_FOUND,
    NOT_REFUNDABLE,
    REFUND_EXPIRED
}

/**
 * Extends [IllegalStateException] so existing handlers and tests keep working; [detail] carries
 * the one dynamic value some messages need (a pocket name or an idempotency key).
 */
class BankingException(
    val error: BankingError,
    val detail: String? = null
) : IllegalStateException(if (detail == null) error.name else "${error.name}: $detail")
