package com.example.pocketpay.presentation.home

import androidx.compose.ui.res.stringResource
import com.example.pocketpay.R
import com.example.pocketpay.ui.theme.PocketTheme
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pocketpay.data.PocketEntity
import com.example.pocketpay.data.PocketType
import com.example.pocketpay.data.TransactionEntity
import com.example.pocketpay.data.TxType
import com.example.pocketpay.presentation.scanner.QrisScannerDialog
import com.example.pocketpay.presentation.security.BiometricHelper
import com.example.pocketpay.presentation.security.ChangePinDialog
import com.example.pocketpay.presentation.security.PinManager
import com.example.pocketpay.presentation.security.PinSetupDialog
import com.example.pocketpay.presentation.security.PinResult
import com.example.pocketpay.presentation.statement.PdfStatementGenerator
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showSplash by remember { mutableStateOf(true) }
    var pendingTx by remember { mutableStateOf<PendingTransaction?>(null) }
    var isPinDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is HomeUiEffect.ShowSuccessAlert -> {}
                HomeUiEffect.PaymentSuccess -> {}
            }
        }
    }

    // 1. SPLASH SCREEN TRANSITION
    if (showSplash) {
        SplashScreen(
            onSplashFinished = { showSplash = false }
        )
        return
    }

    // 1b. FIRST-LAUNCH PIN SETUP (mandatory before anything else is shown)
    var isPinSet by remember { mutableStateOf(PinManager.isPinSet(context)) }
    if (!isPinSet) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF064E3B)))
        PinSetupDialog(onDone = {
            isPinSet = true
            viewModel.processIntent(HomeUiIntent.UnlockApp) // user just created the PIN
        })
        return
    }

    // 1c. ASK FOR NOTIFICATION PERMISSION ONCE (Android 13+), after the app is unlocked
    val notificationPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(uiState.isAppLocked) {
        if (!uiState.isAppLocked && android.os.Build.VERSION.SDK_INT >= 33) {
            val prefs = context.getSharedPreferences("pocketpay_notified", android.content.Context.MODE_PRIVATE)
            if (!prefs.getBoolean("asked", false) && !com.example.pocketpay.work.PocketNotifier(context).canNotify()) {
                prefs.edit().putBoolean("asked", true).apply()
                notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // 2. FULL SCREEN KELOLA AKUN (PAGE BARU AKUN NASABAH)
    if (uiState.isAccountProfileOpen) {
        AccountProfileScreen(
            currentName = uiState.userName,
            currentPhone = uiState.userPhone,
            currentEmail = uiState.userEmail,
            userTier = uiState.userTier,
            accountNumber = uiState.account?.accountNumber ?: "9012839102",
            onDismiss = {
                viewModel.processIntent(HomeUiIntent.SetAccountProfileVisibility(false))
            },
            onSave = { newName, newPhone, newEmail ->
                viewModel.processIntent(HomeUiIntent.UpdateUserProfile(newName, newPhone, newEmail))
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.clickable {
                                viewModel.processIntent(HomeUiIntent.SetAccountProfileVisibility(true))
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFF064E3B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.userName.take(1),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = uiState.userName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.home_ketuk_untuk_kelola_akun),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = PocketTheme.colors.accent
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.processIntent(HomeUiIntent.ToggleBalanceVisibility)
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = stringResource(R.string.home_samarkan_saldo),
                                tint = PocketTheme.colors.brandText
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.processIntent(HomeUiIntent.SetEStatementDialogVisibility(true))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = stringResource(R.string.home_export_e_statement_pdf),
                                tint = PocketTheme.colors.brandText
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.processIntent(HomeUiIntent.SetReceiveQrDialogVisibility(true))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = stringResource(R.string.home_qr_saya_terima_transfer),
                                tint = PocketTheme.colors.brandText
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        viewModel.processIntent(HomeUiIntent.SetScannerVisibility(true))
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = stringResource(R.string.home_scan_qris),
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. REKENING UTAMA CARD (EMERALD GREEN CONTAINER)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF064E3B),
                                            Color(0xFF047857),
                                            Color(0xFF059669)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.home_rekening_utama_super_deposito),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = formatAmount(uiState.account?.balance ?: 0L, uiState.isBalanceHidden),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 30.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.home_no_rek, uiState.account?.accountNumber ?: "-"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.75f)
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.processIntent(HomeUiIntent.SetScannerVisibility(true))
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color(0xFF064E3B)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(stringResource(R.string.home_bayar_qris), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            viewModel.processIntent(HomeUiIntent.SetPocketTransferDialogVisibility(true))
                                        },
                                        modifier = Modifier.weight(1.1f),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFFD1FAE5),
                                            contentColor = Color(0xFF064E3B)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(stringResource(R.string.home_antar_kantong), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.processIntent(HomeUiIntent.SetCreatePocketDialogVisibility(true))
                                        },
                                        modifier = Modifier.weight(0.9f),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(stringResource(R.string.home_kantong), fontSize = 11.sp, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. FEATURE BANNER (CELENGAN RECEH ROUND-UP)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color(0xFF059669), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.home_celengan_receh_round_up),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PocketTheme.colors.brandText
                                )
                                Text(
                                    text = stringResource(R.string.home_pembulatan_transaksi_qris_otomatis_d),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = PocketTheme.colors.accentText
                                )
                            }
                        }
                    }
                }

                // 3. KANTONG FINANSIAL
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.home_kantong_finansial),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(
                                onClick = {
                                    viewModel.processIntent(HomeUiIntent.SetCreatePocketDialogVisibility(true))
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.home_buat_kantong), fontWeight = FontWeight.Bold, color = PocketTheme.colors.accentText)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.pockets, key = { it.id }) { pocket ->
                                PocketCardItem(
                                    pocket = pocket,
                                    isHidden = uiState.isBalanceHidden,
                                    onClick = {
                                        viewModel.processIntent(HomeUiIntent.SelectPocketForAction(pocket))
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. MUTASI TRANSAKSI + FILTER & SEARCH
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.home_mutasi_transaksi),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.processIntent(HomeUiIntent.SetSearchQuery(it)) },
                            placeholder = { Text(stringResource(R.string.home_cari_merchant_ref_atau_sumber_dana)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.processIntent(HomeUiIntent.SetSearchQuery("")) }) {
                                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.home_clear))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedTxTypeFilter == null,
                                    onClick = { viewModel.processIntent(HomeUiIntent.SetTxTypeFilter(null)) },
                                    label = { Text(stringResource(R.string.home_semua_tipe)) }
                                )
                            }
                            items(TxType.entries.toTypedArray()) { type ->
                                FilterChip(
                                    selected = uiState.selectedTxTypeFilter == type,
                                    onClick = { viewModel.processIntent(HomeUiIntent.SetTxTypeFilter(type)) },
                                    label = { Text(type.name) }
                                )
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(DateRangeFilter.entries.toTypedArray()) { filter ->
                                FilterChip(
                                    selected = uiState.dateRangeFilter == filter,
                                    onClick = { viewModel.processIntent(HomeUiIntent.SetDateRangeFilter(filter)) },
                                    label = { Text(stringResource(filter.labelRes)) }
                                )
                            }
                        }
                    }
                }

                val displayedTxs = uiState.filteredTransactions
                if (displayedTxs.isEmpty()) {
                    item {
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty() || uiState.selectedTxTypeFilter != null || uiState.dateRangeFilter != DateRangeFilter.ALL)
                                stringResource(R.string.home_tidak_ada_transaksi_yang_cocok_denga)
                            else stringResource(R.string.home_belum_ada_riwayat_transaksi),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(displayedTxs, key = { it.id }) { tx ->
                        TransactionItemRow(
                            tx = tx,
                            isHidden = uiState.isBalanceHidden,
                            onClick = {
                                viewModel.processIntent(HomeUiIntent.SelectTransactionForDetail(tx))
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }

        // AUTO-LOCK SECURITY SHIELD OVERLAY
        if (uiState.isAppLocked) {
            val activity = context as? FragmentActivity
            val biometricAvailable = activity != null && BiometricHelper.isBiometricAvailable(activity)
            AppLockOverlay(
                biometricAvailable = biometricAvailable,
                onBiometricRequest = {
                    if (activity != null) {
                        BiometricHelper.showBiometricPrompt(
                            activity = activity,
                            title = context.getString(R.string.home_buka_kunci_pocketpay),
                            subtitle = context.getString(R.string.home_verifikasi_biometrik_untuk_melanjutk),
                            onSuccess = { viewModel.processIntent(HomeUiIntent.UnlockApp) },
                            onFallbackToPin = { /* user stays on lock screen and enters PIN */ },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                onUnlocked = { viewModel.processIntent(HomeUiIntent.UnlockApp) }
            )
        }
    }

    // DIALOG 1: SCANNER KAMERA & GALERI
    if (uiState.isScannerOpen) {
        QrisScannerDialog(
            onDismiss = {
                viewModel.processIntent(HomeUiIntent.SetScannerVisibility(false))
            },
            onQrScanned = { merchant, amount ->
                viewModel.processIntent(HomeUiIntent.OnQrScannedSuccess(merchant, amount))
            },
            onManualInputFallback = {
                viewModel.processIntent(HomeUiIntent.SetScannerVisibility(false))
                viewModel.processIntent(HomeUiIntent.SetPaymentDialogVisibility(true))
            }
        )
    }

    // DIALOG 2: PEMBAYARAN QRIS
    if (uiState.isTransferDialogOpen) {
        val activity = context as? FragmentActivity

        PaymentQrisDialog(
            initialMerchant = uiState.prefilledMerchant,
            initialAmount = uiState.prefilledAmount,
            pockets = uiState.pockets.filter { it.pocketType == PocketType.SPENDING },
            onDismiss = {
                viewModel.processIntent(HomeUiIntent.SetPaymentDialogVisibility(false))
            },
            onConfirmPayment = { merchant, amount, pocketId, isRoundUp ->
                pendingTx = PendingTransaction(merchant, amount, pocketId, isRoundUp)

                if (activity != null && BiometricHelper.isBiometricAvailable(activity)) {
                    BiometricHelper.showBiometricPrompt(
                        activity = activity,
                        title = context.getString(R.string.home_verifikasi_pembayaran),
                        subtitle = context.getString(R.string.home_konfirmasi_transaksi_ke, amount.toRupiah(), merchant),
                        onSuccess = {
                            viewModel.processIntent(
                                HomeUiIntent.OpenQrisPayment(
                                    merchant = merchant,
                                    amount = amount,
                                    pocketId = pocketId,
                                    isRoundUpEnabled = isRoundUp,
                                    authMethod = "Biometrik (Sidik Jari / Face ID)"
                                )
                            )
                        },
                        onFallbackToPin = {
                            isPinDialogOpen = true
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    isPinDialogOpen = true
                }
            }
        )
    }

    // DIALOG 3: FALLBACK PIN TRANSAKSI
    if (isPinDialogOpen && pendingTx != null) {
        val tx = pendingTx!!
        TransactionPinDialog(
            merchant = tx.merchant,
            amount = tx.amount,
            onDismiss = {
                isPinDialogOpen = false
                pendingTx = null
            },
            onPinSuccess = {
                isPinDialogOpen = false
                viewModel.processIntent(
                    HomeUiIntent.OpenQrisPayment(
                        merchant = tx.merchant,
                        amount = tx.amount,
                        pocketId = tx.pocketId,
                        isRoundUpEnabled = tx.isRoundUpEnabled,
                        authMethod = "PIN 6-Digit"
                    )
                )
                pendingTx = null
            }
        )
    }

    // DIALOG 4: BUAT KANTONG BARU
    if (uiState.isCreatePocketDialogOpen) {
        CreatePocketDialog(
            onDismiss = {
                viewModel.processIntent(HomeUiIntent.SetCreatePocketDialogVisibility(false))
            },
            onConfirm = { name, initialAmount, type, tenorMonths, targetAmount ->
                viewModel.processIntent(HomeUiIntent.CreateNewPocket(name, initialAmount, type, tenorMonths, targetAmount))
            }
        )
    }

    // DIALOG 5: KELOLA SALDO KANTONG
    uiState.selectedPocketForAction?.let { pocket ->
        ManagePocketBalanceDialog(
            pocket = pocket,
            onDismiss = {
                viewModel.processIntent(HomeUiIntent.SelectPocketForAction(null))
            },
            onExecute = { amount, actionType ->
                viewModel.processIntent(
                    HomeUiIntent.ExecutePocketAction(pocket, amount, actionType)
                )
            },
            onClosePocket = {
                viewModel.processIntent(HomeUiIntent.ClosePocket(pocket))
            }
        )
    }

    // DIALOG 6: DETAIL & STRUK MUTASI DIGITAL
    uiState.selectedTransactionForDetail?.let { tx ->
        TransactionDetailReceiptDialog(
            transaction = tx,
            onRefund = { viewModel.processIntent(HomeUiIntent.RefundTransaction(tx)) },
            onDismiss = {
                viewModel.processIntent(HomeUiIntent.SelectTransactionForDetail(null))
            }
        )
    }

    // DIALOG 7: TRANSFER ANTAR-KANTONG
    if (uiState.isPocketTransferDialogOpen) {
        PocketToPocketTransferDialog(
            pockets = uiState.pockets.filter { it.pocketType == PocketType.SPENDING },
            onDismiss = {
                viewModel.processIntent(HomeUiIntent.SetPocketTransferDialogVisibility(false))
            },
            onConfirmTransfer = { fromId, toId, amount ->
                viewModel.processIntent(
                    HomeUiIntent.ExecutePocketToPocketTransfer(fromId, toId, amount)
                )
            }
        )
    }

    // DIALOG 8: GENERATE QR TERIMA TRANSFER
    if (uiState.isReceiveQrDialogOpen) {
        ReceiveQrDialog(
            accountNumber = uiState.account?.accountNumber ?: "9012839102",
            onDismiss = {
                viewModel.processIntent(HomeUiIntent.SetReceiveQrDialogVisibility(false))
            }
        )
    }

    // DIALOG 9: EXPORT E-STATEMENT PDF
    if (uiState.isEStatementDialogOpen) {
        EStatementPdfDialog(
            accountNumber = uiState.account?.accountNumber ?: "9012839102",
            currentBalance = uiState.account?.balance ?: 0L,
            transactions = uiState.recentTransactions,
            onDismiss = {
                viewModel.processIntent(HomeUiIntent.SetEStatementDialogVisibility(false))
            }
        )
    }

    // DIALOG 11: ANIMATED SWEET ALERT (M-BANKING MODERN POPUP)
    uiState.successAlert?.let { alert ->
        AnimatedSuccessPaymentDialog(
            title = alert.title,
            message = alert.message,
            onDismiss = {
                viewModel.processIntent(HomeUiIntent.DismissSuccessAlert)
            }
        )
    }
}

// -----------------------------------------------------------------------------
// HALAMAN BARU: KELOLA AKUN NASABAH (FULL-SCREEN SCREEN)
// -----------------------------------------------------------------------------
