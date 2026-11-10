package com.example.pocketpay.presentation.home

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
        PinSetupDialog(onDone = { isPinSet = true })
        return
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
            .background(Color(0xFFF8FAFC))
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
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Ketuk untuk Kelola Akun",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color(0xFF059669)
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
                                contentDescription = "Samarkan Saldo",
                                tint = Color(0xFF064E3B)
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.processIntent(HomeUiIntent.SetEStatementDialogVisibility(true))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Export E-Statement PDF",
                                tint = Color(0xFF064E3B)
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.processIntent(HomeUiIntent.SetReceiveQrDialogVisibility(true))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QR Saya / Terima Transfer",
                                tint = Color(0xFF064E3B)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        viewModel.processIntent(HomeUiIntent.SetScannerVisibility(true))
                    },
                    containerColor = Color(0xFF064E3B),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QRIS",
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            containerColor = Color(0xFFF8FAFC)
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
                                    text = "Rekening Utama (Super Deposito)",
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
                                    text = "No. Rek: ${uiState.account?.accountNumber ?: "-"}",
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
                                        Text("Bayar QRIS", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
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
                                        Text("Antar Kantong", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
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
                                        Text("Kantong", fontSize = 11.sp, maxLines = 1, softWrap = false)
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
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
                                    text = "Celengan Receh (Round-Up)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF064E3B)
                                )
                                Text(
                                    text = "Pembulatan transaksi QRIS otomatis ditabung ke Celengan Receh 3.5% p.a.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color(0xFF047857)
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
                                text = "Kantong Finansial",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            TextButton(
                                onClick = {
                                    viewModel.processIntent(HomeUiIntent.SetCreatePocketDialogVisibility(true))
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Buat Kantong", fontWeight = FontWeight.Bold, color = Color(0xFF047857))
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
                            text = "Mutasi Transaksi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.processIntent(HomeUiIntent.SetSearchQuery(it)) },
                            placeholder = { Text("Cari merchant, ref, atau sumber dana...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.processIntent(HomeUiIntent.SetSearchQuery("")) }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedTxTypeFilter == null,
                                    onClick = { viewModel.processIntent(HomeUiIntent.SetTxTypeFilter(null)) },
                                    label = { Text("Semua Tipe") }
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
                                    label = { Text(filter.label) }
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
                                "Tidak ada transaksi yang cocok dengan filter."
                            else "Belum ada riwayat transaksi.",
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
                            title = "Buka Kunci PocketPay",
                            subtitle = "Verifikasi biometrik untuk melanjutkan",
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
                        title = "Verifikasi Pembayaran",
                        subtitle = "Konfirmasi transaksi ${amount.toRupiah()} ke $merchant",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountProfileScreen(
    currentName: String,
    currentPhone: String,
    currentEmail: String,
    userTier: String,
    accountNumber: String,
    onDismiss: () -> Unit,
    onSave: (newName: String, newPhone: String, newEmail: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf(currentPhone) }
    var email by remember { mutableStateOf(currentEmail) }
    var isChangePinOpen by remember { mutableStateOf(false) }
    val profileContext = LocalContext.current

    if (isChangePinOpen) {
        ChangePinDialog(
            onDismiss = { isChangePinOpen = false },
            onChanged = {
                isChangePinOpen = false
                Toast.makeText(profileContext, "PIN berhasil diganti.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Akun Nasabah", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Nasabah Avatar Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.take(1),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF064E3B)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = userTier,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Informasi Pribadi
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Informasi Diri & Kontak",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF064E3B)
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nama Lengkap Nasabah") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Nomor Telepon") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Perbankan") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onSave(name, phone, email)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Proteksi Keamanan (Shield Status)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Status Keamanan & Proteksi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF064E3B)
                        )

                        SecurityStatusRow(label = "PIN Transaksi", value = "6-Digit PIN Transaksi (Aktif)")
                        SecurityStatusRow(label = "Biometrik", value = "Fingerprint / Face ID (Aktif)")
                        SecurityStatusRow(label = "Anti-Screenshot", value = "FLAG_SECURE Active (Aktif)")
                        SecurityStatusRow(label = "Session Timeout", value = "Inactivity Auto-Lock 60 Detik")

                        OutlinedButton(
                            onClick = { isChangePinOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ganti PIN")
                        }
                    }
                }
            }

            // Informasi Rekening
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Informasi Rekening Perbankan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                        Text("• No. Rekening Utama: $accountNumber", style = MaterialTheme.typography.bodySmall, color = Color(0xFF047857))
                        Text("• Tipe Rekening: Super Deposito PocketPay", style = MaterialTheme.typography.bodySmall, color = Color(0xFF047857))
                        Text("• Status Rekening: Aktif & Terverifikasi", style = MaterialTheme.typography.bodySmall, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityStatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
        }
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
    }
}

// -----------------------------------------------------------------------------
// ANIMATED SPLASH SCREEN COMPONENT
// -----------------------------------------------------------------------------

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000L)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF064E3B)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.pocketpay.R.drawable.ic_launcher_foreground),
                    contentDescription = "PocketPay Logo",
                    modifier = Modifier.size(76.dp)
                )
            }

            Text(
                text = "PocketPay",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = "Next-Gen Digital Banking & Micro-Savings",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// ANIMATED SWEET ALERT (M-BANKING MODERN POPUP)
// -----------------------------------------------------------------------------

@Composable
fun AnimatedSuccessPaymentDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)) + fadeIn()
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFFD1FAE5), CircleShape)
                        .border(3.dp, Color(0xFF10B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF064E3B)
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color(0xFF334155),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Mantap! OK", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// -----------------------------------------------------------------------------
// OTHER HELPER SUB-COMPONENTS
// -----------------------------------------------------------------------------

@Composable
fun EStatementPdfDialog(
    accountNumber: String,
    currentBalance: Long,
    transactions: List<TransactionEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf(DateRangeFilter.ALL) }

    val filteredTxs = remember(selectedPeriod, transactions) {
        val now = System.currentTimeMillis()
        val sevenDaysMillis = 7L * 24 * 60 * 60 * 1000
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000
        transactions.filter { tx ->
            when (selectedPeriod) {
                DateRangeFilter.ALL -> true
                DateRangeFilter.LAST_7_DAYS -> (now - tx.timestamp) <= sevenDaysMillis
                DateRangeFilter.LAST_30_DAYS -> (now - tx.timestamp) <= thirtyDaysMillis
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFF064E3B)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export E-Statement PDF",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cetak Rekening Koran resmi berformat PDF lengkap dengan Kop Surat PocketPay, Watermark, & Rincian Mutasi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Pilih Periode Laporan:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateRangeFilter.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.label) }
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFECFDF5)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "Ringkasan Dokumen:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                        Text(text = "• No. Rekening: $accountNumber", style = MaterialTheme.typography.bodySmall, color = Color(0xFF047857))
                        Text(text = "• Periode: ${selectedPeriod.label}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF047857))
                        Text(text = "• Total Transaksi: ${filteredTxs.size} Mutasi", style = MaterialTheme.typography.bodySmall, color = Color(0xFF047857))
                        Text(text = "• Saldo Akhir: ${currentBalance.toRupiah()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val pdfFile = PdfStatementGenerator.generateEStatementPdf(
                            context = context,
                            accountNumber = accountNumber,
                            periodLabel = selectedPeriod.label,
                            transactions = filteredTxs,
                            currentBalance = currentBalance
                        )
                        if (pdfFile != null) {
                            PdfStatementGenerator.openEStatementPdf(context, pdfFile)
                        } else {
                            Toast.makeText(context, "Gagal membuat PDF", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Buka PDF")
                }

                Button(
                    onClick = {
                        val pdfFile = PdfStatementGenerator.generateEStatementPdf(
                            context = context,
                            accountNumber = accountNumber,
                            periodLabel = selectedPeriod.label,
                            transactions = filteredTxs,
                            currentBalance = currentBalance
                        )
                        if (pdfFile != null) {
                            PdfStatementGenerator.shareEStatementPdf(context, pdfFile)
                        } else {
                            Toast.makeText(context, "Gagal membuat PDF", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bagikan PDF")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun AppLockOverlay(
    biometricAvailable: Boolean,
    onBiometricRequest: () -> Unit,
    onUnlocked: () -> Unit
) {
    val lockContext = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (biometricAvailable) onBiometricRequest()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF064E3B))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = "PocketPay Shield",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Sesi aplikasi terkunci otomatis untuk keamanan finansial Anda. Buka kunci untuk kembali ke dashboard.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                        pin = it
                        errorText = null
                    }
                },
                label = { Text("PIN 6 Digit") },
                singleLine = true,
                isError = errorText != null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            errorText?.let {
                Text(text = it, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val result = PinManager.verify(lockContext, pin)
                    if (result == PinResult.Success) {
                        onUnlocked()
                    } else {
                        errorText = PinManager.describe(result)
                        pin = ""
                    }
                },
                enabled = pin.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF064E3B)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buka Kunci dengan PIN", fontWeight = FontWeight.Bold)
            }

            if (biometricAvailable) {
                TextButton(onClick = onBiometricRequest) {
                    Text("Gunakan Biometrik", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun TransactionDetailReceiptDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val receiptSummary = """
        === STRUK MUTASI DIGITAL POCKETPAY ===
        Status        : ${transaction.status.name}
        No. Referensi : ${transaction.referenceNo}
        Judul         : ${transaction.title}
        Nominal       : ${transaction.amount.toRupiah()}
        Tipe          : ${transaction.type.name}
        Sumber Dana   : ${transaction.sourceOfFund}
        Waktu         : ${transaction.timestamp.toFullDateTimeFormatted()}
        Otentikasi    : ${transaction.authMethod}
        Idempotency   : ${transaction.idempotencyKey}
        =====================================
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = Color(0xFF064E3B)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Struk Transaksi Digital",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = Color(0xFFD1FAE5),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = transaction.status.name,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8FAFC)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = transaction.amount.toRupiah(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF064E3B),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    HorizontalDivider()

                    ReceiptRowItem(label = "Nama / Merchant", value = transaction.title)
                    ReceiptRowItem(label = "No. Referensi", value = transaction.referenceNo)
                    ReceiptRowItem(label = "Tipe Mutasi", value = transaction.type.name)
                    ReceiptRowItem(label = "Sumber Dana", value = transaction.sourceOfFund)
                    ReceiptRowItem(label = "Waktu", value = transaction.timestamp.toFullDateTimeFormatted())
                    ReceiptRowItem(label = "Metode Otentikasi", value = transaction.authMethod)
                    ReceiptRowItem(label = "Idempotency Key", value = transaction.idempotencyKey.take(12) + "...")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, receiptSummary)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Bagikan Struk Digital"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B))
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bagikan Struk")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun ReceiptRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 180.dp),
            color = Color(0xFF0F172A)
        )
    }
}

@Composable
fun PocketToPocketTransferDialog(
    pockets: List<PocketEntity>,
    onDismiss: () -> Unit,
    onConfirmTransfer: (fromPocketId: String, toPocketId: String, amount: Long) -> Unit
) {
    var fromPocketId by remember { mutableStateOf(pockets.firstOrNull()?.id ?: "") }
    var toPocketId by remember { mutableStateOf(pockets.getOrNull(1)?.id ?: "") }
    var amountText by remember { mutableStateOf("25000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Transfer Antar-Kantong",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (pockets.size < 2) {
                Text(
                    text = "Anda membutuhkan minimal 2 Kantong Belanja (SPENDING) untuk mentransfer antar kantong.",
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pilih Kantong Asal:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    pockets.forEach { pocket ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = fromPocketId == pocket.id,
                                onClick = {
                                    fromPocketId = pocket.id
                                    if (toPocketId == pocket.id) {
                                        toPocketId = pockets.firstOrNull { it.id != pocket.id }?.id ?: ""
                                    }
                                }
                            )
                            Text("${pocket.name} (${pocket.balance.toRupiah()})")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Pilih Kantong Tujuan:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    pockets.filter { it.id != fromPocketId }.forEach { pocket ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = toPocketId == pocket.id,
                                onClick = { toPocketId = pocket.id }
                            )
                            Text("${pocket.name} (${pocket.balance.toRupiah()})")
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Nominal Transfer (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (pockets.size >= 2) {
                Button(
                    onClick = {
                        val amount = amountText.toLongOrNull() ?: 0L
                        if (amount > 0 && fromPocketId.isNotEmpty() && toPocketId.isNotEmpty()) {
                            onConfirmTransfer(fromPocketId, toPocketId, amount)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B))
                ) {
                    Text("Transfer Sekarang")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun ReceiveQrDialog(
    accountNumber: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var amountText by remember { mutableStateOf("") }
    val enteredAmount = amountText.toLongOrNull()

    val qrPayload = "POCKETPAY|$accountNumber|${enteredAmount ?: 0L}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "QR Terima Transfer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tunjukkan QR ini ke pengirim untuk menerima transfer dana.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(2.dp, Color(0xFF064E3B), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeCanvas(dataText = qrPayload)
                }

                Text(
                    text = "No. Rekening: $accountNumber",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                if (enteredAmount != null && enteredAmount > 0) {
                    Text(
                        text = "Nominal: ${enteredAmount.toRupiah()}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF064E3B),
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Set Nominal Transfer (Opsional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val shareText = """
                        Transfer ke Rekening PocketPay:
                        No. Rekening: $accountNumber
                        ${if (enteredAmount != null && enteredAmount > 0) "Nominal: ${enteredAmount.toRupiah()}" else ""}
                    """.trimIndent()

                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Bagikan Informasi Transfer"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B))
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bagikan QR / No. Rek")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun QrCodeCanvas(dataText: String) {
    val seed = abs(dataText.hashCode())
    val gridCount = 13

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellSize = size.width / gridCount

        val drawFinderPattern = { startX: Int, startY: Int ->
            drawRect(
                color = Color.Black,
                topLeft = Offset(startX * cellSize, startY * cellSize),
                size = Size(3 * cellSize, 3 * cellSize)
            )
            drawRect(
                color = Color.White,
                topLeft = Offset((startX + 0.5f) * cellSize, (startY + 0.5f) * cellSize),
                size = Size(2 * cellSize, 2 * cellSize)
            )
            drawRect(
                color = Color.Black,
                topLeft = Offset((startX + 1f) * cellSize, (startY + 1f) * cellSize),
                size = Size(1 * cellSize, 1 * cellSize)
            )
        }

        drawFinderPattern(0, 0)
        drawFinderPattern(gridCount - 3, 0)
        drawFinderPattern(0, gridCount - 3)

        var currentSeed = seed
        for (r in 0 until gridCount) {
            for (c in 0 until gridCount) {
                val isFinderArea = (r < 3 && c < 3) || (r < 3 && c >= gridCount - 3) || (r >= gridCount - 3 && c < 3)
                if (!isFinderArea) {
                    currentSeed = (currentSeed * 1103515245 + 12345) and 0x7fffffff
                    if (currentSeed % 2 == 0) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionPinDialog(
    merchant: String,
    amount: Long,
    onDismiss: () -> Unit,
    onPinSuccess: () -> Unit
) {
    val pinContext = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val isError = errorText != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Masukkan PIN Transaksi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Verifikasi pembayaran ${amount.toRupiah()} ke $merchant",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pin = it
                            errorText = null
                        }
                    },
                    label = { Text("6 Digit PIN") },
                    singleLine = true,
                    isError = isError,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )

                errorText?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = PinManager.verify(pinContext, pin)
                    if (result == PinResult.Success) {
                        onPinSuccess()
                    } else {
                        errorText = PinManager.describe(result)
                        pin = ""
                    }
                },
                enabled = pin.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B))
            ) {
                Text("Konfirmasi PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun PocketCardItem(
    pocket: PocketEntity,
    isHidden: Boolean = false,
    onClick: () -> Unit
) {
    val isLocked = pocket.pocketType == PocketType.LOCKED_DEPOSIT
    val currentTime = System.currentTimeMillis()
    val isMatured = pocket.maturityDate != null && currentTime >= pocket.maturityDate

    Card(
        modifier = Modifier
            .width(180.dp)
            .height(if (pocket.targetAmount != null) 168.dp else 145.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) Color(0xFFFFF7ED) else Color(0xFFECFDF5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isLocked) Color(0xFFFFEDD5) else Color(0xFFD1FAE5),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Savings,
                        contentDescription = null,
                        tint = if (isLocked) Color(0xFFC2410C) else Color(0xFF047857),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "${pocket.interestRatePercent}% p.a.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) Color(0xFFC2410C) else Color(0xFF047857)
                )
            }

            Column {
                Text(
                    text = pocket.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatAmount(pocket.balance, isHidden),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )

                pocket.targetAmount?.let { target ->
                    val progress = (pocket.balance.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = Color(0xFF059669),
                        trackColor = Color(0xFFD1FAE5)
                    )
                    Text(
                        text = if (pocket.balance >= target) "Target tercapai \u2713"
                        else if (isHidden) "Target ••••"
                        else "${(progress * 100).toInt()}% dari ${target.toRupiah()}",
                        fontSize = 10.sp,
                        color = Color(0xFF047857)
                    )
                }

                if (isLocked && pocket.maturityDate != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isMatured) "Jatuh Tempo" else "s/d ${pocket.maturityDate.toDateFormatted()}",
                        fontSize = 10.sp,
                        color = if (isMatured) Color(0xFF15803D) else Color(0xFFC2410C)
                    )
                }
            }
        }
    }
}

@Composable
fun ManagePocketBalanceDialog(
    pocket: PocketEntity,
    onDismiss: () -> Unit,
    onExecute: (amount: Long, actionType: PocketActionType) -> Unit,
    onClosePocket: () -> Unit
) {
    var amountText by remember { mutableStateOf("50000") }
    var selectedAction by remember { mutableStateOf(PocketActionType.TOP_UP) }
    var showConfirmCloseDialog by remember { mutableStateOf(false) }

    val currentTime = System.currentTimeMillis()
    val isDepositLocked = pocket.pocketType == PocketType.LOCKED_DEPOSIT && (pocket.maturityDate ?: 0L) > currentTime

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kelola Saldo: ${pocket.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Saldo Saat Ini: ${pocket.balance.toRupiah()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = (selectedAction == PocketActionType.TOP_UP),
                        onClick = { selectedAction = PocketActionType.TOP_UP },
                        label = { Text("Top-Up") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = (selectedAction == PocketActionType.WITHDRAW),
                        onClick = { selectedAction = PocketActionType.WITHDRAW },
                        label = { Text("Tarik Saldo") },
                        enabled = !isDepositLocked,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isDepositLocked) {
                    Text(
                        text = "Kantong Deposito belum jatuh tempo (${pocket.maturityDate?.toDateFormatted() ?: "-"}). Saldo belum dapat ditarik atau ditutup.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Nominal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = { showConfirmCloseDialog = true },
                    enabled = !isDepositLocked,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup Kantong Ini")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    if (amount > 0) {
                        onExecute(amount, selectedAction)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B))
            ) {
                Text(if (selectedAction == PocketActionType.TOP_UP) "Top-Up Sekarang" else "Tarik Saldo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )

    if (showConfirmCloseDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCloseDialog = false },
            title = { Text("Tutup Kantong?") },
            text = {
                Text(
                    "Seluruh sisa saldo sebesar ${pocket.balance.toRupiah()} akan otomatis dikembalikan ke Rekening Utama."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmCloseDialog = false
                        onClosePocket()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Ya, Tutup Kantong")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCloseDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun CreatePocketDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Long, type: PocketType, tenorMonths: Int?, targetAmount: Long?) -> Unit
) {
    var targetText by remember { mutableStateOf("") }
    var pocketName by remember { mutableStateOf("") }
    var initialAmountText by remember { mutableStateOf("100000") }
    var selectedType by remember { mutableStateOf(PocketType.SPENDING) }
    var selectedTenor by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alokasi Kantong Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pocketName,
                    onValueChange = { pocketName = it },
                    label = { Text("Nama Kantong") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = initialAmountText,
                    onValueChange = { initialAmountText = it },
                    label = { Text("Saldo Awal Dialokasikan (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Tipe Kantong:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = (selectedType == PocketType.SPENDING),
                        onClick = { selectedType = PocketType.SPENDING }
                    )
                    Text("Kantong Belanja (3.5%)")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = (selectedType == PocketType.LOCKED_DEPOSIT),
                        onClick = { selectedType = PocketType.LOCKED_DEPOSIT }
                    )
                    Text("Deposito Terkunci (6.0%)")
                }

                if (selectedType == PocketType.SPENDING) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) targetText = it },
                        label = { Text("Target Tabungan (Rp, opsional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedType == PocketType.LOCKED_DEPOSIT) {
                    Text(
                        text = "Pilih Tenor Deposito:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 3, 6).forEach { tenor ->
                            FilterChip(
                                selected = (selectedTenor == tenor),
                                onClick = { selectedTenor = tenor },
                                label = { Text("$tenor Bln") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = initialAmountText.toLongOrNull() ?: 0L
                    if (pocketName.isNotBlank() && amount > 0) {
                        onConfirm(
                            pocketName,
                            amount,
                            selectedType,
                            if (selectedType == PocketType.LOCKED_DEPOSIT) selectedTenor else null,
                            targetText.toLongOrNull()?.takeIf { it > 0 && selectedType == PocketType.SPENDING }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B))
            ) {
                Text("Simpan & Potong Rekening")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun TransactionItemRow(
    tx: TransactionEntity,
    isHidden: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = when (tx.type) {
                            TxType.DEBIT -> Color(0xFFFEE2E2)
                            TxType.CREDIT -> Color(0xFFD1FAE5)
                            TxType.REALLOCATION -> Color(0xFFECFDF5)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (tx.type) {
                        TxType.DEBIT -> Icons.Default.ArrowUpward
                        TxType.CREDIT -> Icons.Default.ArrowDownward
                        TxType.REALLOCATION -> Icons.Default.SwapHoriz
                    },
                    contentDescription = null,
                    tint = when (tx.type) {
                        TxType.DEBIT -> Color(0xFFDC2626)
                        TxType.CREDIT -> Color(0xFF059669)
                        TxType.REALLOCATION -> Color(0xFF064E3B)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Sumber: ${tx.sourceOfFund}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            val prefix = when (tx.type) {
                TxType.DEBIT -> "- "
                TxType.CREDIT -> "+ "
                TxType.REALLOCATION -> ""
            }
            Text(
                text = if (isHidden) "Rp ••••••" else "$prefix${tx.amount.toRupiah()}",
                fontWeight = FontWeight.ExtraBold,
                color = when (tx.type) {
                    TxType.DEBIT -> Color(0xFFDC2626)
                    TxType.CREDIT -> Color(0xFF059669)
                    TxType.REALLOCATION -> Color(0xFF0F172A)
                }
            )
        }
    }
}

@Composable
fun PaymentQrisDialog(
    initialMerchant: String = "Kantin Mahasiswa PocketPay",
    initialAmount: Long = 23_500L,
    pockets: List<PocketEntity>,
    onDismiss: () -> Unit,
    onConfirmPayment: (merchant: String, amount: Long, pocketId: String?, isRoundUp: Boolean) -> Unit
) {
    var merchant by remember { mutableStateOf(initialMerchant) }
    var amountText by remember { mutableStateOf(initialAmount.toString()) }
    var selectedPocketId by remember { mutableStateOf<String?>(null) }
    var isRoundUpEnabled by remember { mutableStateOf(true) }

    val amount = amountText.toLongOrNull() ?: 0L
    val remainder = amount % 5000L
    val roundUpDiff = if (remainder == 0L) 0L else (5000L - remainder)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konfirmasi Bayar QRIS") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Nama Merchant") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Nominal Pembayaran (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFECFDF5), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Celengan Receh (Round-Up)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF064E3B)
                        )
                        Text(
                            text = if (roundUpDiff > 0) "Bulatkan Rp 5.000 (+ Rp ${roundUpDiff.toRupiah()})" else "Nominal kelipatan Rp 5.000",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color(0xFF047857)
                        )
                    }

                    Switch(
                        checked = isRoundUpEnabled,
                        onCheckedChange = { isRoundUpEnabled = it }
                    )
                }

                val targetP = pockets.find { it.id == selectedPocketId }
                if (targetP != null && targetP.balance < amount) {
                    val deficit = amount - targetP.balance
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF3C7)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Smart Cascading Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Saldo ${targetP.name} kurang ${deficit.toRupiah()}. Sisa ${deficit.toRupiah()} ditalangi dari Rekening Utama.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Pilih Sumber Dana (Source-of-Fund):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = (selectedPocketId == null),
                        onClick = { selectedPocketId = null }
                    )
                    Text(text = "Rekening Utama")
                }

                pockets.forEach { pocket ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = (selectedPocketId == pocket.id),
                            onClick = { selectedPocketId = pocket.id }
                        )
                        Text(text = "${pocket.name} (${pocket.balance.toRupiah()})")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amount > 0 && merchant.isNotBlank()) {
                        onConfirmPayment(merchant, amount, selectedPocketId, isRoundUpEnabled)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B))
            ) {
                Text("Konfirmasi & Bayar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun SplashScreenPreview() {
    SplashScreen(onSplashFinished = {})
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun AccountProfileScreenPreview() {
    AccountProfileScreen(
        currentName = "Nasabah Utama",
        currentPhone = "0812-3456-7890",
        currentEmail = "nasabah@pocketpay.com",
        userTier = "Gold Premier Member",
        accountNumber = "9012839102",
        onDismiss = {},
        onSave = { _, _, _ -> }
    )
}