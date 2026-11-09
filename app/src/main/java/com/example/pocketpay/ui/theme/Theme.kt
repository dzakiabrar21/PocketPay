package com.example.pocketpay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldAccent,
    secondary = MintLightContainer,
    tertiary = IncomeGreen,
    background = Color(0xFF064E3B),
    surface = Color(0xFF065F46)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldGreenPrimary,
    secondary = EmeraldAccent,
    tertiary = IncomeGreen,
    primaryContainer = MintLightContainer,
    background = BackgroundSoft,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun PocketPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}