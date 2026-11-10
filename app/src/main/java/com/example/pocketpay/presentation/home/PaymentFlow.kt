package com.example.pocketpay.presentation.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketpay.R
import com.example.pocketpay.data.PocketEntity
import com.example.pocketpay.domain.qris.Qris
import com.example.pocketpay.ui.components.Avatar
import com.example.pocketpay.ui.components.CircleIcon
import com.example.pocketpay.ui.components.DashedDivider
import com.example.pocketpay.ui.components.PillButton
import com.example.pocketpay.ui.components.PillStyle
import com.example.pocketpay.ui.components.PinDots
import com.example.pocketpay.ui.components.PinKeypad
import com.example.pocketpay.ui.components.PocketBackground
import com.example.pocketpay.ui.components.PocketTextField
import com.example.pocketpay.ui.components.SoftCard
import com.example.pocketpay.ui.components.StatusChip
import com.example.pocketpay.ui.components.SuccessBadge
import com.example.pocketpay.ui.theme.PocketTheme

// ---------------------------------------------------------------------------------------------
// PIN entry (lock screen, PIN setup, payment confirmation all share this)
// ---------------------------------------------------------------------------------------------

/**
 * Full-screen PIN pad. [onComplete] fires as soon as [length] digits are entered; the caller
 * validates and, on failure, bumps [resetKey] (and sets [error]) to clear the dots.
 */
