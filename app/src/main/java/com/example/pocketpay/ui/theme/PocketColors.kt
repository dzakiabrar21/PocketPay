package com.example.pocketpay.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colours that Material's colour scheme has no slot for. Every screen asks for a role
 * ("warning chip", "danger text") instead of a hex value, so light and dark both stay legible.
 */
@Immutable
data class PocketColors(
    /** Brand green used for text/icons that sit on a surface (not on the green hero). */
    val brandText: Color,
    val accentText: Color,
    val accent: Color,
    val successText: Color,
    val warnContainer: Color,
    val warnChip: Color,
    val warnText: Color,
    val amberContainer: Color,
    val amberIcon: Color,
    val amberText: Color,
    val dangerContainer: Color,
    val danger: Color
)

val LightPocketColors = PocketColors(
    brandText = Color(0xFF064E3B),
    accentText = Color(0xFF047857),
    accent = Color(0xFF059669),
    successText = Color(0xFF15803D),
    warnContainer = Color(0xFFFFF7ED),
    warnChip = Color(0xFFFFEDD5),
    warnText = Color(0xFFC2410C),
    amberContainer = Color(0xFFFEF3C7),
    amberIcon = Color(0xFFD97706),
    amberText = Color(0xFF92400E),
    dangerContainer = Color(0xFFFEE2E2),
    danger = Color(0xFFDC2626)
)

val DarkPocketColors = PocketColors(
    brandText = Color(0xFFA7F3D0),
    accentText = Color(0xFF6EE7B7),
    accent = Color(0xFF34D399),
    successText = Color(0xFF4ADE80),
    warnContainer = Color(0xFF2B1B10),
    warnChip = Color(0xFF4A2A12),
    warnText = Color(0xFFFDBA74),
    amberContainer = Color(0xFF3A2C0A),
    amberIcon = Color(0xFFFBBF24),
    amberText = Color(0xFFFCD34D),
    dangerContainer = Color(0xFF3F1517),
    danger = Color(0xFFF87171)
)

val LocalPocketColors = staticCompositionLocalOf { LightPocketColors }

object PocketTheme {
    val colors: PocketColors
        @Composable @ReadOnlyComposable get() = LocalPocketColors.current
}
