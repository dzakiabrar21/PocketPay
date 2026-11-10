package com.example.pocketpay.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pocketpay.R
import com.example.pocketpay.data.TxType
import com.example.pocketpay.presentation.security.BiometricHelper
import com.example.pocketpay.ui.components.Avatar
import com.example.pocketpay.ui.components.CircleIcon
import com.example.pocketpay.ui.components.PillButton
import com.example.pocketpay.ui.components.PillStyle
import com.example.pocketpay.ui.components.PocketBackground
import com.example.pocketpay.ui.components.PocketChip
import com.example.pocketpay.ui.components.PocketTextField
import com.example.pocketpay.ui.components.SoftCard
import com.example.pocketpay.ui.components.StatusChip
import com.example.pocketpay.ui.theme.PocketTheme

/** Profile tab: editable details, live security status and account info. */
@Composable
fun ProfileTab(
    name: String,
    phone: String,
    email: String,
    tier: String,
    accountNumber: String,
    biometricAvailable: Boolean,
    onSave: (name: String, phone: String, email: String) -> Unit,
    onChangePin: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var editName by remember(name) { mutableStateOf(name) }
    var editPhone by remember(phone) { mutableStateOf(phone) }
    var editEmail by remember(email) { mutableStateOf(email) }
    val c = PocketTheme.colors

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
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(editName.ifBlank { name }, size = 88.dp)
                Spacer(Modifier.height(12.dp))
                Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                StatusChip(tier, container = c.lime.copy(alpha = 0.25f), content = c.accentText)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.profile_acc_number, accountNumber),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            SoftCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.profile_informasi_diri_kontak), fontWeight = FontWeight.Bold)
                    PocketTextField(editName, { editName = it }, stringResource(R.string.profile_nama_lengkap_nasabah), leadingIcon = Icons.Rounded.Person)
                    PocketTextField(
                        editPhone, { editPhone = it }, stringResource(R.string.profile_nomor_telepon),
                        leadingIcon = Icons.Rounded.Phone, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    PocketTextField(
                        editEmail, { editEmail = it }, stringResource(R.string.profile_email_perbankan),
                        leadingIcon = Icons.Rounded.Email, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    PillButton(
                        stringResource(R.string.profile_simpan_perubahan),
                        { onSave(editName.trim(), editPhone.trim(), editEmail.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = editName.isNotBlank() && (editName != name || editPhone != phone || editEmail != email)
                    )
                }
            }
        }
        item {
            SoftCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.profile_status_keamanan_proteksi), fontWeight = FontWeight.Bold)
                    SecurityRow(Icons.Rounded.Lock, stringResource(R.string.profile_pin_transaksi), stringResource(R.string.sec_active), true)
                    SecurityRow(
                        Icons.Rounded.Fingerprint, stringResource(R.string.profile_biometrik),
                        stringResource(if (biometricAvailable) R.string.sec_active else R.string.sec_unavailable), biometricAvailable
                    )
                    SecurityRow(Icons.Rounded.VisibilityOff, stringResource(R.string.profile_anti_screenshot), stringResource(R.string.sec_active), true)
                    SecurityRow(Icons.Rounded.Timer, stringResource(R.string.profile_session_timeout), stringResource(R.string.sec_auto_lock_value), true)
                    PillButton(
                        stringResource(R.string.profile_ganti_pin), onChangePin,
                        modifier = Modifier.fillMaxWidth(), icon = Icons.Rounded.AdminPanelSettings, style = PillStyle.OUTLINE
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityRow(icon: ImageVector, label: String, value: String, ok: Boolean) {
    val c = PocketTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircleIcon(icon, null, size = 40.dp, container = c.soft, tint = c.brandText)
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        StatusChip(
            value,
            container = if (ok) c.lime.copy(alpha = 0.25f) else c.amberContainer,
            content = if (ok) c.accentText else c.amberText
        )
    }
}

/** Full-screen transaction history with search and filters. */
@Composable
fun HistoryScreen(
    state: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val txs = state.filteredTransactions
    val filtered = state.searchQuery.isNotEmpty() || state.selectedTxTypeFilter != null || state.dateRangeFilter != DateRangeFilter.ALL

    PocketBackground(modifier) { Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIcon(
                Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.profile_kembali),
                container = MaterialTheme.colorScheme.surface, onClick = onBack
            )
            Spacer(Modifier.width(14.dp))
            Text(stringResource(R.string.home_mutasi_transaksi), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PocketTextField(
                    state.searchQuery, { onIntent(HomeUiIntent.SetSearchQuery(it)) },
                    stringResource(R.string.history_search), leadingIcon = Icons.Rounded.Search
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { PocketChip(state.selectedTxTypeFilter == null, { onIntent(HomeUiIntent.SetTxTypeFilter(null)) }, stringResource(R.string.home_semua_tipe)) }
                    items(TxType.entries.toList()) { type ->
                        PocketChip(state.selectedTxTypeFilter == type, { onIntent(HomeUiIntent.SetTxTypeFilter(type)) }, txTypeLabel(type))
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DateRangeFilter.entries.toList()) { f ->
                        PocketChip(state.dateRangeFilter == f, { onIntent(HomeUiIntent.SetDateRangeFilter(f)) }, stringResource(f.labelRes))
                    }
                }
            }
            item {
                SoftCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    if (txs.isEmpty()) {
                        Text(
                            stringResource(if (filtered) R.string.home_tidak_ada_transaksi_yang_cocok_denga else R.string.home_belum_ada_riwayat_transaksi),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    } else {
                        Column {
                            txs.forEachIndexed { i, tx ->
                                if (i > 0) HorizontalDivider(color = listDividerColor())
                                TransactionItemRow(tx, isHidden = state.isBalanceHidden, onClick = { onIntent(HomeUiIntent.SelectTransactionForDetail(tx)) })
                            }
                        }
                    }
                }
            }
        }
    }
} }
