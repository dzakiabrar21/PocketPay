package com.example.pocketpay

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pocketpay.data.AccountEntity
import com.example.pocketpay.data.PocketEntity
import com.example.pocketpay.data.PocketType
import com.example.pocketpay.data.TransactionEntity
import com.example.pocketpay.data.TxStatus
import com.example.pocketpay.data.TxType
import com.example.pocketpay.presentation.home.CreatePocketSheet
import com.example.pocketpay.presentation.home.EStatementSheet
import com.example.pocketpay.presentation.home.HistoryScreen
import com.example.pocketpay.presentation.home.HomeTab
import com.example.pocketpay.presentation.home.HomeUiState
import com.example.pocketpay.presentation.home.ManagePocketSheet
import com.example.pocketpay.presentation.home.PinEntryScreen
import com.example.pocketpay.presentation.home.PocketsTab
import com.example.pocketpay.presentation.home.ProfileTab
import com.example.pocketpay.presentation.home.QrisPaymentScreen
import com.example.pocketpay.presentation.home.ReceiptSheet
import com.example.pocketpay.presentation.home.ReceiveQrScreen
import com.example.pocketpay.presentation.home.SplashScreen
import com.example.pocketpay.presentation.home.StatsTab
import com.example.pocketpay.presentation.home.SuccessAlertData
import com.example.pocketpay.presentation.home.SuccessScreen
import com.example.pocketpay.presentation.home.TransferSheet
import com.example.pocketpay.ui.components.FloatingBottomBar
import com.example.pocketpay.ui.components.NavItem
import com.example.pocketpay.ui.components.PocketBackground
import com.example.pocketpay.ui.theme.PocketPayTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Renders every redesigned screen in light and dark and writes PNGs to the app's files dir.
 * The real window uses FLAG_SECURE (system screenshots are black); capturing the composable
 * itself bypasses that, which makes visual review possible.
 */
