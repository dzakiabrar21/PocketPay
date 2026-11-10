package com.example.pocketpay.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, spacing: Double = 0.0) =
    TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = spacing.sp
    )

val Typography = Typography(
    headlineSmall = style(24, 32, FontWeight.Bold),
    titleLarge = style(22, 28, FontWeight.Bold),
    titleMedium = style(16, 24, FontWeight.SemiBold, 0.15),
    titleSmall = style(14, 20, FontWeight.SemiBold, 0.1),
    bodyLarge = style(16, 24, FontWeight.Normal, 0.5),
    bodyMedium = style(14, 20, FontWeight.Normal, 0.25),
    bodySmall = style(12, 16, FontWeight.Normal, 0.4),
    labelMedium = style(12, 16, FontWeight.Medium, 0.5),
    labelSmall = style(11, 16, FontWeight.Medium, 0.5)
)
