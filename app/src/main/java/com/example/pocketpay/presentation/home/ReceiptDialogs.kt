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
