package com.example.pocketpay.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, spacing: Double = 0.0) =
    TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = spacing.sp
    )

/** Bold, generous headings with calm body text, as in the reference design. */
val Typography = Typography(
    displayMedium = style(44, 52, FontWeight.Bold, -0.5),
    displaySmall = style(36, 44, FontWeight.Bold, -0.25),
    headlineLarge = style(30, 38, FontWeight.Bold),
    headlineMedium = style(26, 34, FontWeight.Bold),
    headlineSmall = style(22, 30, FontWeight.Bold),
    titleLarge = style(20, 28, FontWeight.Bold),
    titleMedium = style(16, 24, FontWeight.SemiBold, 0.1),
    titleSmall = style(14, 20, FontWeight.SemiBold, 0.1),
    bodyLarge = style(16, 24, FontWeight.Normal, 0.2),
    bodyMedium = style(14, 20, FontWeight.Normal, 0.2),
    bodySmall = style(12, 16, FontWeight.Normal, 0.3),
    labelLarge = style(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = style(12, 16, FontWeight.Medium, 0.4),
    labelSmall = style(11, 16, FontWeight.Medium, 0.4)
)