@Composable
fun PinEntryScreen(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onDark: Boolean = true,
    error: String? = null,
    length: Int = 6,
    resetKey: Any = Unit,
    onBack: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    onComplete: (String) -> Unit
) {
    val c = PocketTheme.colors
    var pin by remember(resetKey) { mutableStateOf("") }
    val fg = if (onDark) c.onHero else MaterialTheme.colorScheme.onSurface
    val fgMuted = if (onDark) c.onHeroMuted else MaterialTheme.colorScheme.onSurfaceVariant
    if (onBack != null) BackHandler(onBack = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {}
            .background(
                if (onDark) Brush.verticalGradient(listOf(c.hero, c.heroAlt))
                else Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background))
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    CircleIcon(
                        Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.profile_kembali),
                        container = if (onDark) c.heroChip else MaterialTheme.colorScheme.surface,
                        tint = fg, onClick = onBack
                    )
                }
            }
            Spacer(Modifier.weight(0.5f))
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(c.lime.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Shield, null, tint = c.lime, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(title, color = fg, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, color = fgMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(28.dp))
            PinDots(length, pin.length, isError = error != null, onDark = onDark)
            Box(Modifier.height(36.dp).padding(top = 12.dp), contentAlignment = Alignment.Center) {
                if (error != null) {
                    Text(error, color = if (onDark) Color(0xFFFFA3A6) else c.danger, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.weight(1f))
            PinKeypad(
                onDigit = { d ->
                    if (pin.length < length) {
                        pin += d
                        if (pin.length == length) onComplete(pin)
                    }
                },
                onDelete = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                onDark = onDark,
                leading = leading
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// QRIS payment form
// ---------------------------------------------------------------------------------------------

@Composable
fun QrisPaymentScreen(
    initialMerchant: String,
    initialAmount: Long,
    pockets: List<PocketEntity>,
    mainBalance: Long,
    onBack: () -> Unit,
    onConfirm: (merchant: String, amount: Long, pocketId: String?, isRoundUp: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val c = PocketTheme.colors
    var merchant by remember { mutableStateOf(initialMerchant) }
    var amountText by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toString() else "") }
    var selectedPocketId by remember { mutableStateOf<String?>(null) }
    var roundUp by remember { mutableStateOf(true) }

    val amount = amountText.toLongOrNull() ?: 0L
    val remainder = amount % 5000L
    val roundUpDiff = if (remainder == 0L) 0L else 5000L - remainder
    val selectedPocket = pockets.firstOrNull { it.id == selectedPocketId }
    val deficit = if (selectedPocket != null && amount > selectedPocket.balance) amount - selectedPocket.balance else 0L

    PocketBackground(modifier) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleIcon(
                    Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.profile_kembali),
                    container = MaterialTheme.colorScheme.surface, onClick = onBack
                )
                Spacer(Modifier.width(14.dp))
                Text(stringResource(R.string.pay_konfirmasi_bayar_qris), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircleIcon(Icons.Rounded.Storefront, null, container = c.soft, tint = c.brandText, size = 48.dp)
                        Spacer(Modifier.width(12.dp))
                        PocketTextField(merchant, { merchant = it }, stringResource(R.string.pay_nama_merchant))
                    }
                }

                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.pay_nominal_pembayaran_rp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    BasicTextField(
                        value = amountText,
                        onValueChange = { if (it.length <= 12 && it.all(Char::isDigit)) amountText = it.trimStart('0') },
                        textStyle = TextStyle(
                            fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        cursorBrush = SolidColor(c.accent),
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                if (amountText.isEmpty()) Text("0", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.outline)
                                inner()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }

                Text(stringResource(R.string.pay_pilih_sumber_dana_source_of_fund), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SourceOption(
                        selected = selectedPocketId == null, icon = Icons.Rounded.AccountBalanceWallet,
                        title = stringResource(R.string.pay_rekening_utama), balance = mainBalance.toRupiah()
                    ) { selectedPocketId = null }
                    pockets.forEach { p ->
                        SourceOption(
                            selected = selectedPocketId == p.id, icon = Icons.Rounded.Savings,
                            title = p.name, balance = p.balance.toRupiah()
                        ) { selectedPocketId = p.id }
                    }
                }

                if (deficit > 0 && selectedPocket != null) {
                    Surface(shape = RoundedCornerShape(18.dp), color = c.amberContainer) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Bolt, null, tint = c.amberIcon)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(stringResource(R.string.pay_smart_cascading_active), color = c.amberText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    stringResource(R.string.pay_saldo_kurang_sisa_ditalangi_dari_rek, selectedPocket.name, deficit.toRupiah(), deficit.toRupiah()),
                                    color = c.amberText, style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircleIcon(Icons.Rounded.Savings, null, container = c.lime, tint = c.onLime, size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.home_celengan_receh_round_up), fontWeight = FontWeight.Bold)
                            Text(
                                if (roundUpDiff > 0) stringResource(R.string.pay_bulatkan_rp_5_000, roundUpDiff.toRupiah())
                                else stringResource(R.string.pay_nominal_kelipatan_rp_5_000),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = roundUp, onCheckedChange = { roundUp = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = c.lime, checkedThumbColor = c.onLime)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            PillButton(
                text = if (amount > 0) stringResource(R.string.pay_now_amount, amount.toRupiah()) else stringResource(R.string.pay_now),
                onClick = { onConfirm(merchant.trim(), amount, selectedPocketId, roundUp) },
                enabled = amount > 0 && merchant.isNotBlank(),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                height = 56.dp
            )
        }
    }
}

@Composable
private fun SourceOption(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    balance: String,
    onClick: () -> Unit
) {
    val c = PocketTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) c.lime else c.border)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIcon(icon, null, container = if (selected) c.lime.copy(alpha = 0.25f) else c.soft, tint = c.brandText, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(balance, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                Modifier.size(22.dp).clip(CircleShape)
                    .background(if (selected) c.lime else Color.Transparent)
                    .then(if (selected) Modifier else Modifier.background(MaterialTheme.colorScheme.outlineVariant, CircleShape).padding(2.dp).background(MaterialTheme.colorScheme.surface, CircleShape))
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Success
// ---------------------------------------------------------------------------------------------

/** Full-screen confirmation with a receipt-style detail card, used after every successful action. */
@Composable
fun SuccessScreen(
    data: SuccessAlertData,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDone)
    val context = LocalContext.current
    PocketBackground(modifier) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(48.dp))
                SuccessBadge()
                Spacer(Modifier.height(20.dp))
                Text(data.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (data.amount != null) {
                    Spacer(Modifier.height(18.dp))
                    Text(stringResource(R.string.success_amount_label), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text(data.amount.toRupiah(), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                }
                if (data.message.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(data.message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                }
                if (data.rows.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    SoftCard(contentPadding = PaddingValues(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            data.rows.forEachIndexed { i, (label, value) ->
                                if (i > 0) DashedDivider()
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.width(16.dp))
                                    Text(value, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (data.rows.isNotEmpty()) {
                    CircleIcon(
                        Icons.Rounded.Share, stringResource(R.string.receipt_bagikan_struk),
                        container = MaterialTheme.colorScheme.surface, size = 56.dp,
                        onClick = {
                            val text = buildString {
                                append(data.title)
                                data.amount?.let { append("\n").append(it.toRupiah()) }
                                data.rows.forEach { (l, v) -> append("\n").append(l).append(": ").append(v) }
                            }
                            val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                            context.startActivity(Intent.createChooser(send, context.getString(R.string.receipt_bagikan_struk_digital)))
                        }
                    )
                }
                PillButton(stringResource(R.string.success_done), onDone, modifier = Modifier.weight(1f), height = 56.dp)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Receive QR ("Scan me")
// ---------------------------------------------------------------------------------------------

@Composable
fun ReceiveQrScreen(
    accountNumber: String,
    name: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val c = PocketTheme.colors
    var amountText by remember { mutableStateOf("") }
    val entered = amountText.toLongOrNull()?.takeIf { it > 0 }
    val payload = remember(accountNumber, entered) { Qris.build("PocketPay", "JAKARTA", accountNumber, entered) }

    PocketBackground(modifier) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleIcon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.profile_kembali), container = MaterialTheme.colorScheme.surface, onClick = onBack)
                Spacer(Modifier.width(14.dp))
                Text(stringResource(R.string.receive_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(name, size = 48.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(name, fontWeight = FontWeight.Bold)
                            Text("PocketPay • $accountNumber", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(32.dp), color = Color.White, border = BorderStroke(1.dp, c.border)) {
                    Box(Modifier.padding(22.dp).size(250.dp), contentAlignment = Alignment.Center) {
                        QrCodeCanvas(dataText = payload)
                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color.White).padding(4.dp).clip(RoundedCornerShape(12.dp)).background(c.lime), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Savings, null, tint = c.onLime, modifier = Modifier.size(28.dp))
                        }
                    }
                }
                Text(
                    if (entered != null) stringResource(R.string.pay_nominal, entered.toRupiah()) else stringResource(R.string.receive_caption),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                    fontWeight = if (entered != null) FontWeight.Bold else FontWeight.Normal
                )
                PocketTextField(
                    amountText, { if (it.length <= 12 && it.all(Char::isDigit)) amountText = it },
                    stringResource(R.string.pay_set_nominal_transfer_opsional), prefix = "Rp ",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(4.dp))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(
                    stringResource(R.string.pay_bagikan_qr_no_rek),
                    {
                        val text = buildString {
                            append(context.getString(R.string.pay_share_transfer, accountNumber))
                            if (entered != null) append('\n').append(context.getString(R.string.pay_nominal, entered.toRupiah()))
                        }
                        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                        context.startActivity(Intent.createChooser(send, context.getString(R.string.pay_bagikan_informasi_transfer)))
                    },
                    modifier = Modifier.weight(1f), icon = Icons.Rounded.Share, style = PillStyle.OUTLINE, height = 54.dp
                )
                PillButton(stringResource(R.string.pay_tutup), onBack, modifier = Modifier.weight(1f), height = 54.dp)
            }
        }
    }
}

/** Real, scannable QR code (ZXing encodes the matrix; we only paint the modules). */
@Composable
fun QrCodeCanvas(dataText: String, modifier: Modifier = Modifier) {
    val matrix = remember(dataText) {
        com.google.zxing.qrcode.QRCodeWriter().encode(
            dataText,
            com.google.zxing.BarcodeFormat.QR_CODE,
            0, 0,
            mapOf(
                com.google.zxing.EncodeHintType.MARGIN to 0,
                // High error correction lets us cover the centre with the logo and still scan.
                com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
            )
        )
    }
    Canvas(modifier = modifier.fillMaxSize()) {
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

/** Little helper chip reused by flows: shows a coloured status pill. */
@Composable
fun SuccessChip(text: String) = StatusChip(text, PocketTheme.colors.lime.copy(alpha = 0.25f), PocketTheme.colors.accentText)
