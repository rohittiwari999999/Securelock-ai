package com.securelock.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCameraFront
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.security.VaultRepository
import com.securelock.ai.ui.components.SecureTopBar
import com.securelock.ai.ui.components.WhiteSecurityCard
import com.securelock.ai.ui.navigation.Routes
import com.securelock.ai.ui.theme.AmberWarn
import com.securelock.ai.ui.theme.CrimsonAlert
import com.securelock.ai.ui.theme.EmeraldSafe

private data class DashboardModuleCard(
    val title: String,
    val subtitle: String,
    val metric: String,
    val icon: ImageVector,
    val accent: Color,
    val route: String
)

@Composable
fun DashboardScreen(
    repository: SecureStorageRepository,
    vaultRepository: VaultRepository,
    onNavigate: (String) -> Unit,
    onLockNow: () -> Unit
) {
    val settings by repository.settings.collectAsState()
    val stats by repository.statistics.collectAsState()
    val intruders by repository.intruders.collectAsState()
    val maskedNotifications by repository.notifications.collectAsState()
    val vaultItems by vaultRepository.mediaItems.collectAsState()

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val bg = MaterialTheme.colorScheme.background
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val fg = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val cards = listOf(
        DashboardModuleCard(
            title = "App Lock",
            subtitle = "Pattern, PIN & Fingerprint app protection",
            metric = "${settings.protectedPackages.size} Locked",
            icon = Icons.Default.Lock,
            accent = primary,
            route = Routes.APP_LOCK
        ),
        DashboardModuleCard(
            title = "Secret Gallery Vault",
            subtitle = "Encrypted private photos, videos & albums",
            metric = "${vaultItems.size} Items",
            icon = Icons.Default.PhotoLibrary,
            accent = primary,
            route = Routes.VAULT
        ),
        DashboardModuleCard(
            title = "Intruder Detection",
            subtitle = "Failed attempt threshold & foreground capture",
            metric = "${intruders.size} Events",
            icon = Icons.Default.PhotoCameraFront,
            accent = CrimsonAlert,
            route = Routes.INTRUDER_SELFIE
        ),
        DashboardModuleCard(
            title = "Lock Notifications",
            subtitle = "Local-only notification privacy vault",
            metric = if (settings.lockNotificationsEnabled) "${maskedNotifications.size} Masked" else "Paused",
            icon = Icons.Default.NotificationsOff,
            accent = primary,
            route = Routes.LOCK_NOTIFICATIONS
        ),
        DashboardModuleCard(
            title = "Fake Crash Screen",
            subtitle = "Simulated decoy gates inside protection flow",
            metric = if (settings.protectionScreenConfig.enabled) "Active" else "Off",
            icon = Icons.Default.WarningAmber,
            accent = AmberWarn,
            route = Routes.FAKE_CRASH
        ),
        DashboardModuleCard(
            title = "Camera Privacy",
            subtitle = "Camera permissions, indicators & live self-test",
            metric = "Monitor",
            icon = Icons.Default.CameraAlt,
            accent = EmeraldSafe,
            route = Routes.CAMERA_MONITOR
        ),
        DashboardModuleCard(
            title = "Private Browser",
            subtitle = "Multi-tab WebView with zero history persistence",
            metric = "Incognito",
            icon = Icons.Default.Language,
            accent = primary,
            route = Routes.PRIVATE_BROWSER
        ),
        DashboardModuleCard(
            title = "Statistics",
            subtitle = "Weekly unlock analytics & security event log",
            metric = "${stats.totalAttempts} Tries",
            icon = Icons.Default.BarChart,
            accent = secondary,
            route = Routes.STATISTICS
        ),
        DashboardModuleCard(
            title = "Theme Customization",
            subtitle = "8 themes, custom accents & launcher icons",
            metric = settings.themeId.displayName,
            icon = Icons.Default.Palette,
            accent = primary,
            route = Routes.THEME_CUSTOMIZATION
        )
    )

    Scaffold(
        containerColor = bg,
        topBar = {
            SecureTopBar(
                title = "SecureLock AI",
                subtitle = "Offline Privacy & Vault Suite",
                actions = {
                    IconButton(onClick = { onNavigate(Routes.PERMISSIONS) }) {
                        Icon(Icons.Default.Security, contentDescription = "Permissions Manager")
                    }
                    IconButton(onClick = { onNavigate(Routes.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = secondary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Keystore Encryption Active",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = fg
                                )
                                Text(
                                    text = "${settings.protectedPackages.size} apps · ${vaultItems.size} vault files · ${stats.failedAttempts} failed tries",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = secondary
                                )
                            }
                        }
                        TextButton(onClick = onLockNow) {
                            Text("Lock", color = secondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            items(cards, key = { it.route }) { item ->
                WhiteSecurityCard(
                    onClick = { onNavigate(item.route) }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = item.accent.copy(alpha = 0.14f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = item.accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Text(
                                text = item.metric,
                                style = MaterialTheme.typography.labelMedium,
                                color = item.accent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
