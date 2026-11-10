package com.example.pocketpay.presentation.home

import androidx.compose.ui.text.style.TextDecoration
import com.example.pocketpay.data.TxStatus
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
            containerColor = if (isLocked) PocketTheme.colors.warnContainer else MaterialTheme.colorScheme.primaryContainer
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
                            color = if (isLocked) PocketTheme.colors.warnChip else MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Savings,
                        contentDescription = null,
                        tint = if (isLocked) PocketTheme.colors.warnText else PocketTheme.colors.accentText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.comp_p_a, pocket.interestRatePercent),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) PocketTheme.colors.warnText else PocketTheme.colors.accentText
                )
            }

            Column {
                Text(
                    text = pocket.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatAmount(pocket.balance, isHidden),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                pocket.targetAmount?.let { target ->
                    val progress = (pocket.balance.toFloat() / target.toFloat()).coerceIn(0f, 1f)
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = PocketTheme.colors.accent,
                        trackColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                    Text(
                        text = if (pocket.balance >= target) stringResource(R.string.comp_target_tercapai_u2713)
                        else if (isHidden) stringResource(R.string.comp_target)
                        else stringResource(R.string.comp_dari, (progress * 100).toInt(), target.toRupiah()),
                        fontSize = 10.sp,
                        color = PocketTheme.colors.accentText
                    )
                }

                if (isLocked && pocket.maturityDate != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isMatured) stringResource(R.string.comp_jatuh_tempo) else stringResource(R.string.comp_s_d, pocket.maturityDate.toDateFormatted()),
                        fontSize = 10.sp,
                        color = if (isMatured) PocketTheme.colors.successText else PocketTheme.colors.warnText
                    )
                }
            }
        }
    }
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
            containerColor = MaterialTheme.colorScheme.surface
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
                            TxType.DEBIT -> PocketTheme.colors.dangerContainer
                            TxType.CREDIT -> MaterialTheme.colorScheme.secondaryContainer
                            TxType.REALLOCATION -> MaterialTheme.colorScheme.primaryContainer
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
                        TxType.DEBIT -> PocketTheme.colors.danger
                        TxType.CREDIT -> PocketTheme.colors.accent
                        TxType.REALLOCATION -> PocketTheme.colors.brandText
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (tx.status != TxStatus.SUCCESS) {
                    Text(
                        text = statusLabel(tx.status),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (tx.status == TxStatus.REVERSED) PocketTheme.colors.warnText else PocketTheme.colors.amberText
                    )
                }
                Text(
                    text = stringResource(R.string.comp_sumber, tx.sourceOfFund),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val prefix = when (tx.type) {
                TxType.DEBIT -> "- "
                TxType.CREDIT -> "+ "
                TxType.REALLOCATION -> ""
            }
            Text(
                text = if (isHidden) "Rp ••••••" else "$prefix${tx.amount.toRupiah()}",
                textDecoration = if (tx.status == TxStatus.REVERSED) TextDecoration.LineThrough else null,
                fontWeight = FontWeight.ExtraBold,
                color = when (tx.type) {
                    TxType.DEBIT -> PocketTheme.colors.danger
                    TxType.CREDIT -> PocketTheme.colors.accent
                    TxType.REALLOCATION -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
