package com.example.pocketpay.presentation.home

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pocketpay.R
import com.example.pocketpay.data.PocketType
import com.example.pocketpay.presentation.scanner.QrisScannerDialog
import com.example.pocketpay.presentation.security.BiometricHelper
import com.example.pocketpay.presentation.security.PinManager
import com.example.pocketpay.presentation.security.PinResult
import com.example.pocketpay.ui.components.CircleIcon
import com.example.pocketpay.ui.components.FloatingBottomBar
import com.example.pocketpay.ui.components.NavItem
import com.example.pocketpay.ui.components.hideIf
import com.example.pocketpay.ui.components.PocketBackground
import com.example.pocketpay.ui.theme.PocketTheme
import com.example.pocketpay.work.PocketNotifier
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toRupiah(): String {
    val localeId = Locale.Builder().setLanguage("in").setRegion("ID").build()
    val formatter = NumberFormat.getCurrencyInstance(localeId)
    formatter.maximumFractionDigits = 0
    return formatter.format(this)
}

fun formatAmount(amount: Long, isHidden: Boolean): String {
    return if (isHidden) "Rp ••••••" else amount.toRupiah()
}

fun Long.toDateFormatted(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFullDateTimeFormatted(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(this))
}

data class PendingTransaction(
    val merchant: String,
    val amount: Long,
    val pocketId: String?,
    val isRoundUpEnabled: Boolean
)

private const val TAB_HOME = 0
private const val TAB_STATS = 1
private const val TAB_POCKETS = 2
private const val TAB_PROFILE = 3

