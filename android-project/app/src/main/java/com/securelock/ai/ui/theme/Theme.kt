package com.securelock.ai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securelock.ai.data.AppThemeId
import com.securelock.ai.data.CardStyleMode

val LocalCardStyleMode = staticCompositionLocalOf { CardStyleMode.ELEVATED }
val LocalThemePalette = staticCompositionLocalOf { paletteForTheme(AppThemeId.MIDNIGHT_BLUE) }

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
    themeId: AppThemeId = AppThemeId.MIDNIGHT_BLUE,
    customAccentHex: Long? = null,
    forceLightMode: Boolean? = null,
    cardStyleMode: CardStyleMode = CardStyleMode.ELEVATED,
    content: @Composable () -> Unit
) {
    val palette = paletteForTheme(themeId)
    val accent = customAccentHex?.let { Color(it) } ?: palette.primaryAccent
    val useDarkCards = when (forceLightMode) {
        true -> false
        false -> true
        null -> palette.defaultDarkCards
    }
    val isPureLightBackground = themeId == AppThemeId.PURE_WHITE && forceLightMode != false
    val bgColor = if (forceLightMode == true && themeId != AppThemeId.PURE_WHITE) {
        palette.background
    } else if (isPureLightBackground) {
        Color(0xFFF1F5F9)
    } else {
        palette.background
    }

    val colorScheme = if (useDarkCards) {
        darkColorScheme(
            primary = accent,
            onPrimary = CrispWhite,
            primaryContainer = palette.surfaceVariant,
            onPrimaryContainer = palette.secondaryAccent,
            secondary = palette.secondaryAccent,
            onSecondary = DeepNavy,
            background = bgColor,
            onBackground = if (isPureLightBackground) DeepNavy else CrispWhite,
            surface = palette.cardDark,
            onSurface = CrispWhite,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF283A5A),
            error = CrimsonAlert,
            onError = CrispWhite
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = CrispWhite,
            primaryContainer = accent.copy(alpha = 0.12f),
            onPrimaryContainer = DeepNavy,
            secondary = palette.secondaryAccent,
            onSecondary = DeepNavy,
            background = bgColor,
            onBackground = if (isPureLightBackground) DeepNavy else CrispWhite,
            surface = palette.cardLight,
            onSurface = DeepNavy,
            surfaceVariant = CoolCanvas,
            onSurfaceVariant = SlateText,
            outline = SlateBorder,
            error = CrimsonAlert,
            onError = CrispWhite
        )
    }

    CompositionLocalProvider(
        LocalCardStyleMode provides cardStyleMode,
        LocalThemePalette provides palette.copy(primaryAccent = accent)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SecureTypography,
            shapes = SecureShapes,
            content = content
        )
    }
}
