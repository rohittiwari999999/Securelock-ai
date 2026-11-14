package com.securelock.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securelock.ai.data.AppThemeMode

private val NavyLightCardColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = CrispWhite,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = DeepNavy,
    secondary = SkyAccent,
    onSecondary = DeepNavy,
    background = DeepNavy,
    onBackground = CrispWhite,
    surface = CrispWhite,
    onSurface = DeepNavy,
    surfaceVariant = CoolCanvas,
    onSurfaceVariant = SlateText,
    outline = SlateBorder,
    error = CrimsonAlert,
    onError = CrispWhite
)

private val NavyDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = CrispWhite,
    primaryContainer = MidnightBlue,
    onPrimaryContainer = SkyAccent,
    secondary = SkyAccent,
    onSecondary = DeepNavy,
    background = DeepNavy,
    onBackground = CrispWhite,
    surface = MidnightBlue,
    onSurface = CrispWhite,
    surfaceVariant = NavySurface,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF1E3A5F),
    error = CrimsonAlert,
    onError = CrispWhite
)

private val SecureTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

private val SecureShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun SecureLockTheme(
    themeMode: AppThemeMode = AppThemeMode.NAVY_LIGHT_CARDS,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.NAVY_LIGHT_CARDS -> NavyLightCardColorScheme
        AppThemeMode.NAVY_DARK -> NavyDarkColorScheme
        AppThemeMode.SYSTEM -> if (isSystemInDarkTheme()) NavyDarkColorScheme else NavyLightCardColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SecureTypography,
        shapes = SecureShapes,
        content = content
    )
}
