package com.example.pocketpay.presentation.home

import androidx.annotation.StringRes
import com.example.pocketpay.R
import com.example.pocketpay.data.AccountEntity
import com.example.pocketpay.data.PocketEntity
import com.example.pocketpay.data.PocketType
import com.example.pocketpay.data.TransactionEntity
import com.example.pocketpay.data.TxType

enum class PocketActionType { TOP_UP, WITHDRAW }
enum class DateRangeFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all_time),
    LAST_7_DAYS(R.string.filter_7_days),
    LAST_30_DAYS(R.string.filter_30_days)
}

data class SuccessAlertData(
    val title: String,
    val message: String
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val account: AccountEntity? = null,
    val pockets: List<PocketEntity> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val isTransferDialogOpen: Boolean = false,
    val isCreatePocketDialogOpen: Boolean = false,
    val isScannerOpen: Boolean = false,
    val isPocketTransferDialogOpen: Boolean = false,
    val isReceiveQrDialogOpen: Boolean = false,
    val isEStatementDialogOpen: Boolean = false,
    val isAccountProfileOpen: Boolean = false,
    val isBalanceHidden: Boolean = false,
    val isRoundUpEnabled: Boolean = true,
    val isAppLocked: Boolean = true, // locked on cold start; unlocked via PIN/biometric
    val successAlert: SuccessAlertData? = null,
    val userName: String = "Nasabah Utama",
    val userPhone: String = "0812-3456-7890",
    val userEmail: String = "nasabah@pocketpay.com",
    val userTier: String = "Gold Premier Member",
    val selectedPocketForAction: PocketEntity? = null,
    val selectedTransactionForDetail: TransactionEntity? = null,
    val selectedTxTypeFilter: TxType? = null,
    val searchQuery: String = "",
    val dateRangeFilter: DateRangeFilter = DateRangeFilter.ALL,
    val prefilledMerchant: String = "Kantin Mahasiswa PocketPay",
    val prefilledAmount: Long = 23_500L,
    val errorMessage: String? = null
) {
    val filteredTransactions: List<TransactionEntity>
        get() {
            val now = System.currentTimeMillis()
            val sevenDaysMillis = 7L * 24 * 60 * 60 * 1000
            val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000

            return recentTransactions.filter { tx ->
                val matchesType = selectedTxTypeFilter == null || tx.type == selectedTxTypeFilter
                val matchesQuery = searchQuery.isBlank() ||
                        tx.title.contains(searchQuery, ignoreCase = true) ||
                        tx.sourceOfFund.contains(searchQuery, ignoreCase = true) ||
                        tx.referenceNo.contains(searchQuery, ignoreCase = true)
                val matchesDate = when (dateRangeFilter) {
                    DateRangeFilter.ALL -> true
                    DateRangeFilter.LAST_7_DAYS -> (now - tx.timestamp) <= sevenDaysMillis
                    DateRangeFilter.LAST_30_DAYS -> (now - tx.timestamp) <= thirtyDaysMillis
                }
                matchesType && matchesQuery && matchesDate
            }
        }
}

sealed interface HomeUiIntent {
    data object RefreshData : HomeUiIntent
    data class OpenQrisPayment(
        val merchant: String,
        val amount: Long,
        val pocketId: String?,
        val isRoundUpEnabled: Boolean = true,
        val authMethod: String = "Biometrik / PIN 6-Digit"
    ) : HomeUiIntent
    data class CreateNewPocket(val name: String, val initialBalance: Long, val type: PocketType, val tenorMonths: Int? = null, val targetAmount: Long? = null) : HomeUiIntent
    data class SetPaymentDialogVisibility(val isOpen: Boolean) : HomeUiIntent
    data class SetCreatePocketDialogVisibility(val isOpen: Boolean) : HomeUiIntent
    data class SetScannerVisibility(val isOpen: Boolean) : HomeUiIntent
    data class SetPocketTransferDialogVisibility(val isOpen: Boolean) : HomeUiIntent
    data class SetReceiveQrDialogVisibility(val isOpen: Boolean) : HomeUiIntent
    data class SetEStatementDialogVisibility(val isOpen: Boolean) : HomeUiIntent
    data class SetAccountProfileVisibility(val isOpen: Boolean) : HomeUiIntent
    data class UpdateUserProfile(val name: String, val phone: String, val email: String) : HomeUiIntent
    data class OnQrScannedSuccess(val merchant: String, val amount: Long) : HomeUiIntent
    data class SelectPocketForAction(val pocket: PocketEntity?) : HomeUiIntent
    data class ExecutePocketAction(val pocket: PocketEntity, val amount: Long, val actionType: PocketActionType) : HomeUiIntent
    data class ExecutePocketToPocketTransfer(val fromPocketId: String, val toPocketId: String, val amount: Long) : HomeUiIntent
    data class SelectTransactionForDetail(val transaction: TransactionEntity?) : HomeUiIntent
    data class SetTxTypeFilter(val type: TxType?) : HomeUiIntent
    data class SetSearchQuery(val query: String) : HomeUiIntent
    data class SetDateRangeFilter(val filter: DateRangeFilter) : HomeUiIntent
    data class RefundTransaction(val transaction: TransactionEntity) : HomeUiIntent
    data object ToggleBalanceVisibility : HomeUiIntent
    data class SetRoundUpEnabled(val isEnabled: Boolean) : HomeUiIntent
    data object LockApp : HomeUiIntent
    data object UnlockApp : HomeUiIntent
    data object DismissSuccessAlert : HomeUiIntent
    data object DismissErrorMessage : HomeUiIntent
    data class ClosePocket(val pocket: PocketEntity) : HomeUiIntent
}

sealed interface HomeUiEffect {
    data class ShowToast(val message: String) : HomeUiEffect
    data class ShowSuccessAlert(val title: String, val message: String) : HomeUiEffect
    data object PaymentSuccess : HomeUiEffect
}