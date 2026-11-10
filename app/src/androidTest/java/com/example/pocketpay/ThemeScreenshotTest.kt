package com.example.pocketpay

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pocketpay.data.PocketEntity
import com.example.pocketpay.data.PocketType
import com.example.pocketpay.data.TransactionEntity
import com.example.pocketpay.data.TxType
import com.example.pocketpay.presentation.home.AccountProfileScreen
import com.example.pocketpay.presentation.home.PocketCardItem
import com.example.pocketpay.presentation.home.TransactionItemRow
import com.example.pocketpay.ui.theme.PocketPayTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Renders key screens in light and dark and writes PNGs to the app's files dir.
 * The app window uses FLAG_SECURE (so system screenshots are black); capturing the composable
 * itself bypasses that, which makes visual review possible.
 */
@RunWith(AndroidJUnit4::class)
class ThemeScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    private val now = System.currentTimeMillis()
    private val spending = PocketEntity("a", "Jajan Harian", 350_000, PocketType.SPENDING, 3.5, targetAmount = 500_000)
    private val locked = PocketEntity("b", "Deposito Darurat", 2_000_000, PocketType.LOCKED_DEPOSIT, 6.0, 3, now + 90L * 86_400_000)
    private val txs = listOf(
        TransactionEntity("1", "QRIS - Kantin", 23_500, "Rekening Utama", TxType.DEBIT),
        TransactionEntity("2", "Bunga Jajan Harian (30 hari)", 287, "Bunga 3.5% p.a.", TxType.CREDIT),
        TransactionEntity("3", "Transfer: Jajan > Belanja", 20_000, "Kantong: Jajan", TxType.REALLOCATION)
    )

    @Composable
    private fun Gallery() {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PocketCardItem(pocket = spending) {}
            PocketCardItem(pocket = locked) {}
            txs.forEach { TransactionItemRow(tx = it) }
        }
    }

    private fun shot(name: String) {
        val bmp: Bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
        val file = File(dir, "$name.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 0)
    }

    private fun gallery(dark: Boolean, label: String) {
        rule.setContent { PocketPayTheme(darkTheme = dark) { Gallery() } }
        rule.waitForIdle()
        shot("gallery_$label")
    }

    private fun profile(dark: Boolean, label: String) {
        rule.setContent {
            PocketPayTheme(darkTheme = dark) {
                AccountProfileScreen("Budi Santoso", "0812-3456-7890", "budi@pocketpay.com",
                    "Gold Premier Member", "9012839102", {}, { _, _, _ -> })
            }
        }
        rule.waitForIdle()
        shot("profile_$label")
    }

    @Test fun gallery_light() = gallery(false, "light")
    @Test fun gallery_dark() = gallery(true, "dark")
    @Test fun profile_light() = profile(false, "light")
    @Test fun profile_dark() = profile(true, "dark")
}
