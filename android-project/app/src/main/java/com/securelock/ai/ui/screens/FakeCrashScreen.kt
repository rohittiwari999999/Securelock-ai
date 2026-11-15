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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
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
import com.securelock.ai.data.DecoyMode
import com.securelock.ai.data.ProtectionScreenConfig
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.ui.components.NeuglassBackdrop
import com.securelock.ai.ui.components.SecureTopBar
import com.securelock.ai.ui.components.WhiteSecurityCard
import com.securelock.ai.ui.components.neuglassSurface
import com.securelock.ai.ui.theme.CrispWhite
import com.securelock.ai.ui.theme.DeepNavy

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
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            SecureTopBar(
                title = "Fake Crash / Decoy Screen",
                subtitle = "Simulated screens inside SecureLock flow",
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
                                    text = "Enable Simulated Decoy Cover",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "These are simulated privacy screens triggered only inside SecureLock's protection flow. Authentication is required to exit, and they never impersonate genuine Android system security dialogs.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Text("Simulated Decoy Template", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(DecoyMode.entries) { mode ->
                                FilterChip(
                                    selected = config.decoyMode == mode,
                                    onClick = {
                                        repository.updateSettings {
                                            it.copy(
                                                protectionScreenConfig = config.copy(
                                                    decoyMode = mode,
                                                    title = mode.defaultTitle,
                                                    message = mode.defaultMessage
                                                )
                                            )
                                        }
                                    },
                                    label = { Text(mode.label) }
                                )
                            }
                        }

                        if (config.decoyMode != DecoyMode.BLANK_SCREEN) {
                            OutlinedTextField(
                                value = config.title,
                                onValueChange = { value ->
                                    repository.updateSettings {
                                        it.copy(protectionScreenConfig = config.copy(title = value))
                                    }
                                },
                                label = { Text("Simulated Heading") },
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
                                label = { Text("Simulated Message") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = "Blank Screen shows a solid screen. Tap the subtle corner control or bottom action to authenticate and exit.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text("Cover Icon", style = MaterialTheme.typography.labelMedium)
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

                        Text("Backdrop Color", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(0xFF0A1628L, 0xFF000000L, 0xFF1E293BL, 0xFF112240L).forEach { hex ->
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Preview Simulated Decoy Screen")
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
    if (config.decoyMode == DecoyMode.BLANK_SCREEN) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(config.backgroundHex))
                .safeDrawingPadding()
                .padding(24.dp)
        ) {
            TextButton(
                onClick = onProceedToUnlock,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Text(
                    text = "Authenticate to Exit Decoy",
                    color = CrispWhite.copy(alpha = 0.35f)
                )
            }
        }
        return
    }

    val icon: ImageVector = when (config.decoyMode) {
        DecoyMode.APP_STOPPED_SIMULATED -> Icons.Default.ErrorOutline
        DecoyMode.LOADING_ERROR -> Icons.Default.HourglassEmpty
        DecoyMode.NETWORK_ERROR -> Icons.Default.CloudOff
        else -> when (config.iconKey) {
            "lock" -> Icons.Default.Lock
            "hidden" -> Icons.Default.VisibilityOff
            "pause" -> Icons.Default.PauseCircle
            else -> Icons.Default.Shield
        }
    }

    NeuglassBackdrop(
        accentColor = Color(config.accentHex),
        secondaryColor = Color(config.accentHex)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(config.backgroundHex).copy(alpha = 0.72f))
                .safeDrawingPadding()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .neuglassSurface(
                        cornerRadius = 24.dp,
                        surfaceColor = CrispWhite.copy(alpha = 0.92f),
                        accentColor = Color(config.accentHex)
                    )
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
                    text = config.title.ifBlank { config.decoyMode.defaultTitle },
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepNavy,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = config.message.ifBlank { config.decoyMode.defaultMessage },
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepNavy.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Simulated SecureLock Decoy",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(config.accentHex)
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
}
