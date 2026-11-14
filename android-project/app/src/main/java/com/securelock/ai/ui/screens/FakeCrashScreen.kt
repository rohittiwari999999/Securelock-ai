package com.securelock.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.securelock.ai.data.ProtectionScreenConfig
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.ui.components.SecureTopBar
import com.securelock.ai.ui.components.WhiteSecurityCard
import com.securelock.ai.ui.theme.CrispWhite
import com.securelock.ai.ui.theme.DeepNavy
import com.securelock.ai.ui.theme.ElectricBlue
import com.securelock.ai.ui.theme.SlateText

@Composable
fun FakeCrashConfigScreen(
    repository: SecureStorageRepository,
    onBack: () -> Unit,
    onTestAuthReturn: () -> Unit
) {
    val settings by repository.settings.collectAsState()
    val config = settings.protectionScreenConfig
    var showPreviewGate by remember { mutableStateOf(false) }

    if (showPreviewGate) {
        AppProtectionGateScreen(
            config = config,
            onProceedToUnlock = {
                showPreviewGate = false
                onTestAuthReturn()
            },
            onDismissHome = { showPreviewGate = false }
        )
        return
    }

    Scaffold(
        containerColor = DeepNavy,
        topBar = {
            SecureTopBar(
                title = "App Protection Screen",
                subtitle = "Neutral App Unavailable cover",
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Protection Cover",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Shows a neutral App Unavailable screen before authentication. Never mimics Android OS crash dialogs.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateText
                                )
                            }
                            Switch(
                                checked = config.enabled,
                                onCheckedChange = { enabled ->
                                    repository.updateSettings {
                                        it.copy(protectionScreenConfig = config.copy(enabled = enabled))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                WhiteSecurityCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Customize Screen Copy", style = MaterialTheme.typography.titleMedium)

                        OutlinedTextField(
                            value = config.title,
                            onValueChange = { value ->
                                repository.updateSettings {
                                    it.copy(protectionScreenConfig = config.copy(title = value))
                                }
                            },
                            label = { Text("Screen Heading") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = config.message,
                            onValueChange = { value ->
                                repository.updateSettings {
                                    it.copy(protectionScreenConfig = config.copy(message = value))
                                }
                            },
                            label = { Text("Neutral Protection Message") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Cover Icon", style = MaterialTheme.typography.labelMedium, color = SlateText)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "shield" to "Shield",
                                "lock" to "Lock",
                                "hidden" to "Privacy",
                                "pause" to "Paused"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = config.iconKey == key,
                                    onClick = {
                                        repository.updateSettings {
                                            it.copy(protectionScreenConfig = config.copy(iconKey = key))
                                        }
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Text("Background Color", style = MaterialTheme.typography.labelMedium, color = SlateText)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(0xFF0A1628L, 0xFF1E293BL, 0xFF0F172AL, 0xFF112240L).forEach { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(hex))
                                        .clickable {
                                            repository.updateSettings {
                                                it.copy(protectionScreenConfig = config.copy(backgroundHex = hex))
                                            }
                                        }
                                )
                            }
                        }

                        Button(
                            onClick = { showPreviewGate = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Preview App Protection Screen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppProtectionGateScreen(
    config: ProtectionScreenConfig,
    onProceedToUnlock: () -> Unit,
    onDismissHome: () -> Unit
) {
    val icon: ImageVector = when (config.iconKey) {
        "lock" -> Icons.Default.Lock
        "hidden" -> Icons.Default.VisibilityOff
        "pause" -> Icons.Default.PauseCircle
        else -> Icons.Default.Shield
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(config.backgroundHex))
            .safeDrawingPadding()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CrispWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(config.accentHex).copy(alpha = 0.14f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(config.accentHex),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = config.title.ifBlank { "App Unavailable" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepNavy,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = config.message.ifBlank {
                        "This application is protected by SecureLock AI. Authenticate to continue."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onProceedToUnlock,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(config.accentHex)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Authenticate to Unlock")
                }

                OutlinedButton(
                    onClick = onDismissHome,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}
