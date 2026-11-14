package com.securelock.ai.ui.screens

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.securelock.ai.data.AppIconStyle
import com.securelock.ai.data.AppThemeId
import com.securelock.ai.data.CardStyleMode
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.ui.components.SecureTopBar
import com.securelock.ai.ui.components.WhiteSecurityCard
import com.securelock.ai.ui.theme.paletteForTheme

@Composable
fun ThemeCustomizationScreen(
    repository: SecureStorageRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by repository.settings.collectAsState()

    val accentSwatches = listOf(
        0xFF2563EB to "Electric",
        0xFFD4AF37 to "Gold",
        0xFF7C3AED to "Violet",
        0xFF10B981 to "Emerald",
        0xFF0284C7 to "Ocean",
        0xFFE11D48 to "Rose"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SecureTopBar(
                title = "Theme Customization",
                subtitle = "${settings.themeId.displayName} active",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Preview Card
            item {
                WhiteSecurityCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Column {
                            Text("Live Theme Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${settings.themeId.displayName} · ${settings.cardStyleMode.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 8 Theme Selector
            item {
                WhiteSecurityCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("8 Premium Themes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        AppThemeId.entries.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { themeId ->
                                    val palette = paletteForTheme(themeId)
                                    val selected = settings.themeId == themeId
                                    Surface(
                                        onClick = {
                                            repository.updateSettings {
                                                it.copy(themeId = themeId, customAccentHex = null)
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = palette.background,
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = if (selected) palette.primaryAccent else Color.Gray.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(palette.primaryAccent)
                                                )
                                                Text(
                                                    text = themeId.displayName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (themeId == AppThemeId.PURE_WHITE) Color(0xFF0A1628) else Color.White
                                                )
                                            }
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = palette.primaryAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Custom Accent & Light/Dark & Card Style
            item {
                WhiteSecurityCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Custom Accent Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            accentSwatches.forEach { (hex, _) ->
                                val selected = settings.customAccentHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(hex))
                                        .border(
                                            width = if (selected) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            repository.updateSettings { it.copy(customAccentHex = hex) }
                                        }
                                )
                            }
                        }

                        Text("Surface Mode", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(null to "Theme Default", true to "Light Cards", false to "Dark Cards").forEach { (value, label) ->
                                FilterChip(
                                    selected = settings.forceLightMode == value,
                                    onClick = { repository.updateSettings { it.copy(forceLightMode = value) } },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Text("Dynamic Card Style", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CardStyleMode.entries.forEach { style ->
                                FilterChip(
                                    selected = settings.cardStyleMode == style,
                                    onClick = { repository.updateSettings { it.copy(cardStyleMode = style) } },
                                    label = { Text(style.displayName) }
                                )
                            }
                        }
                    }
                }
            }

            // Custom App Icons (via Android activity-alias)
            item {
                WhiteSecurityCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Custom App Icon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Switches the active launcher activity-alias via PackageManager. Launcher updates within a few seconds.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppIconStyle.entries.chunked(2).forEach { col ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                    col.forEach { iconStyle ->
                                        FilterChip(
                                            selected = settings.appIconStyle == iconStyle,
                                            onClick = {
                                                val pm = context.packageManager
                                                AppIconStyle.entries.forEach { entry ->
                                                    val newState = if (entry == iconStyle) {
                                                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                                    } else {
                                                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                                                    }
                                                    pm.setComponentEnabledSetting(
                                                        ComponentName(context.packageName, entry.aliasClassName),
                                                        newState,
                                                        PackageManager.DONT_KILL_APP
                                                    )
                                                }
                                                repository.updateSettings { it.copy(appIconStyle = iconStyle) }
                                            },
                                            label = { Text(iconStyle.displayName) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
