package com.example.pocketpay.presentation.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketpay.R
import com.example.pocketpay.data.PocketEntity
import com.example.pocketpay.data.PocketType
import com.example.pocketpay.data.TransactionEntity
import com.example.pocketpay.data.TxStatus
import com.example.pocketpay.presentation.statement.PdfStatementGenerator
import com.example.pocketpay.ui.components.CircleIcon
import com.example.pocketpay.ui.components.DashedDivider
import com.example.pocketpay.ui.components.PillButton
import com.example.pocketpay.ui.components.PillStyle
import com.example.pocketpay.ui.components.PocketChip
import com.example.pocketpay.ui.components.PocketTextField
import com.example.pocketpay.ui.components.SoftCard
import com.example.pocketpay.ui.components.StatusChip
import com.example.pocketpay.ui.theme.PocketTheme

/** Bottom sheet shell shared by every secondary action (create/manage pocket, transfer, statement, receipt). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) { content() }
}

@Composable
private fun SheetColumn(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        content()
    }
}

private fun amountFilter(raw: String): String? = raw.takeIf { it.length <= 12 && it.all(Char::isDigit) }

private val NumberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)

// ---------------------------------------------------------------------------------------------
// Create pocket
// ---------------------------------------------------------------------------------------------

@Composable
fun CreatePocketSheet(
    onConfirm: (name: String, amount: Long, type: PocketType, tenorMonths: Int?, targetAmount: Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(PocketType.SPENDING) }
    var tenor by remember { mutableIntStateOf(1) }
    val amount = amountText.toLongOrNull() ?: 0L

    SheetColumn(stringResource(R.string.pocket_alokasi_kantong_baru)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TypeCard(type == PocketType.SPENDING, Icons.Rounded.Savings, stringResource(R.string.pocket_kantong_belanja_3_5), Modifier.weight(1f)) { type = PocketType.SPENDING }
            TypeCard(type == PocketType.LOCKED_DEPOSIT, Icons.Rounded.Lock, stringResource(R.string.pocket_deposito_terkunci_6_0), Modifier.weight(1f)) { type = PocketType.LOCKED_DEPOSIT }
        }
        PocketTextField(name, { name = it }, stringResource(R.string.pocket_nama_kantong))
        PocketTextField(
            amountText, { amountFilter(it)?.let { v -> amountText = v } },
            stringResource(R.string.pocket_saldo_awal_dialokasikan_rp), prefix = "Rp ", keyboardOptions = NumberKeyboard, visualTransformation = com.example.pocketpay.ui.components.ThousandsTransformation
        )
        if (type == PocketType.SPENDING) {
            PocketTextField(
                targetText, { amountFilter(it)?.let { v -> targetText = v } },
                stringResource(R.string.pocket_target_tabungan_rp_opsional), prefix = "Rp ", keyboardOptions = NumberKeyboard, visualTransformation = com.example.pocketpay.ui.components.ThousandsTransformation
            )
        } else {
            Text(stringResource(R.string.pocket_pilih_tenor_deposito), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 6).forEach { t -> PocketChip(tenor == t, { tenor = t }, stringResource(R.string.pocket_bln, t)) }
            }
        }
        PillButton(
            stringResource(R.string.pocket_simpan_potong_rekening),
            {
                onConfirm(
                    name.trim(), amount, type,
                    if (type == PocketType.LOCKED_DEPOSIT) tenor else null,
                    targetText.toLongOrNull()?.takeIf { it > 0 && type == PocketType.SPENDING }
                )
            },
            modifier = Modifier.fillMaxWidth(), enabled = name.isNotBlank() && amount > 0, height = 56.dp
        )
    }
}

@Composable
private fun TypeCard(selected: Boolean, icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val c = PocketTheme.colors
    Surface(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp),
        color = if (selected) c.lime.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) c.lime else c.border)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CircleIcon(icon, null, container = if (selected) c.lime else c.soft, tint = if (selected) c.onLime else c.brandText, size = 38.dp)
            Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Manage pocket
// ---------------------------------------------------------------------------------------------

@Composable
fun ManagePocketSheet(
    pocket: PocketEntity,
    onExecute: (amount: Long, actionType: PocketActionType) -> Unit,
    onClosePocket: () -> Unit
) {
    val c = PocketTheme.colors
    var amountText by remember { mutableStateOf("") }
    var confirmClose by remember { mutableStateOf(false) }
    val amount = amountText.toLongOrNull() ?: 0L
    val locked = pocket.pocketType == PocketType.LOCKED_DEPOSIT && (pocket.maturityDate ?: 0L) > System.currentTimeMillis()

    if (confirmClose) {
        AlertDialog(
            onDismissRequest = { confirmClose = false },
            title = { Text(stringResource(R.string.pocket_tutup_kantong)) },
            text = { Text(stringResource(R.string.pocket_seluruh_sisa_saldo_sebesar_akan_otom, pocket.balance.toRupiah())) },
            confirmButton = {
                PillButton(stringResource(R.string.pocket_ya_tutup_kantong), { confirmClose = false; onClosePocket() }, height = 44.dp)
            },
            dismissButton = { TextButton(onClick = { confirmClose = false }) { Text(stringResource(R.string.pay_batal)) } }
        )
    }

    SheetColumn(stringResource(R.string.pocket_kelola_saldo, pocket.name)) {
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIcon(
                    if (pocket.pocketType == PocketType.LOCKED_DEPOSIT) Icons.Rounded.Lock else Icons.Rounded.Savings, null,
                    container = c.lime, tint = c.onLime, size = 48.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.stats_current_balance), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(pocket.balance.toRupiah(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                StatusChip(stringResource(R.string.comp_p_a, pocket.interestRatePercent), c.lime.copy(alpha = 0.25f), c.accentText)
            }
        }
        if (locked) {
            Surface(shape = RoundedCornerShape(18.dp), color = c.warnContainer) {
                Text(
                    stringResource(R.string.pocket_kantong_deposito_belum_jatuh_tempo_s, (pocket.maturityDate ?: 0L).toDateFormatted()),
                    color = c.warnText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(14.dp)
                )
            }
        }
        PocketTextField(
            amountText, { amountFilter(it)?.let { v -> amountText = v } },
            stringResource(R.string.pocket_nominal_rp), prefix = "Rp ", keyboardOptions = NumberKeyboard, visualTransformation = com.example.pocketpay.ui.components.ThousandsTransformation
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(
                stringResource(R.string.pocket_top_up_sekarang), { onExecute(amount, PocketActionType.TOP_UP) },
                modifier = Modifier.weight(1f), enabled = amount > 0 && !locked, height = 54.dp
            )
            PillButton(
                stringResource(R.string.pocket_tarik_saldo), { onExecute(amount, PocketActionType.WITHDRAW) },
                modifier = Modifier.weight(1f), style = PillStyle.OUTLINE, enabled = amount > 0 && !locked, height = 54.dp
            )
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(onClick = { confirmClose = true }, enabled = !locked) {
                Text(stringResource(R.string.pocket_tutup_kantong_ini), color = if (locked) MaterialTheme.colorScheme.onSurfaceVariant else c.danger, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Transfer between pockets
// ---------------------------------------------------------------------------------------------

@Composable
fun TransferSheet(
    pockets: List<PocketEntity>,
    onConfirm: (fromPocketId: String, toPocketId: String, amount: Long) -> Unit
) {
    var fromId by remember { mutableStateOf(pockets.firstOrNull()?.id ?: "") }
    var toId by remember { mutableStateOf(pockets.getOrNull(1)?.id ?: "") }
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.toLongOrNull() ?: 0L

    SheetColumn(stringResource(R.string.pocket_transfer_antar_kantong)) {
        if (pockets.size < 2) {
            Surface(shape = RoundedCornerShape(18.dp), color = PocketTheme.colors.amberContainer) {
                Text(stringResource(R.string.pocket_anda_membutuhkan_minimal_2_kantong_b), color = PocketTheme.colors.amberText, modifier = Modifier.padding(14.dp))
            }
        } else {
            Text(stringResource(R.string.pocket_pilih_kantong_asal), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            pockets.forEach { p ->
                PocketPickRow(p, selected = fromId == p.id) {
                    fromId = p.id
                    if (toId == p.id) toId = pockets.firstOrNull { it.id != p.id }?.id ?: ""
                }
            }
            Text(stringResource(R.string.pocket_pilih_kantong_tujuan), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            pockets.filter { it.id != fromId }.forEach { p -> PocketPickRow(p, selected = toId == p.id) { toId = p.id } }
            PocketTextField(
                amountText, { amountFilter(it)?.let { v -> amountText = v } },
                stringResource(R.string.pocket_nominal_transfer_rp), prefix = "Rp ", keyboardOptions = NumberKeyboard, visualTransformation = com.example.pocketpay.ui.components.ThousandsTransformation
            )
            PillButton(
                stringResource(R.string.pocket_transfer_sekarang), { onConfirm(fromId, toId, amount) },
                modifier = Modifier.fillMaxWidth(), enabled = amount > 0 && fromId.isNotEmpty() && toId.isNotEmpty() && fromId != toId, height = 56.dp
            )
        }
    }
}

@Composable
private fun PocketPickRow(pocket: PocketEntity, selected: Boolean, onClick: () -> Unit) {
    val c = PocketTheme.colors
    Surface(
        onClick = onClick, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) c.lime else c.border)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIcon(Icons.Rounded.AccountBalanceWallet, null, container = if (selected) c.lime.copy(alpha = 0.25f) else c.soft, tint = c.brandText, size = 38.dp)
            Spacer(Modifier.width(12.dp))
            Text(pocket.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text(pocket.balance.toRupiah(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// E-statement
// ---------------------------------------------------------------------------------------------

@Composable
fun EStatementSheet(
    accountNumber: String,
    currentBalance: Long,
    transactions: List<TransactionEntity>
) {
    val context = LocalContext.current
    var period by remember { mutableStateOf(DateRangeFilter.ALL) }
    val filtered = remember(period, transactions) {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000
        transactions.filter { tx ->
            when (period) {
                DateRangeFilter.ALL -> true
                DateRangeFilter.LAST_7_DAYS -> now - tx.timestamp <= 7 * day
                DateRangeFilter.LAST_30_DAYS -> now - tx.timestamp <= 30 * day
            }
        }
    }
    fun generate(): java.io.File? = PdfStatementGenerator.generateEStatementPdf(
        context = context, accountNumber = accountNumber, periodLabel = context.getString(period.labelRes),
        transactions = filtered, currentBalance = currentBalance
    )

    SheetColumn(stringResource(R.string.home_export_e_statement_pdf)) {
        Text(stringResource(R.string.receipt_cetak_rekening_koran_resmi_berformat), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.receipt_pilih_periode_laporan), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateRangeFilter.entries.forEach { p -> PocketChip(period == p, { period = p }, stringResource(p.labelRes)) }
        }
        SoftCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.receipt_ringkasan_dokumen), fontWeight = FontWeight.Bold)
                SummaryRow(stringResource(R.string.receipt_no_rekening, accountNumber))
                SummaryRow(stringResource(R.string.receipt_periode, stringResource(period.labelRes)))
                SummaryRow(stringResource(R.string.receipt_total_transaksi_mutasi, filtered.size))
                SummaryRow(stringResource(R.string.receipt_saldo_akhir, currentBalance.toRupiah()), bold = true)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(
                stringResource(R.string.receipt_buka_pdf),
                {
                    generate()?.let { PdfStatementGenerator.openEStatementPdf(context, it) }
                        ?: Toast.makeText(context, context.getString(R.string.receipt_gagal_membuat_pdf), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f), icon = Icons.Rounded.PictureAsPdf, style = PillStyle.OUTLINE, height = 54.dp
            )
            PillButton(
                stringResource(R.string.receipt_bagikan_pdf),
                {
                    generate()?.let { PdfStatementGenerator.shareEStatementPdf(context, it) }
                        ?: Toast.makeText(context, context.getString(R.string.receipt_gagal_membuat_pdf), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f), icon = Icons.Rounded.Share, height = 54.dp
            )
        }
    }
}

@Composable
private fun SummaryRow(text: String, bold: Boolean = false) {
    Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
}

// ---------------------------------------------------------------------------------------------
// Transaction receipt
// ---------------------------------------------------------------------------------------------

@Composable
fun ReceiptSheet(transaction: TransactionEntity, onRefund: () -> Unit) {
    val context = LocalContext.current
    val c = PocketTheme.colors
    var confirmRefund by remember { mutableStateOf(false) }

    val receiptSummary = stringResource(
        R.string.receipt_share_text,
        transaction.status.name, transaction.referenceNo, transaction.title, transaction.amount.toRupiah(),
        txTypeLabel(transaction.type), transaction.sourceOfFund, transaction.timestamp.toFullDateTimeFormatted(),
        authMethodLabel(transaction.authMethod), transaction.idempotencyKey
    )

    if (confirmRefund) {
        AlertDialog(
            onDismissRequest = { confirmRefund = false },
            title = { Text(stringResource(R.string.refund_confirm_title)) },
            text = { Text(stringResource(R.string.refund_confirm_message, transaction.amount.toRupiah())) },
            confirmButton = { PillButton(stringResource(R.string.refund_confirm_yes), { confirmRefund = false; onRefund() }, height = 44.dp) },
            dismissButton = { TextButton(onClick = { confirmRefund = false }) { Text(stringResource(R.string.pay_batal)) } }
        )
    }

    val chipBg = when (transaction.status) {
        TxStatus.SUCCESS -> c.lime.copy(alpha = 0.25f); TxStatus.PENDING -> c.amberContainer; TxStatus.REVERSED -> c.warnChip
    }
    val chipFg = when (transaction.status) {
        TxStatus.SUCCESS -> c.accentText; TxStatus.PENDING -> c.amberText; TxStatus.REVERSED -> c.warnText
    }

    SheetColumn(stringResource(R.string.receipt_struk_transaksi_digital)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(statusLabel(transaction.status), chipBg, chipFg)
            Text(transaction.amount.toRupiah(), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        }
        SoftCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val rows = listOf(
                    stringResource(R.string.receipt_nama_merchant) to transaction.title,
                    stringResource(R.string.receipt_no_referensi) to transaction.referenceNo,
                    stringResource(R.string.receipt_tipe_mutasi) to txTypeLabel(transaction.type),
                    stringResource(R.string.receipt_sumber_dana) to transaction.sourceOfFund,
                    stringResource(R.string.receipt_waktu) to transaction.timestamp.toFullDateTimeFormatted(),
                    stringResource(R.string.receipt_metode_otentikasi) to authMethodLabel(transaction.authMethod),
                    stringResource(R.string.receipt_idempotency_key) to transaction.idempotencyKey.take(12) + "..."
                )
                rows.forEachIndexed { i, (label, value) ->
                    if (i > 0) DashedDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(16.dp))
                        Text(value, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        if (transaction.isRefundable()) {
            PillButton(
                stringResource(R.string.refund_button), { confirmRefund = true },
                modifier = Modifier.fillMaxWidth(), icon = Icons.Rounded.Undo, style = PillStyle.OUTLINE, height = 54.dp
            )
        }
        PillButton(
            stringResource(R.string.receipt_bagikan_struk),
            {
                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, receiptSummary) }
                context.startActivity(Intent.createChooser(send, context.getString(R.string.receipt_bagikan_struk_digital)))
            },
            modifier = Modifier.fillMaxWidth(), icon = Icons.Rounded.Share, height = 56.dp
        )
    }
}
