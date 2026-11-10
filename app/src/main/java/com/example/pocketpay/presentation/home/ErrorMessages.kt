package com.example.pocketpay.presentation.home

import android.content.Context
import androidx.annotation.StringRes
import com.example.pocketpay.R
import com.example.pocketpay.data.BankingError
import com.example.pocketpay.data.BankingException

/**
 * Localized, user-safe text for a failure. Known business errors get a specific message; anything
 * else (database or programming errors) shows [fallback] instead of leaking internals.
 */
fun Throwable.userMessage(context: Context, @StringRes fallback: Int): String {
    val e = this as? BankingException ?: return context.getString(fallback)
    return when (e.error) {
        BankingError.AMOUNT_NOT_POSITIVE -> context.getString(R.string.bank_err_amount)
        BankingError.ACCOUNT_NOT_FOUND -> context.getString(R.string.bank_err_account_missing)
        BankingError.POCKET_NOT_FOUND -> context.getString(R.string.bank_err_pocket_missing)
        BankingError.SOURCE_POCKET_NOT_FOUND -> context.getString(R.string.bank_err_source_missing)
        BankingError.DEST_POCKET_NOT_FOUND -> context.getString(R.string.bank_err_dest_missing)
        BankingError.INSUFFICIENT_ACCOUNT -> context.getString(R.string.bank_err_account_funds)
        BankingError.INSUFFICIENT_ACCOUNT_TOPUP -> context.getString(R.string.bank_err_account_funds_topup)
        BankingError.INSUFFICIENT_ROUNDUP -> context.getString(R.string.bank_err_roundup_funds)
        BankingError.INSUFFICIENT_POCKET -> context.getString(R.string.bank_err_pocket_funds)
        BankingError.INSUFFICIENT_NAMED_POCKET -> context.getString(R.string.bank_err_named_pocket_funds, e.detail.orEmpty())
        BankingError.INSUFFICIENT_COMBINED -> context.getString(R.string.bank_err_combined_funds, e.detail.orEmpty())
        BankingError.DEPOSIT_LOCKED -> context.getString(R.string.bank_err_deposit_locked)
        BankingError.DEPOSIT_LOCKED_CLOSE -> context.getString(R.string.bank_err_deposit_locked_close)
        BankingError.DEPOSIT_NOT_FOR_QRIS -> context.getString(R.string.bank_err_deposit_qris)
        BankingError.SOURCE_LOCKED -> context.getString(R.string.bank_err_source_locked)
        BankingError.DEST_LOCKED -> context.getString(R.string.bank_err_dest_locked)
        BankingError.SAME_POCKET -> context.getString(R.string.bank_err_same_pocket)
        BankingError.INVALID_SOURCE -> context.getString(R.string.bank_err_invalid_source)
        BankingError.DUPLICATE -> context.getString(R.string.bank_err_duplicate)
        BankingError.TX_NOT_FOUND -> context.getString(R.string.bank_err_tx_missing)
        BankingError.NOT_REFUNDABLE -> context.getString(R.string.bank_err_not_refundable)
        BankingError.REFUND_EXPIRED -> context.getString(R.string.bank_err_refund_expired)
    }
}
