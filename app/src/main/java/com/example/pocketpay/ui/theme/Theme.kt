package com.example.pocketpay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF047857),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0F2A22),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF052E22),
    secondaryContainer = Color(0xFF134E3A),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = IncomeGreen,
    background = Color(0xFF0B1411),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111C18),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1B2A25),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF0B1411),
    surfaceContainerLow = Color(0xFF0F1A16),
    surfaceContainer = Color(0xFF14211C),
    surfaceContainerHigh = Color(0xFF192822),
    surfaceContainerHighest = Color(0xFF1F302A),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF2A3A34),
    error = Color(0xFFF87171),
    onError = Color(0xFF3F1517)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldGreenPrimary,
    secondary = EmeraldAccent,
    tertiary = IncomeGreen,
    primaryContainer = MintLightContainer,
    onPrimaryContainer = EmeraldGreenPrimary,
    secondaryContainer = IncomeGreenContainer,
    onSecondaryContainer = EmeraldGreenPrimary,
    background = BackgroundSoft,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
    onSurfaceVariant = TextMuted
)

@Composable
fun PocketPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalPocketColors provides if (darkTheme) DarkPocketColors else LightPocketColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