/**
 * App shell: splash, PIN setup, the four tabs with the floating bottom bar, and every flow
 * (scan, pay, PIN, success, sheets) layered on top. All screens below are stateless; this
 * composable only wires them to [HomeViewModel].
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val onIntent: (HomeUiIntent) -> Unit = viewModel::processIntent

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var isPinSet by remember { mutableStateOf(PinManager.isPinSet(context)) }
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var isChangePinOpen by remember { mutableStateOf(false) }
    var pendingTx by remember { mutableStateOf<PendingTransaction?>(null) }
    var isPinConfirmOpen by remember { mutableStateOf(false) }
    var payPinError by remember { mutableStateOf<String?>(null) }
    var payPinAttempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeUiEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is HomeUiEffect.ShowSuccessAlert -> {}
                HomeUiEffect.PaymentSuccess -> {}
            }
        }
    }

    // Ask for notification permission once (Android 13+), after the app is unlocked.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(uiState.isAppLocked, showSplash, isPinSet) {
        if (!uiState.isAppLocked && !showSplash && isPinSet && Build.VERSION.SDK_INT >= 33) {
            val prefs = context.getSharedPreferences("pocketpay_notified", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("asked", false) && !PocketNotifier(context).canNotify()) {
                prefs.edit().putBoolean("asked", true).apply()
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val activity = context as? FragmentActivity
    val biometricAvailable = activity != null && BiometricHelper.isBiometricAvailable(activity)

    fun requestBiometricUnlock() {
        if (activity == null) return
        BiometricHelper.showBiometricPrompt(
            activity = activity,
            title = context.getString(R.string.home_buka_kunci_pocketpay),
            subtitle = context.getString(R.string.home_verifikasi_biometrik_untuk_melanjutk),
            onSuccess = { onIntent(HomeUiIntent.UnlockApp) },
            onFallbackToPin = { /* stay on the lock screen and use the keypad */ },
            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        )
    }

    fun submitPayment(tx: PendingTransaction, authMethod: String) {
        onIntent(
            HomeUiIntent.OpenQrisPayment(
                merchant = tx.merchant, amount = tx.amount, pocketId = tx.pocketId,
                isRoundUpEnabled = tx.isRoundUpEnabled, authMethod = authMethod
            )
        )
    }

    fun confirmPayment(tx: PendingTransaction) {
        pendingTx = tx
        if (activity != null && BiometricHelper.isBiometricAvailable(activity)) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = context.getString(R.string.home_verifikasi_pembayaran),
                subtitle = context.getString(R.string.home_konfirmasi_transaksi_ke, tx.amount.toRupiah(), tx.merchant),
                onSuccess = { submitPayment(tx, "Biometrik (Sidik Jari / Face ID)"); pendingTx = null },
                onFallbackToPin = { isPinConfirmOpen = true },
                onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            )
        } else {
            isPinConfirmOpen = true
        }
    }

    val tx0 = pendingTx
    val pinOverlay = isPinConfirmOpen && tx0 != null
    val successOverlay = uiState.successAlert != null
    val lockOverlay = uiState.isAppLocked
    val topOverlay = pinOverlay || successOverlay || lockOverlay || isChangePinOpen
    val anyOverlay = topOverlay || showHistory || uiState.isTransferDialogOpen || uiState.isReceiveQrDialogOpen || uiState.isScannerOpen

    Box(Modifier.fillMaxSize()) {
        when {
            showSplash -> SplashScreen(onSplashFinished = { showSplash = false })

            !isPinSet -> PinSetupScreen(onDone = { isPinSet = true; onIntent(HomeUiIntent.UnlockApp) })

            else -> {
                // ---- main tabs ----
                PocketBackground(Modifier.hideIf(anyOverlay)) {
                    val tabs = listOf(
                        NavItem(stringResource(R.string.nav_home), Icons.Rounded.Home),
                        NavItem(stringResource(R.string.nav_stats), Icons.Rounded.BarChart),
                        NavItem(stringResource(R.string.nav_pockets), Icons.Rounded.Savings),
                        NavItem(stringResource(R.string.nav_profile), Icons.Rounded.Person)
                    )
                    val barPadding = PaddingValues(bottom = 104.dp)
                    Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        when (selectedTab) {
                            TAB_HOME -> HomeTab(
                                uiState, onIntent,
                                onViewAllPockets = { selectedTab = TAB_POCKETS },
                                onViewAllHistory = { showHistory = true },
                                onOpenProfile = { selectedTab = TAB_PROFILE },
                                modifier = Modifier.fillMaxSize(), contentPadding = barPadding
                            )
                            TAB_STATS -> StatsTab(uiState, onIntent, Modifier.fillMaxSize(), barPadding)
                            TAB_POCKETS -> PocketsTab(uiState, onIntent, Modifier.fillMaxSize(), barPadding)
                            else -> ProfileTab(
                                name = uiState.userName, phone = uiState.userPhone, email = uiState.userEmail,
                                tier = uiState.userTier, accountNumber = uiState.account?.accountNumber ?: "-",
                                biometricAvailable = biometricAvailable,
                                onSave = { n, p, e -> onIntent(HomeUiIntent.UpdateUserProfile(n, p, e)) },
                                onChangePin = { isChangePinOpen = true },
                                modifier = Modifier.fillMaxSize(), contentPadding = barPadding
                            )
                        }
                        FloatingBottomBar(
                            tabs, selectedTab, { selectedTab = it },
                            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                        )
                    }
                }

                // ---- full-screen flows ----
                OverlayTransition(showHistory) { HistoryScreen(uiState, onIntent, onBack = { showHistory = false }, modifier = Modifier.hideIf(topOverlay)) }

                if (uiState.isScannerOpen) {
                    QrisScannerDialog(
                        onDismiss = { onIntent(HomeUiIntent.SetScannerVisibility(false)) },
                        onQrScanned = { merchant, amount -> onIntent(HomeUiIntent.OnQrScannedSuccess(merchant, amount)) },
                        onManualInputFallback = {
                            onIntent(HomeUiIntent.SetScannerVisibility(false))
                            onIntent(HomeUiIntent.SetPaymentDialogVisibility(true))
                        }
                    )
                }

                if (uiState.isTransferDialogOpen) {
                    QrisPaymentScreen(
                        initialMerchant = uiState.prefilledMerchant,
                        initialAmount = uiState.prefilledAmount,
                        pockets = uiState.pockets.filter { it.pocketType == PocketType.SPENDING },
                        mainBalance = uiState.account?.balance ?: 0L,
                        onBack = { onIntent(HomeUiIntent.SetPaymentDialogVisibility(false)) },
                        onConfirm = { merchant, amount, pocketId, roundUp ->
                            confirmPayment(PendingTransaction(merchant, amount, pocketId, roundUp))
                        },
                        modifier = Modifier.hideIf(topOverlay)
                    )
                }

                val tx = pendingTx
                if (isPinConfirmOpen && tx != null) {
                    PinEntryScreen(
                        title = stringResource(R.string.pin_pay_title),
                        subtitle = stringResource(R.string.pin_pay_subtitle, tx.amount.toRupiah(), tx.merchant),
                        error = payPinError,
                        resetKey = payPinAttempt,
                        onBack = { isPinConfirmOpen = false; pendingTx = null; payPinError = null },
                        leading = if (biometricAvailable) {
                            {
                                CircleIcon(
                                    Icons.Rounded.Fingerprint, stringResource(R.string.pin_use_biometric),
                                    container = PocketTheme.colors.heroChip, tint = PocketTheme.colors.lime, size = 72.dp,
                                    onClick = { isPinConfirmOpen = false; confirmPayment(tx) }
                                )
                            }
                        } else null,
                        onComplete = { pin ->
                            val result = PinManager.verify(context, pin)
                            if (result == PinResult.Success) {
                                isPinConfirmOpen = false; payPinError = null
                                submitPayment(tx, "PIN 6-Digit")
                                pendingTx = null
                            } else {
                                payPinError = PinManager.describe(context, result); payPinAttempt++
                            }
                        }
                    )
                }

                if (uiState.isReceiveQrDialogOpen) {
                    ReceiveQrScreen(
                        accountNumber = uiState.account?.accountNumber ?: "-",
                        name = uiState.userName,
                        onBack = { onIntent(HomeUiIntent.SetReceiveQrDialogVisibility(false)) },
                        modifier = Modifier.hideIf(topOverlay)
                    )
                }

                if (isChangePinOpen) {
                    ChangePinScreen(
                        onBack = { isChangePinOpen = false },
                        onChanged = {
                            isChangePinOpen = false
                            Toast.makeText(context, context.getString(R.string.profile_pin_berhasil_diganti), Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                uiState.successAlert?.let { SuccessScreen(it, onDone = { onIntent(HomeUiIntent.DismissSuccessAlert) }) }

                // ---- sheets ----
                if (uiState.isCreatePocketDialogOpen) {
                    PocketSheet(onDismiss = { onIntent(HomeUiIntent.SetCreatePocketDialogVisibility(false)) }) {
                        CreatePocketSheet { name, amount, type, tenor, target ->
                            onIntent(HomeUiIntent.CreateNewPocket(name, amount, type, tenor, target))
                        }
                    }
                }
                uiState.selectedPocketForAction?.let { pocket ->
                    PocketSheet(onDismiss = { onIntent(HomeUiIntent.SelectPocketForAction(null)) }) {
                        ManagePocketSheet(
                            pocket = pocket,
                            onExecute = { amount, type -> onIntent(HomeUiIntent.ExecutePocketAction(pocket, amount, type)) },
                            onClosePocket = { onIntent(HomeUiIntent.ClosePocket(pocket)) }
                        )
                    }
                }
                uiState.selectedTransactionForDetail?.let { selected ->
                    PocketSheet(onDismiss = { onIntent(HomeUiIntent.SelectTransactionForDetail(null)) }) {
                        ReceiptSheet(selected, onRefund = { onIntent(HomeUiIntent.RefundTransaction(selected)) })
                    }
                }
                if (uiState.isPocketTransferDialogOpen) {
                    PocketSheet(onDismiss = { onIntent(HomeUiIntent.SetPocketTransferDialogVisibility(false)) }) {
                        TransferSheet(uiState.pockets.filter { it.pocketType == PocketType.SPENDING }) { from, to, amount ->
                            onIntent(HomeUiIntent.ExecutePocketToPocketTransfer(from, to, amount))
                        }
                    }
                }
                if (uiState.isEStatementDialogOpen) {
                    PocketSheet(onDismiss = { onIntent(HomeUiIntent.SetEStatementDialogVisibility(false)) }) {
                        EStatementSheet(
                            accountNumber = uiState.account?.accountNumber ?: "-",
                            currentBalance = uiState.account?.balance ?: 0L,
                            transactions = uiState.recentTransactions
                        )
                    }
                }

                // ---- lock screen sits above everything ----
                if (uiState.isAppLocked) {
                    LockScreen(
                        biometricAvailable = biometricAvailable,
                        onBiometricRequest = ::requestBiometricUnlock,
                        onUnlocked = { onIntent(HomeUiIntent.UnlockApp) }
                    )
                }
            }
        }
    }
}

/** Slides a full-screen flow up from the bottom and fades it out again; keeps the last content during the exit. */
@Composable
fun OverlayTransition(visible: Boolean, content: @Composable () -> Unit) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(280)) { it / 8 } +
            androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)),
        exit = androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(220)) { it / 8 } +
            androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180))
    ) { content() }
}
