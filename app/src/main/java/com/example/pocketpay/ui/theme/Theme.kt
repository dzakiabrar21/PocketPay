package com.example.pocketpay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9BE34B),
    onPrimary = Color(0xFF0A1220),
    primaryContainer = Color(0xFF223A14),
    onPrimaryContainer = Color(0xFFC6F29C),
    secondary = Color(0xFFE7ECF5),
    onSecondary = Color(0xFF0A1220),
    secondaryContainer = Color(0xFF1C2A42),
    onSecondaryContainer = Color(0xFFE7ECF5),
    tertiary = Color(0xFF5ED67A),
    background = Color(0xFF0A1220),
    onBackground = Color(0xFFE7ECF5),
    surface = Color(0xFF111C30),
    onSurface = Color(0xFFE7ECF5),
    surfaceVariant = Color(0xFF18263C),
    onSurfaceVariant = Color(0xFF8FA0BA),
    surfaceContainerLowest = Color(0xFF0A1220),
    surfaceContainerLow = Color(0xFF0F192B),
    surfaceContainer = Color(0xFF131F34),
    surfaceContainerHigh = Color(0xFF18263C),
    surfaceContainerHighest = Color(0xFF1E2E48),
    outline = Color(0xFF34476A),
    outlineVariant = Color(0xFF22324C),
    error = Color(0xFFFF7B7F),
    onError = Color(0xFF3A1719)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8CD63F),
    onPrimary = Color(0xFF0E1A2B),
    primaryContainer = Color(0xFFE4F5D3),
    onPrimaryContainer = Color(0xFF1F3A08),
    secondary = Color(0xFF0E1A2B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EEF7),
    onSecondaryContainer = Color(0xFF0E1A2B),
    tertiary = Color(0xFF2F9E44),
    background = Color(0xFFF4F8F1),
    onBackground = Color(0xFF0E1A2B),
    surface = Color.White,
    onSurface = Color(0xFF0E1A2B),
    surfaceVariant = Color(0xFFEEF3EA),
    onSurfaceVariant = Color(0xFF6B7A90),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FBF7),
    surfaceContainer = Color(0xFFF1F6EC),
    surfaceContainerHigh = Color(0xFFEDF3E8),
    surfaceContainerHighest = Color(0xFFE7EEE1),
    outline = Color(0xFFD3DCE8),
    outlineVariant = Color(0xFFE6ECF4),
    error = Color(0xFFE5484D),
    onError = Color.White
)

/** Larger, friendlier corners than Material's defaults, matching the pill/card look of the design. */
val PocketShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
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
            shapes = PocketShapes,
            content = content
        )
    }
}
