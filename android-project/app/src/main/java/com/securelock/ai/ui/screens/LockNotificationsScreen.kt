package com.securelock.ai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.securelock.ai.security.PermissionHelper
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.ui.components.SecureTopBar
import com.securelock.ai.ui.components.WhiteSecurityCard
import com.securelock.ai.ui.theme.ElectricBlue
import com.securelock.ai.ui.theme.EmeraldSafe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LockNotificationsScreen(
    repository: SecureStorageRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by repository.settings.collectAsState()
    val maskedNotifications by repository.notifications.collectAsState()
    val listenerGranted = remember(context) {
        PermissionHelper.isNotificationListenerEnabled(context)
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            SecureTopBar(
                title = "Lock Notifications",
                subtitle = "Local-only notification privacy vault",
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
            item {
                WhiteSecurityCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Optional Notification Listener Permission",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (listenerGranted) {
                                "Notification Listener access is active. Masked notification previews are stored exclusively on-device and never uploaded."
                            } else {
                                "Grant Notification Access in Android Settings so SecureLock AI can hide sensitive message previews for protected apps. Notification content never leaves this device."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (listenerGranted) EmeraldSafe else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                context.startActivity(PermissionHelper.notificationListenerSettingsIntent())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Text(if (listenerGranted) "Manage Notification Access" else "Enable Notification Access")
                        }
                    }
                }
            }

            item {
                WhiteSecurityCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enable Notification Privacy", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Redact previews from protected apps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.lockNotificationsEnabled,
                                onCheckedChange = { enabled ->
                                    repository.updateSettings { it.copy(lockNotificationsEnabled = enabled) }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Mask Locked Apps", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Hide notifications for every app in App Lock",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.maskAllProtectedNotifications,
                                onCheckedChange = { enabled ->
                                    repository.updateSettings { it.copy(maskAllProtectedNotifications = enabled) }
                                }
                            )
                        }

                        if (maskedNotifications.isNotEmpty()) {
                            OutlinedButton(onClick = { repository.clearProtectedNotifications() }) {
                                Text("Clear Local Vault Log")
                            }
                        }
                    }
                }
            }

            if (maskedNotifications.isEmpty()) {
                item {
                    WhiteSecurityCard {
                        Text(
                            text = "No protected notifications have been masked yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(maskedNotifications, key = { it.id }) { item ->
                    val timeStr = remember(item.timestampMillis) {
                        SimpleDateFormat("HH:mm · MMM dd", Locale.getDefault())
                            .format(Date(item.timestampMillis))
                    }
                    WhiteSecurityCard {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.appName, style = MaterialTheme.typography.titleMedium)
                                Text(timeStr, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(item.maskedTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(item.maskedBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