@RunWith(AndroidJUnit4::class)
class ThemeScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    private val day = 24L * 3600 * 1000
    private val now = System.currentTimeMillis()
    private val pockets = listOf(
        PocketEntity("a", "Jajan Harian", 350_000, PocketType.SPENDING, 3.5, targetAmount = 500_000),
        PocketEntity("b", "Deposito Darurat", 2_000_000, PocketType.LOCKED_DEPOSIT, 6.0, 3, now + 90 * day),
        PocketEntity("c", "Celengan Receh", 24_500, PocketType.SPENDING, 3.5)
    )
    private val txs = listOf(
        TransactionEntity("1", "QRIS - Kantin Mahasiswa", 23_500, "Rekening Utama", TxType.DEBIT, timestamp = now),
        TransactionEntity("2", "Bunga Jajan Harian (30 hari)", 287, "Bunga 3.5% p.a.", TxType.CREDIT, timestamp = now - day),
        TransactionEntity("3", "Transfer: Jajan > Belanja", 20_000, "Kantong: Jajan", TxType.REALLOCATION, timestamp = now - 2 * day),
        TransactionEntity("4", "QRIS - Warung Bu Sari", 48_000, "Kantong: Jajan", TxType.DEBIT, timestamp = now - 3 * day),
        TransactionEntity("5", "Refund: QRIS - Toko Buku", 150_000, "Pengembalian", TxType.CREDIT, timestamp = now - 4 * day),
        TransactionEntity("6", "QRIS - Toko Buku", 150_000, "Rekening Utama", TxType.DEBIT, TxStatus.REVERSED, timestamp = now - 4 * day),
        TransactionEntity("7", "QRIS - Kopi Senja", 32_000, "Rekening Utama", TxType.DEBIT, timestamp = now - 5 * day)
    )
    private val state = HomeUiState(
        isLoading = false, account = AccountEntity(balance = 4_998_500L), pockets = pockets,
        recentTransactions = txs, userName = "Dzaki Abrar", userPhone = "0812-3456-7890", userEmail = "dzaki@pocketpay.com"
    )

    private fun save(name: String) {
        val bmp: Bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "$name.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 0)
    }

    /** Renders [content] in light, captures, flips to dark, captures again. */
    private fun shotBoth(name: String, content: @Composable () -> Unit) {
        val dark = mutableStateOf(false)
        rule.setContent { PocketPayTheme(darkTheme = dark.value) { content() } }
        rule.waitForIdle(); save("${name}_light")
        dark.value = true
        rule.waitForIdle(); save("${name}_dark")
    }

    @Composable
    private fun Shell(tab: Int, body: @Composable (PaddingValues) -> Unit) {
        PocketBackground {
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
                body(PaddingValues(bottom = 104.dp))
                FloatingBottomBar(
                    listOf(
                        NavItem("Home", Icons.Rounded.Home), NavItem("Statistic", Icons.Rounded.BarChart),
                        NavItem("Pockets", Icons.Rounded.Savings), NavItem("Profile", Icons.Rounded.Person)
                    ), tab, {}, Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                )
            }
        }
    }

    @Test fun home() = shotBoth("home") { Shell(0) { HomeTab(state, {}, {}, {}, {}, Modifier.fillMaxSize(), it) } }
    @Test fun stats() = shotBoth("stats") { Shell(1) { StatsTab(state, {}, Modifier.fillMaxSize(), it, now) } }
    @Test fun pocketsTab() = shotBoth("pockets") { Shell(2) { PocketsTab(state, {}, Modifier.fillMaxSize(), it) } }
    @Test fun profile() = shotBoth("profile") {
        Shell(3) { ProfileTab("Dzaki Abrar", "0812-3456-7890", "dzaki@pocketpay.com", "Gold Premier Member", "9012839102", true, { _, _, _ -> }, {}, Modifier.fillMaxSize(), it) }
    }
    @Test fun history() = shotBoth("history") { HistoryScreen(state, {}, {}) }
    @Test fun splash() = shotBoth("splash") { SplashScreen({}, holdMillis = 600_000) }
    @Test fun pin() = shotBoth("pin") { PinEntryScreen("Enter PIN", "The session locked automatically. Enter your PIN to continue.", error = "Wrong PIN. Attempts left: 3", onComplete = {}) }
    @Test fun pay() = shotBoth("pay") { QrisPaymentScreen("Kantin Mahasiswa PocketPay", 23_500, pockets.filter { it.pocketType == PocketType.SPENDING }, 4_998_500, {}, { _, _, _, _ -> }) }
    @Test fun success() = shotBoth("success") {
        SuccessScreen(
            SuccessAlertData(
                "Payment success!", "", 23_500L,
                listOf("Merchant" to "Kantin Mahasiswa PocketPay", "Source of funds" to "Rekening Utama", "Celengan Receh" to "Rp1.500", "Reference" to "76367D01", "Time" to "11 Nov 2026, 14:03:22")
            ), {}
        )
    }
    @Test fun receive() = shotBoth("receive") { ReceiveQrScreen("9012839102", "Dzaki Abrar", {}) }

    @Test fun sheets() = shotBoth("sheets") {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
                CreatePocketSheet { _, _, _, _, _ -> }
            }
        }
    }
    @Test fun manage() = shotBoth("manage") { Box(Modifier.fillMaxSize().statusBarsPadding()) { ManagePocketSheet(pockets[0], { _, _ -> }, {}) } }
    @Test fun receipt() = shotBoth("receipt") { Box(Modifier.fillMaxSize().statusBarsPadding()) { ReceiptSheet(txs[0], {}) } }
    @Test fun transfer() = shotBoth("transfer") { Box(Modifier.fillMaxSize().statusBarsPadding()) { TransferSheet(pockets.filter { it.pocketType == PocketType.SPENDING }) { _, _, _ -> } } }
    @Test fun statement() = shotBoth("statement") { Box(Modifier.fillMaxSize().statusBarsPadding()) { EStatementSheet("9012839102", 4_998_500, txs) } }
}
