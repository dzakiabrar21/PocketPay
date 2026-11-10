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

@Composable
fun PaymentQrisDialog(
    initialMerchant: String = "Kantin Mahasiswa PocketPay",
    initialAmount: Long = 23_500L,
    pockets: List<PocketEntity>,
    onDismiss: () -> Unit,
    onConfirmPayment: (merchant: String, amount: Long, pocketId: String?, isRoundUp: Boolean) -> Unit
) {
    var merchant by remember { mutableStateOf(initialMerchant) }
    var amountText by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toString() else "") }
    var selectedPocketId by remember { mutableStateOf<String?>(null) }
    var isRoundUpEnabled by remember { mutableStateOf(true) }

    val amount = amountText.toLongOrNull() ?: 0L
    val remainder = amount % 5000L
    val roundUpDiff = if (remainder == 0L) 0L else (5000L - remainder)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pay_konfirmasi_bayar_qris)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text(stringResource(R.string.pay_nama_merchant)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.pay_nominal_pembayaran_rp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_celengan_receh_round_up),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PocketTheme.colors.brandText
                        )
                        Text(
                            text = if (roundUpDiff > 0) stringResource(R.string.pay_bulatkan_rp_5_000, roundUpDiff.toRupiah()) else stringResource(R.string.pay_nominal_kelipatan_rp_5_000),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = PocketTheme.colors.accentText
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
                            containerColor = PocketTheme.colors.amberContainer
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
                                tint = PocketTheme.colors.amberIcon,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.pay_smart_cascading_active),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PocketTheme.colors.amberText
                                )
                                Text(
                                    text = stringResource(R.string.pay_saldo_kurang_sisa_ditalangi_dari_rek, targetP.name, deficit.toRupiah(), deficit.toRupiah()),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = PocketTheme.colors.amberText
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.pay_pilih_sumber_dana_source_of_fund),
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
                    Text(text = stringResource(R.string.pay_rekening_utama))
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.pay_konfirmasi_bayar))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pay_batal)) }
        }
    )
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
                text = stringResource(R.string.pay_masukkan_pin_transaksi),
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
                    text = stringResource(R.string.pay_verifikasi_pembayaran_ke, amount.toRupiah(), merchant),
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
                    label = { Text(stringResource(R.string.pay_6_digit_pin)) },
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
                        errorText = PinManager.describe(pinContext, result)
                        pin = ""
                    }
                },
                enabled = pin.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.pay_konfirmasi_pin))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pay_batal))
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

    val qrPayload = remember(accountNumber, enteredAmount) {
        com.example.pocketpay.domain.qris.Qris.build("PocketPay", "JAKARTA", accountNumber, enteredAmount)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.pay_qr_terima_transfer),
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
                    text = stringResource(R.string.pay_tunjukkan_qr_ini_ke_pengirim_untuk),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(2.dp, PocketTheme.colors.brandText, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeCanvas(dataText = qrPayload)
                }

                Text(
                    text = stringResource(R.string.pay_no_rekening, accountNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                if (enteredAmount != null && enteredAmount > 0) {
                    Text(
                        text = stringResource(R.string.pay_nominal, enteredAmount.toRupiah()),
                        style = MaterialTheme.typography.titleSmall,
                        color = PocketTheme.colors.brandText,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.pay_set_nominal_transfer_opsional)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val shareText = buildString {
                        append(context.getString(R.string.pay_share_transfer, accountNumber))
                        if (enteredAmount != null && enteredAmount > 0) {
                            append('\n').append(context.getString(R.string.pay_nominal, enteredAmount.toRupiah()))
                        }
                    }

                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.pay_bagikan_informasi_transfer)))
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.pay_bagikan_qr_no_rek))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pay_tutup))
            }
        }
    )
}

@Composable
fun QrCodeCanvas(dataText: String) {
    // Real, scannable QR code (ZXing encodes the matrix; we only paint the modules).
    val matrix = remember(dataText) {
        com.google.zxing.qrcode.QRCodeWriter().encode(
            dataText,
            com.google.zxing.BarcodeFormat.QR_CODE,
            0, 0,
            mapOf(com.google.zxing.EncodeHintType.MARGIN to 0)
        )
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cell = size.width / matrix.width
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix.get(x, y)) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(x * cell, y * cell),
                        size = Size(cell + 0.5f, cell + 0.5f)
                    )
                }
            }
        }
    }
}
