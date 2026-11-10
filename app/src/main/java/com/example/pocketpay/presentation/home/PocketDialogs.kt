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
