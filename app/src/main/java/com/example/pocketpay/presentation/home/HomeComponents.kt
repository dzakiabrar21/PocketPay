package com.example.pocketpay.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketpay.R
import com.example.pocketpay.data.PocketEntity
import com.example.pocketpay.data.PocketType
import com.example.pocketpay.data.TransactionEntity
import com.example.pocketpay.data.TxStatus
import com.example.pocketpay.data.TxType
import com.example.pocketpay.ui.theme.PocketTheme

/**
 * Pocket as a coloured card: lime for spending pockets, deep navy for locked deposits.
 * Shows the goal progress or the maturity date when relevant.
 */
@Composable
fun PocketCardItem(
    pocket: PocketEntity,
    modifier: Modifier = Modifier,
    isHidden: Boolean = false,
    fixedHeight: androidx.compose.ui.unit.Dp? = null,
    onClick: () -> Unit
) {
    val c = PocketTheme.colors
    val isLocked = pocket.pocketType == PocketType.LOCKED_DEPOSIT
    val matured = pocket.maturityDate != null && System.currentTimeMillis() >= pocket.maturityDate
    val brush = if (isLocked) Brush.linearGradient(listOf(c.hero, c.heroAlt))
    else Brush.linearGradient(listOf(c.lime, c.lime.copy(alpha = 0.78f)))
    val fg = if (isLocked) c.onHero else c.onLime
    val fgMuted = fg.copy(alpha = 0.72f)

    Box(
        modifier = modifier
            .width(196.dp)
            .then(if (fixedHeight != null) Modifier.height(fixedHeight) else Modifier)
            .clip(RoundedCornerShape(26.dp))
            .background(brush)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(fg.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isLocked) Icons.Rounded.Lock else Icons.Rounded.Savings, null, tint = fg, modifier = Modifier.size(18.dp))
                }
                Text(
                    stringResource(R.string.comp_p_a, pocket.interestRatePercent),
                    color = fg, fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(pocket.name, color = fgMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                formatAmount(pocket.balance, isHidden),
                color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, maxLines = 1
            )
            pocket.targetAmount?.let { target ->
                val progress = (pocket.balance.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = fg, trackColor = fg.copy(alpha = 0.2f), gapSize = 0.dp, drawStopIndicator = {}
                )
                Text(
                    if (pocket.balance >= target) stringResource(R.string.comp_target_tercapai_u2713)
                    else if (isHidden) stringResource(R.string.comp_target)
                    else stringResource(R.string.comp_dari, (progress * 100).toInt(), target.toRupiah()),
                    color = fgMuted, fontSize = 11.sp, maxLines = 1
                )
            }
            if (isLocked && pocket.maturityDate != null) {
                Text(
                    if (matured) stringResource(R.string.comp_jatuh_tempo)
                    else stringResource(R.string.comp_s_d, pocket.maturityDate.toDateFormatted()),
                    color = if (matured) c.lime else fgMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** One transaction: icon tile, title with date, amount and status. Place several inside a SoftCard. */
@Composable
fun TransactionItemRow(
    tx: TransactionEntity,
    modifier: Modifier = Modifier,
    isHidden: Boolean = false,
    onClick: () -> Unit = {}
) {
    val c = PocketTheme.colors
    val (tileBg, tileFg, icon) = when (tx.type) {
        TxType.DEBIT -> Triple(c.dangerContainer, c.danger, Icons.Rounded.ArrowUpward)
        TxType.CREDIT -> Triple(c.soft, c.accent, Icons.Rounded.ArrowDownward)
        TxType.REALLOCATION -> Triple(c.soft, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Rounded.SwapHoriz)
    }
    val reversed = tx.status == TxStatus.REVERSED
    val prefix = when (tx.type) { TxType.DEBIT -> "- "; TxType.CREDIT -> "+ "; TxType.REALLOCATION -> "" }

    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(tileBg), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tileFg, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                tx.timestamp.toDateFormatted(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (isHidden) "Rp ••••••" else "$prefix${tx.amount.toRupiah()}",
                fontWeight = FontWeight.Bold,
                textDecoration = if (reversed) TextDecoration.LineThrough else null,
                color = when {
                    reversed -> MaterialTheme.colorScheme.onSurfaceVariant
                    tx.type == TxType.CREDIT -> c.accent
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1
            )
            val (label, color) = when (tx.status) {
                TxStatus.SUCCESS -> stringResource(
                    when (tx.type) { TxType.DEBIT -> R.string.status_paid; TxType.CREDIT -> R.string.status_received; TxType.REALLOCATION -> R.string.status_moved }
                ) to c.successText
                TxStatus.PENDING -> statusLabel(tx.status) to c.amberText
                TxStatus.REVERSED -> statusLabel(tx.status) to c.warnText
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun statusLabel(status: TxStatus): String = when (status) {
    TxStatus.SUCCESS -> stringResource(R.string.status_success)
    TxStatus.PENDING -> stringResource(R.string.status_pending)
    TxStatus.REVERSED -> stringResource(R.string.status_reversed)
}

/** Maps the stored (Indonesian) authentication labels to the current language; unknown values pass through. */
@Composable
fun authMethodLabel(raw: String): String = when (raw) {
    "PIN 6-Digit" -> stringResource(R.string.auth_pin)
    "Biometrik (Sidik Jari / Face ID)" -> stringResource(R.string.auth_biometric)
    "Biometrik / PIN 6-Digit" -> stringResource(R.string.auth_default)
    "Otomatis" -> stringResource(R.string.auth_auto)
    else -> raw
}

/** Thin colour for dividers inside list cards. */
@Composable
fun listDividerColor(): Color = MaterialTheme.colorScheme.outlineVariant

/** Human label for a transaction type (the enum names are internal). */
@Composable
fun txTypeLabel(type: TxType): String = stringResource(
    when (type) { TxType.DEBIT -> R.string.type_expense; TxType.CREDIT -> R.string.type_income; TxType.REALLOCATION -> R.string.type_transfer }
)
