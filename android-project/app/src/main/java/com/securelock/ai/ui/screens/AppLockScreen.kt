package com.securelock.ai.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.securelock.ai.data.InstalledAppItem
import com.securelock.ai.security.PermissionHelper
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.service.AppLockMonitorService
import com.securelock.ai.ui.components.SecureTopBar
import com.securelock.ai.ui.components.WhiteSecurityCard
import com.securelock.ai.ui.theme.CrispWhite
import com.securelock.ai.ui.theme.DeepNavy
import com.securelock.ai.ui.theme.ElectricBlue
import com.securelock.ai.ui.theme.SlateText

@Composable
fun AppLockScreen(
    repository: SecureStorageRepository,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val context = LocalContext.current
    val settings by repository.settings.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val permissions = remember(context) { PermissionHelper.getSnapshot(context) }

    val installedApps = remember(context, settings.protectedPackages) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }
        resolved
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName }
            .map { pkg ->
                val label = runCatching {
                    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0L))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getApplicationInfo(pkg, 0)
                    }
                    pm.getApplicationLabel(info).toString()
                }.getOrDefault(pkg)
                InstalledAppItem(
                    packageName = pkg,
                    appName = label,
                    isLocked = settings.protectedPackages.contains(pkg)
                )
            }
            .sortedWith(compareByDescending<InstalledAppItem> { it.isLocked }.thenBy { it.appName.lowercase() })
    }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = DeepNavy,
        topBar = {
            SecureTopBar(
                title = "App Lock",
                subtitle = "${settings.protectedPackages.size} apps protected",
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
                            text = "Supported Android Enforcement Disclosure",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Android does not allow ordinary apps to silently enforce system-wide locking. You must explicitly enable Display Over Other Apps plus either Usage Access or Accessibility Service.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateText
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onOpenPermissions,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                            ) {
                                Text("Review Permissions")
                            }
                            if (permissions.usageStatsGranted && permissions.overlayGranted) {
                                OutlinedButton(onClick = {
                                    val serviceIntent = Intent(context, AppLockMonitorService::class.java)
                                    ContextCompat.startForegroundService(context, serviceIntent)
                                }) {
                                    Text("Start Monitor")
                                }
                            }
                        }
                    }
                }
            }

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
                                    text = "Protect Android Settings",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Prompt unlock when System Settings opens",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateText
                                )
                            }
                            Switch(
                                checked = settings.protectSettingsScreen,
                                onCheckedChange = { enabled ->
                                    repository.updateSettings { it.copy(protectSettingsScreen = enabled) }
                                }
                            )
                        }

                        Text(
                            text = "Auto-Relock Timeout",
                            style = MaterialTheme.typography.labelMedium,
                            color = SlateText
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0 to "Immediate", 30 to "30s", 120 to "2m", 300 to "5m").forEach { (sec, label) ->
                                FilterChip(
                                    selected = settings.relockTimeoutSeconds == sec,
                                    onClick = {
                                        repository.updateSettings { it.copy(relockTimeoutSeconds = sec) }
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed apps or package name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CrispWhite,
                        unfocusedContainerColor = CrispWhite,
                        focusedTextColor = DeepNavy,
                        unfocusedTextColor = DeepNavy
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(filteredApps, key = { it.packageName }) { app ->
                WhiteSecurityCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.labelMedium,
                                color = SlateText
                            )
                        }
                        Switch(
                            checked = app.isLocked,
                            onCheckedChange = { shouldLock ->
                                repository.updateSettings { current ->
                                    val updatedSet = if (shouldLock) {
                                        current.protectedPackages + app.packageName
                                    } else {
                                        current.protectedPackages - app.packageName
                                    }
                                    current.copy(protectedPackages = updatedSet)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
