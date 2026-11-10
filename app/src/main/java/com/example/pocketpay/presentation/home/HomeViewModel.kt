package com.example.pocketpay.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketpay.data.PocketEntity
import com.example.pocketpay.data.ProfileStore
import com.example.pocketpay.data.UserProfile
import com.example.pocketpay.data.PocketType
import com.example.pocketpay.domain.repository.BankingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BankingRepository,
    private val profileStore: ProfileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<HomeUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        val defaults = _uiState.value
        val saved = profileStore.load(UserProfile(defaults.userName, defaults.userPhone, defaults.userEmail))
        _uiState.update { it.copy(userName = saved.name, userPhone = saved.phone, userEmail = saved.email) }
        viewModelScope.launch { repository.accrueInterest() }
        observeBankingData()
    }

    private fun observeBankingData() {
        viewModelScope.launch {
            combine(
                repository.getAccount(),
                repository.getPockets(),
                repository.getTransactions()
            ) { account, pockets, transactions ->
                Triple(account, pockets, transactions)
            }.collect { (account, pockets, transactions) ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        account = account,
                        pockets = pockets,
                        recentTransactions = transactions
                    )
                }
            }
        }
    }

    fun processIntent(intent: HomeUiIntent) {
        when (intent) {
            is HomeUiIntent.RefreshData -> observeBankingData()

            is HomeUiIntent.SetScannerVisibility -> {
                _uiState.update { it.copy(isScannerOpen = intent.isOpen) }
            }

            is HomeUiIntent.OnQrScannedSuccess -> {
                _uiState.update {
                    it.copy(
                        isScannerOpen = false,
                        isTransferDialogOpen = true,
                        prefilledMerchant = intent.merchant,
                        prefilledAmount = intent.amount
                    )
                }
            }

            is HomeUiIntent.OpenQrisPayment -> {
                executeQrisPayment(
                    intent.merchant,
                    intent.amount,
                    intent.pocketId,
                    intent.isRoundUpEnabled,
                    intent.authMethod
                )
            }

            is HomeUiIntent.CreateNewPocket -> {
                allocateNewPocket(intent.name, intent.initialBalance, intent.type, intent.tenorMonths, intent.targetAmount)
            }

            is HomeUiIntent.SetPaymentDialogVisibility -> {
                _uiState.update { it.copy(isTransferDialogOpen = intent.isOpen) }
            }

            is HomeUiIntent.SetCreatePocketDialogVisibility -> {
                _uiState.update { it.copy(isCreatePocketDialogOpen = intent.isOpen) }
            }

            is HomeUiIntent.SetPocketTransferDialogVisibility -> {
                _uiState.update { it.copy(isPocketTransferDialogOpen = intent.isOpen) }
            }

            is HomeUiIntent.SetReceiveQrDialogVisibility -> {
                _uiState.update { it.copy(isReceiveQrDialogOpen = intent.isOpen) }
            }

            is HomeUiIntent.SetEStatementDialogVisibility -> {
                _uiState.update { it.copy(isEStatementDialogOpen = intent.isOpen) }
            }

            is HomeUiIntent.SetAccountProfileVisibility -> {
                _uiState.update { it.copy(isAccountProfileOpen = intent.isOpen) }
            }

            is HomeUiIntent.UpdateUserProfile -> {
                profileStore.save(UserProfile(intent.name, intent.phone, intent.email))
                _uiState.update {
                    it.copy(
                        userName = intent.name,
                        userPhone = intent.phone,
                        userEmail = intent.email,
                        isAccountProfileOpen = false
                    )
                }
                viewModelScope.launch {
                    _effect.send(HomeUiEffect.ShowToast("Profil nasabah berhasil diperbarui!"))
                }
            }

            is HomeUiIntent.ExecutePocketToPocketTransfer -> {
                executePocketToPocketTransfer(intent.fromPocketId, intent.toPocketId, intent.amount)
            }

            is HomeUiIntent.SelectTransactionForDetail -> {
                _uiState.update { it.copy(selectedTransactionForDetail = intent.transaction) }
            }

            is HomeUiIntent.SetTxTypeFilter -> {
                _uiState.update { it.copy(selectedTxTypeFilter = intent.type) }
            }

            is HomeUiIntent.SetSearchQuery -> {
                _uiState.update { it.copy(searchQuery = intent.query) }
            }

            is HomeUiIntent.SetDateRangeFilter -> {
                _uiState.update { it.copy(dateRangeFilter = intent.filter) }
            }

            is HomeUiIntent.ToggleBalanceVisibility -> {
                _uiState.update { it.copy(isBalanceHidden = !it.isBalanceHidden) }
            }

            is HomeUiIntent.SetRoundUpEnabled -> {
                _uiState.update { it.copy(isRoundUpEnabled = intent.isEnabled) }
            }

            is HomeUiIntent.LockApp -> {
                _uiState.update { it.copy(isAppLocked = true) }
            }

            is HomeUiIntent.UnlockApp -> {
                _uiState.update { it.copy(isAppLocked = false) }
            }

            is HomeUiIntent.DismissSuccessAlert -> {
                _uiState.update { it.copy(successAlert = null) }
            }

            is HomeUiIntent.SelectPocketForAction -> {
                _uiState.update { it.copy(selectedPocketForAction = intent.pocket) }
            }

            is HomeUiIntent.ExecutePocketAction -> {
                handlePocketAction(intent.pocket, intent.amount, intent.actionType)
            }

            is HomeUiIntent.ClosePocket -> {
                closePocket(intent.pocket)
            }

            is HomeUiIntent.DismissErrorMessage -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun executeQrisPayment(
        merchant: String,
        amount: Long,
        pocketId: String?,
        isRoundUpEnabled: Boolean,
        authMethod: String
    ) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val roundUpAmount = if (isRoundUpEnabled) {
                val remainder = amount % 5000L
                if (remainder == 0L) 0L else (5000L - remainder)
            } else {
                0L
            }

            val targetPocket = if (pocketId != null) _uiState.value.pockets.find { it.id == pocketId } else null
            val idempotencyKey = java.util.UUID.randomUUID().toString()

            val result = repository.payQrisCascading(
                pocket = targetPocket,
                amount = amount,
                merchant = merchant,
                roundUpAmount = roundUpAmount,
                authMethod = authMethod,
                idempotencyKey = idempotencyKey
            )

            result.onSuccess {
                val roundUpMsg = if (roundUpAmount > 0) {
                    "\n(Rp ${roundUpAmount.toRupiah()} otomatis dialokasikan ke Celengan Receh)"
                } else ""

                val alertData = SuccessAlertData(
                    title = "Pembayaran QRIS Berhasil!",
                    message = "Pembayaran ${amount.toRupiah()} ke $merchant telah sukses diproses.$roundUpMsg\n[Idempotency Key: ${idempotencyKey.take(8)}...]"
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isTransferDialogOpen = false,
                        successAlert = alertData
                    )
                }
                _effect.send(HomeUiEffect.PaymentSuccess)
                _effect.send(HomeUiEffect.ShowSuccessAlert(alertData.title, alertData.message))
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "Terjadi kesalahan pembayaran."
                    )
                }
                _effect.send(HomeUiEffect.ShowToast(throwable.localizedMessage ?: "Gagal memproses transaksi"))
            }
        }
    }

    private fun allocateNewPocket(name: String, balance: Long, type: PocketType, tenorMonths: Int?, targetAmount: Long?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.createAndAllocatePocket(name, balance, type, tenorMonths, targetAmount)
                .onSuccess {
                    val alertData = SuccessAlertData(
                        title = "Kantong Berhasil Dibuat!",
                        message = "Kantong '$name' berhasil dibuat dengan alokasi awal Rp ${balance.toRupiah()}."
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isCreatePocketDialogOpen = false,
                            successAlert = alertData
                        )
                    }
                    _effect.send(HomeUiEffect.ShowSuccessAlert(alertData.title, alertData.message))
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.localizedMessage ?: "Gagal membuat kantong."
                        )
                    }
                    _effect.send(HomeUiEffect.ShowToast(throwable.localizedMessage ?: "Gagal membuat kantong"))
                }
        }
    }

    private fun handlePocketAction(pocket: PocketEntity, amount: Long, actionType: PocketActionType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = when (actionType) {
                PocketActionType.TOP_UP -> repository.topUpPocket(pocket, amount)
                PocketActionType.WITHDRAW -> repository.withdrawFromPocket(pocket, amount)
            }

            result.onSuccess {
                val label = if (actionType == PocketActionType.TOP_UP) "Top-Up" else "Penarikan Saldo"
                val alertData = SuccessAlertData(
                    title = "$label Berhasil!",
                    message = "$label sebesar Rp ${amount.toRupiah()} pada kantong '${pocket.name}' telah sukses."
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedPocketForAction = null,
                        successAlert = alertData
                    )
                }
                _effect.send(HomeUiEffect.ShowSuccessAlert(alertData.title, alertData.message))
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false) }
                _effect.send(HomeUiEffect.ShowToast(throwable.localizedMessage ?: "Gagal memproses saldo"))
            }
        }
    }

    private fun executePocketToPocketTransfer(fromPocketId: String, toPocketId: String, amount: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val fromPocket = _uiState.value.pockets.find { it.id == fromPocketId }
            val toPocket = _uiState.value.pockets.find { it.id == toPocketId }

            if (fromPocket == null || toPocket == null) {
                _uiState.update { it.copy(isLoading = false) }
                _effect.send(HomeUiEffect.ShowToast("Kantong asal atau tujuan tidak ditemukan."))
                return@launch
            }

            repository.transferBetweenPockets(fromPocket, toPocket, amount)
                .onSuccess {
                    val alertData = SuccessAlertData(
                        title = "Transfer Antar-Kantong Sukses!",
                        message = "Berhasil memindahkan Rp ${amount.toRupiah()} dari kantong '${fromPocket.name}' ke '${toPocket.name}'."
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPocketTransferDialogOpen = false,
                            successAlert = alertData
                        )
                    }
                    _effect.send(HomeUiEffect.ShowSuccessAlert(alertData.title, alertData.message))
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.send(HomeUiEffect.ShowToast(throwable.localizedMessage ?: "Gagal mentransfer saldo."))
                }
        }
    }

    private fun closePocket(pocket: PocketEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.closePocket(pocket)
                .onSuccess {
                    val alertData = SuccessAlertData(
                        title = "Kantong Ditutup",
                        message = "Kantong '${pocket.name}' berhasil ditutup dan seluruh sisa saldonya telah dikembalikan ke Rekening Utama."
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedPocketForAction = null,
                            successAlert = alertData
                        )
                    }
                    _effect.send(HomeUiEffect.ShowSuccessAlert(alertData.title, alertData.message))
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.send(HomeUiEffect.ShowToast(throwable.localizedMessage ?: "Gagal menutup kantong."))
                }
        }
    }
}