package com.example.pocketpay.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colours that Material's colour scheme has no slot for. Screens ask for a role
 * ("hero card", "danger text") instead of a hex value, so light and dark stay legible.
 */
@Immutable
data class PocketColors(
    // Dark "hero" surfaces (balance card, scanner, success header) and what sits on them
    val hero: Color,
    val heroAlt: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val heroChip: Color,
    // Signature lime accent
    val lime: Color,
    val onLime: Color,
    // Neutral soft tile behind icons and charts
    val soft: Color,
    val border: Color,
    // Text/icon roles on normal surfaces
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
    hero = Color(0xFF0E1A2B),
    heroAlt = Color(0xFF1A2E4D),
    onHero = Color.White,
    onHeroMuted = Color(0xFFA3B3CC),
    heroChip = Color(0xFF1F3049),
    lime = Color(0xFF8CD63F),
    onLime = Color(0xFF0E1A2B),
    soft = Color(0xFFEBF2E4),
    border = Color(0xFFE1E8DA),
    brandText = Color(0xFF0E1A2B),
    accentText = Color(0xFF3F7F12),
    accent = Color(0xFF2F9E44),
    successText = Color(0xFF2F9E44),
    warnContainer = Color(0xFFFFF4E5),
    warnChip = Color(0xFFFFE4BF),
    warnText = Color(0xFFB45309),
    amberContainer = Color(0xFFFEF3C7),
    amberIcon = Color(0xFFD97706),
    amberText = Color(0xFF92400E),
    dangerContainer = Color(0xFFFDE8E8),
    danger = Color(0xFFE5484D)
)

val DarkPocketColors = PocketColors(
    hero = Color(0xFF16253D),
    heroAlt = Color(0xFF22406B),
    onHero = Color.White,
    onHeroMuted = Color(0xFF9FB1CD),
    heroChip = Color(0xFF263A59),
    lime = Color(0xFF9BE34B),
    onLime = Color(0xFF0A1220),
    soft = Color(0xFF18263C),
    border = Color(0xFF22324C),
    brandText = Color(0xFFE7ECF5),
    accentText = Color(0xFFA5E86B),
    accent = Color(0xFF5ED67A),
    successText = Color(0xFF5ED67A),
    warnContainer = Color(0xFF2B1D0E),
    warnChip = Color(0xFF4A2F12),
    warnText = Color(0xFFFDBA74),
    amberContainer = Color(0xFF3A2C0A),
    amberIcon = Color(0xFFFBBF24),
    amberText = Color(0xFFFCD34D),
    dangerContainer = Color(0xFF3A1719),
    danger = Color(0xFFFF7B7F)
)

val LocalPocketColors = staticCompositionLocalOf { LightPocketColors }

object PocketTheme {
    val colors: PocketColors
        @Composable @ReadOnlyComposable get() = LocalPocketColors.current
}
