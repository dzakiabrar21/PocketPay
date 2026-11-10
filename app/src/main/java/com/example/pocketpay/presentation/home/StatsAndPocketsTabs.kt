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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketpay.R
import com.example.pocketpay.data.PocketType
import com.example.pocketpay.domain.stats.Statistics
import com.example.pocketpay.domain.stats.StatsPeriod
import com.example.pocketpay.ui.components.BarChart
import com.example.pocketpay.ui.components.PillButton
import com.example.pocketpay.ui.components.SectionHeader
import com.example.pocketpay.ui.components.SoftCard
import com.example.pocketpay.ui.theme.PocketTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun TabTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        trailing()
    }
}

/** Segmented two-option switch (Weekly / Monthly). */
@Composable
private fun PeriodSwitch(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    val c = PocketTheme.colors
    Row(
        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(StatsPeriod.WEEKLY to R.string.stats_weekly, StatsPeriod.MONTHLY to R.string.stats_monthly).forEach { (p, label) ->
            val on = p == selected
            Box(
                Modifier.clip(CircleShape).background(if (on) c.lime else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(p) }.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    stringResource(label), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = if (on) c.onLime else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Income/expense overview with a weekly or monthly bar chart, built from the real transactions. */
@Composable
fun StatsTab(
    state: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    now: Long = System.currentTimeMillis()
) {
    var period by rememberSaveable { mutableStateOf(StatsPeriod.WEEKLY) }
    val c = PocketTheme.colors
    val buckets = remember(state.recentTransactions, period, now) {
        if (period == StatsPeriod.WEEKLY) Statistics.weekly(state.recentTransactions, now)
        else Statistics.monthly(state.recentTransactions, now)
    }
    val labels = remember(buckets, period) {
        val fmt = SimpleDateFormat(if (period == StatsPeriod.WEEKLY) "EEE" else "MMM", Locale.getDefault())
        buckets.map { fmt.format(Date(it.startMillis)) }
    }
    val expenses = remember(state.recentTransactions) { Statistics.recentExpenses(state.recentTransactions, 5) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { TabTitle(stringResource(R.string.stats_title)) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.stats_current_balance), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        formatAmount(state.account?.balance ?: 0L, state.isBalanceHidden),
                        fontSize = 32.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
                PeriodSwitch(period) { period = it }
            }
        }
        item {
            SoftCard(contentPadding = PaddingValues(16.dp)) {
                BarChart(
                    values = buckets.map { it.expense + it.income },
                    labels = labels,
                    highlight = buckets.lastIndex
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(Icons.Rounded.ArrowDownward, stringResource(R.string.stats_income), Statistics.totalIncome(buckets), c.accent, Modifier.weight(1f), state.isBalanceHidden)
                StatTile(Icons.Rounded.ArrowUpward, stringResource(R.string.stats_expense), Statistics.totalExpense(buckets), c.danger, Modifier.weight(1f), state.isBalanceHidden)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(stringResource(R.string.stats_recent_expenses))
                SoftCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    if (expenses.isEmpty()) {
                        Text(stringResource(R.string.stats_no_expenses), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp))
                    } else {
                        Column {
                            expenses.forEachIndexed { i, tx ->
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

@Composable
private fun StatTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    amount: Long,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    hidden: Boolean
) {
    SoftCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatAmount(amount, hidden), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 1)
        }
    }
}

/** All pockets as large cards, with the total and an add button. */
@Composable
fun PocketsTab(
    state: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val total = state.pockets.sumOf { it.balance }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TabTitle(stringResource(R.string.pockets_title)) {
                PillButton(
                    stringResource(R.string.pockets_add), { onIntent(HomeUiIntent.SetCreatePocketDialogVisibility(true)) },
                    icon = Icons.Rounded.Add, height = 42.dp
                )
            }
        }
        item {
            Column {
                Text(stringResource(R.string.pockets_total), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Text(formatAmount(total, state.isBalanceHidden), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        if (state.pockets.isEmpty()) {
            item { Text(stringResource(R.string.pockets_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(state.pockets, key = { it.id }) { pocket ->
            PocketCardItem(
                pocket, modifier = Modifier.fillMaxWidth(), isHidden = state.isBalanceHidden,
                onClick = { onIntent(HomeUiIntent.SelectPocketForAction(pocket)) }
            )
        }
        item {
            val spendingCount = state.pockets.count { it.pocketType == PocketType.SPENDING }
            if (spendingCount < 2) {
                Text(
                    stringResource(R.string.pockets_hint_transfer),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
