package com.securelock.ai.ui.theme

import androidx.compose.ui.graphics.Color
import com.securelock.ai.data.AppThemeId

val DeepNavy = Color(0xFF0A1628)
val MidnightBlue = Color(0xFF112240)
val NavySurface = Color(0xFF162B4D)
val ElectricBlue = Color(0xFF2563EB)
val SkyAccent = Color(0xFF38BDF8)
val CrispWhite = Color(0xFFFFFFFF)
val CoolCanvas = Color(0xFFF8FAFC)
val SlateBorder = Color(0xFFE2E8F0)
val SlateText = Color(0xFF475569)
val EmeraldSafe = Color(0xFF16A34A)
val AmberWarn = Color(0xFFD97706)
val CrimsonAlert = Color(0xFFDC2626)

data class ThemePaletteSpec(
    val id: AppThemeId,
    val background: Color,
    val surfaceVariant: Color,
    val cardLight: Color,
    val cardDark: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val defaultDarkCards: Boolean
)

fun paletteForTheme(id: AppThemeId): ThemePaletteSpec = when (id) {
    AppThemeId.MIDNIGHT_BLUE -> ThemePaletteSpec(
        id = id,
        background = Color(0xFF0A1628),
        surfaceVariant = Color(0xFF112240),
        cardLight = Color(0xFFFFFFFF),
        cardDark = Color(0xFF112240),
        primaryAccent = Color(0xFF2563EB),
        secondaryAccent = Color(0xFF38BDF8),
        defaultDarkCards = false
    )
    AppThemeId.BLACK_GOLD -> ThemePaletteSpec(
        id = id,
        background = Color(0xFF0B0B0D),
        surfaceVariant = Color(0xFF17171C),
        cardLight = Color(0xFFFFFBEB),
        cardDark = Color(0xFF16161A),
        primaryAccent = Color(0xFFD4AF37),
        secondaryAccent = Color(0xFFF59E0B),
        defaultDarkCards = true
    )
    AppThemeId.NEON_PURPLE -> ThemePaletteSpec(
        id = id,
        background = Color(0xFF120B24),
        surfaceVariant = Color(0xFF1D1338),
        cardLight = Color(0xFFFAF5FF),
        cardDark = Color(0xFF1D1338),
        primaryAccent = Color(0xFF7C3AED),
        secondaryAccent = Color(0xFFC084FC),
        defaultDarkCards = true
    )
    AppThemeId.PURE_WHITE -> ThemePaletteSpec(
        id = id,
        background = Color(0xFFF8FAFC),
        surfaceVariant = Color(0xFFE2E8F0),
        cardLight = Color(0xFFFFFFFF),
        cardDark = Color(0xFF1E293B),
        primaryAccent = Color(0xFF2563EB),
        secondaryAccent = Color(0xFF0284C7),
        defaultDarkCards = false
    )
    AppThemeId.DARK_AMOLED -> ThemePaletteSpec(
        id = id,
        background = Color(0xFF000000),
        surfaceVariant = Color(0xFF111114),
        cardLight = Color(0xFFF8FAFC),
        cardDark = Color(0xFF0E0E11),
        primaryAccent = Color(0xFF3B82F6),
        secondaryAccent = Color(0xFF60A5FA),
        defaultDarkCards = true
    )
    AppThemeId.CYBER_GREEN -> ThemePaletteSpec(
        id = id,
        background = Color(0xFF041410),
        surfaceVariant = Color(0xFF0B241D),
        cardLight = Color(0xFFECFDF5),
        cardDark = Color(0xFF0B241D),
        primaryAccent = Color(0xFF10B981),
        secondaryAccent = Color(0xFF34D399),
        defaultDarkCards = true
    )
    AppThemeId.OCEAN_BLUE -> ThemePaletteSpec(
        id = id,
        background = Color(0xFF062032),
        surfaceVariant = Color(0xFF0C314A),
        cardLight = Color(0xFFFFFFFF),
        cardDark = Color(0xFF0C314A),
        primaryAccent = Color(0xFF0284C7),
        secondaryAccent = Color(0xFF38BDF8),
        defaultDarkCards = false
    )
    AppThemeId.ROSE_GOLD -> ThemePaletteSpec(
        id = id,
        background = Color(0xFF211118),
        surfaceVariant = Color(0xFF331B26),
        cardLight = Color(0xFFFFF1F2),
        cardDark = Color(0xFF331B26),
        primaryAccent = Color(0xFFE11D48),
        secondaryAccent = Color(0xFFFDA4AF),
        defaultDarkCards = false
    )
}
