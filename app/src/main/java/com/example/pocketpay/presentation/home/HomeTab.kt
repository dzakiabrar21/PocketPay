package com.example.pocketpay.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketpay.R
import com.example.pocketpay.ui.components.Avatar
import com.example.pocketpay.ui.components.CircleIcon
import com.example.pocketpay.ui.components.PillButton
import com.example.pocketpay.ui.components.PillStyle
import com.example.pocketpay.ui.components.SectionHeader
import com.example.pocketpay.ui.components.SoftCard
import com.example.pocketpay.ui.theme.PocketTheme
import java.util.Calendar

@Composable
private fun greeting(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String = stringResource(
    when (hour) {
        in 4..10 -> R.string.greeting_morning
        in 11..14 -> R.string.greeting_afternoon
        in 15..17 -> R.string.greeting_evening
        else -> R.string.greeting_night
    }
)

/** Dashboard: greeting, balance hero with quick actions, pockets and recent activity. */
@Composable
fun HomeTab(
    state: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
    onViewAllPockets: () -> Unit,
    onViewAllHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val celengan = state.pockets.firstOrNull { it.name.equals("Celengan Receh", ignoreCase = true) }?.balance ?: 0L

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(state.userName, size = 46.dp, modifier = Modifier.clip(CircleShape).clickable(onClick = onOpenProfile))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(greeting(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                CircleIcon(
                    Icons.Rounded.PictureAsPdf, stringResource(R.string.home_export_e_statement_pdf),
                    container = MaterialTheme.colorScheme.surface, tint = PocketTheme.colors.brandText,
                    onClick = { onIntent(HomeUiIntent.SetEStatementDialogVisibility(true)) }
                )
            }
        }

        item { HeroBalanceCard(state, onIntent) }

        item {
            SoftCard(onClick = onViewAllPockets) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleIcon(Icons.Rounded.Savings, null, container = PocketTheme.colors.lime, tint = PocketTheme.colors.onLime, size = 46.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.home_celengan_receh_round_up), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.home_celengan_collected, formatAmount(celengan, state.isBalanceHidden)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(
                    stringResource(R.string.home_kantong_finansial),
                    actionText = stringResource(R.string.view_all), onAction = onViewAllPockets
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.pockets, key = { it.id }) { pocket ->
                        PocketCardItem(pocket, isHidden = state.isBalanceHidden, fixedHeight = 164.dp, onClick = { onIntent(HomeUiIntent.SelectPocketForAction(pocket)) })
                    }
                    item {
                        Box(
                            Modifier
                                .width(96.dp)
                                .height(164.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onIntent(HomeUiIntent.SetCreatePocketDialogVisibility(true)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircleIcon(Icons.Rounded.Add, null, container = PocketTheme.colors.soft, size = 40.dp)
                                Spacer(Modifier.height(6.dp))
                                Text(stringResource(R.string.home_buat_kantong), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(
                    stringResource(R.string.home_mutasi_transaksi),
                    actionText = stringResource(R.string.view_all), onAction = onViewAllHistory
                )
                val latest = state.recentTransactions.take(5)
                SoftCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    if (latest.isEmpty()) {
                        Text(
                            stringResource(R.string.home_belum_ada_riwayat_transaksi),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 20.dp).fillMaxWidth()
                        )
                    } else {
                        Column {
                            latest.forEachIndexed { i, tx ->
                                if (i > 0) HorizontalDivider(color = listDividerColor())
                                TransactionItemRow(tx, isHidden = state.isBalanceHidden, onClick = { onIntent(HomeUiIntent.SelectTransactionForDetail(tx)) })
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Dark balance card with account chip, visibility toggle and the four main actions. */
@Composable
fun HeroBalanceCard(state: HomeUiState, onIntent: (HomeUiIntent) -> Unit, modifier: Modifier = Modifier) {
    val c = PocketTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(c.hero, c.heroAlt)))
            .drawBehind {
                drawCircle(c.lime.copy(alpha = 0.10f), radius = 150.dp.toPx(), center = Offset(size.width, 0f))
                drawCircle(c.lime.copy(alpha = 0.06f), radius = 90.dp.toPx(), center = Offset(size.width * 0.1f, size.height))
            }
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(CircleShape).background(c.heroChip).padding(horizontal = 12.dp, vertical = 7.dp)) {
                    Text(
                        "${stringResource(R.string.home_rekening_utama_short)} • ${state.account?.accountNumber ?: "-"}",
                        color = c.onHero, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1
                    )
                }
                Spacer(Modifier.weight(1f))
                CircleIcon(
                    if (state.isBalanceHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    stringResource(R.string.home_samarkan_saldo),
                    container = c.heroChip, tint = c.onHero, size = 38.dp,
                    onClick = { onIntent(HomeUiIntent.ToggleBalanceVisibility) }
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.home_available_balance), color = c.onHeroMuted, style = MaterialTheme.typography.bodySmall)
            Text(
                formatAmount(state.account?.balance ?: 0L, state.isBalanceHidden),
                color = c.onHero, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleIcon(
                    Icons.Rounded.QrCodeScanner, stringResource(R.string.home_scan_qris),
                    container = c.lime, tint = c.onLime, size = 52.dp,
                    onClick = { onIntent(HomeUiIntent.SetScannerVisibility(true)) }
                )
                PillButton(
                    stringResource(R.string.action_receive), { onIntent(HomeUiIntent.SetReceiveQrDialogVisibility(true)) },
                    modifier = Modifier.weight(1f), icon = Icons.Rounded.QrCode2, style = PillStyle.SOFT_ON_HERO, height = 52.dp
                )
                PillButton(
                    stringResource(R.string.action_transfer), { onIntent(HomeUiIntent.SetPocketTransferDialogVisibility(true)) },
                    modifier = Modifier.weight(1f), icon = Icons.Rounded.SwapHoriz, style = PillStyle.SOFT_ON_HERO, height = 52.dp
                )
            }
        }
    }
}
