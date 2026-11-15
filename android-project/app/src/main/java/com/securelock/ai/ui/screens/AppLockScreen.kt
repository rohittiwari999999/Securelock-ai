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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.securelock.ai.data.InstalledAppItem
import com.securelock.ai.data.LockMethod
import com.securelock.ai.security.PermissionHelper
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.service.AppLockMonitorService
import com.securelock.ai.service.AppLockServiceController
import com.securelock.ai.service.AppLockSessionManager
import com.securelock.ai.ui.components.SecureTopBar
import com.securelock.ai.ui.components.WhiteSecurityCard
import com.securelock.ai.ui.theme.AmberWarn
import com.securelock.ai.ui.theme.CrimsonAlert
import com.securelock.ai.ui.theme.EmeraldSafe

@Composable
fun AppLockScreen(
    repository: SecureStorageRepository,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by repository.settings.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    var permissions by remember { mutableStateOf(PermissionHelper.getSnapshot(context)) }
    val monitorRunning by AppLockMonitorService.isRunningFlow.collectAsState()
    var pendingPermissionPromptApp by remember { mutableStateOf<String?>(null) }

    fun refreshAndSyncEngine() {
        permissions = PermissionHelper.getSnapshot(context)
        AppLockServiceController.syncMonitorService(context, repository.settings.value)
    }

    LaunchedEffect(settings.protectedPackages, settings.protectSettingsScreen) {
        refreshAndSyncEngine()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshAndSyncEngine()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun launchExternalPermissionSettings(intent: Intent) {
        AppLockSessionManager.setInternalFlowSuspendedForPicker(true)
        runCatching { context.startActivity(intent) }
    }

    val engineReady = permissions.accessibilityEnabled ||
        (permissions.usageStatsGranted && permissions.overlayGranted)
    val needsProtection = AppLockServiceController.needsAppProtection(settings)
    val engineActive = permissions.accessibilityEnabled ||
        (permissions.usageStatsGranted && permissions.overlayGranted && monitorRunning)

    val installedApps = remember(context, settings.protectedPackages, settings.perAppLockMethods) {
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
                    isLocked = settings.protectedPackages.contains(pkg),
                    overrideMethod = settings.perAppLockMethods[pkg]
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
        containerColor = MaterialTheme.colorScheme.background,
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Real-Time Protection Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val statusText = when {
                                engineActive -> "ACTIVE"
                                needsProtection && !engineReady -> "SETUP REQUIRED"
                                needsProtection && !monitorRunning -> "STARTING"
                                else -> "STANDBY"
                            }
                            val statusColor = when {
                                engineActive -> EmeraldSafe
                                needsProtection && !engineReady -> CrimsonAlert
                                else -> AmberWarn
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = statusColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = if (engineActive) {
                                "Monitoring active (${if (permissions.accessibilityEnabled) "Accessibility" else "Usage Access"} + Overlay). Opening locked apps triggers your lock screen immediately."
                            } else {
                                "To actually lock WhatsApp and other apps when opened on Android, grant Display Over Other Apps plus Accessibility Service (instant) or Usage Access:"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• Display Over Other Apps: ${if (permissions.overlayGranted) "Granted" else "Missing"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (permissions.overlayGranted) EmeraldSafe else CrimsonAlert
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val detectLabel = when {
                                permissions.accessibilityEnabled -> "Accessibility Active (Instant)"
                                permissions.usageStatsGranted -> "Usage Access Granted"
                                else -> "Missing (Enable Accessibility or Usage Access)"
                            }
                            Text(
                                text = "• App Detection: $detectLabel",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (permissions.accessibilityEnabled || permissions.usageStatsGranted) EmeraldSafe else CrimsonAlert
                            )
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!permissions.overlayGranted) {
                                item {
                                    Button(
                                        onClick = {
                                            launchExternalPermissionSettings(
                                                PermissionHelper.overlaySettingsIntent(context)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Enable Overlay")
                                    }
                                }
                            }
                            if (!permissions.accessibilityEnabled) {
                                item {
                                    Button(
                                        onClick = {
                                            launchExternalPermissionSettings(
                                                PermissionHelper.accessibilitySettingsIntent()
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Enable Accessibility")
                                    }
                                }
                            }
                            if (!permissions.usageStatsGranted) {
                                item {
                                    OutlinedButton(onClick = {
                                        launchExternalPermissionSettings(
                                            PermissionHelper.usageAccessSettingsIntent()
                                        )
                                    }) {
                                        Text("Grant Usage Access")
                                    }
                                }
                            }
                            item {
                                OutlinedButton(onClick = {
                                    launchExternalPermissionSettings(
                                        PermissionHelper.batteryOptimizationSettingsIntent()
                                    )
                                }) {
                                    Text("Battery Unrestricted")
                                }
                            }
                            item {
                                OutlinedButton(onClick = onOpenPermissions) {
                                    Text("All Permissions")
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
                                Text("Protect Android Settings", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Prompt unlock when System Settings opens",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.protectSettingsScreen,
                                onCheckedChange = { enabled ->
                                    repository.updateSettings { it.copy(protectSettingsScreen = enabled) }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Lock on Screen Off", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Immediately relock all apps when display turns off",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.lockOnScreenOff,
                                onCheckedChange = { enabled ->
                                    repository.updateSettings { it.copy(lockOnScreenOff = enabled) }
                                }
                            )
                        }

                        Text(
                            text = "Auto-Relock Delay",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(0 to "Immediate", 15 to "15s", 30 to "30s", 120 to "2m", 300 to "5m")) { (sec, label) ->
                                FilterChip(
                                    selected = settings.relockTimeoutSeconds == sec,
                                    onClick = { repository.updateSettings { it.copy(relockTimeoutSeconds = sec) } },
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
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(filteredApps, key = { it.packageName }) { app ->
                WhiteSecurityCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.appName, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    if (shouldLock && !AppLockServiceController.hasRequiredOverlayOrAccessibility(context)) {
                                        pendingPermissionPromptApp = app.appName
                                    }
                                }
                            )
                        }

                        if (app.isLocked) {
                            Text(
                                text = "Per-App Lock Method",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    FilterChip(
                                        selected = app.overrideMethod == null,
                                        onClick = {
                                            repository.updateSettings { current ->
                                                current.copy(perAppLockMethods = current.perAppLockMethods - app.packageName)
                                            }
                                        },
                                        label = { Text("Global") }
                                    )
                                }
                                items(LockMethod.entries) { method ->
                                    FilterChip(
                                        selected = app.overrideMethod == method,
                                        onClick = {
                                            repository.updateSettings { current ->
                                                current.copy(
                                                    perAppLockMethods = current.perAppLockMethods + (app.packageName to method)
                                                )
                                            }
                                        },
                                        label = { Text(method.label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingPermissionPromptApp?.let { appName ->
        AlertDialog(
            onDismissRequest = { pendingPermissionPromptApp = null },
            title = { Text("Enable Permissions to Lock $appName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "$appName is now in your locked list, but Android requires system permissions below before any app lock screen can appear over $appName:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!permissions.overlayGranted) {
                        Button(
                            onClick = {
                                launchExternalPermissionSettings(
                                    PermissionHelper.overlaySettingsIntent(context)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("1. Allow Display Over Other Apps")
                        }
                    }
                    if (!permissions.accessibilityEnabled) {
                        Button(
                            onClick = {
                                launchExternalPermissionSettings(
                                    PermissionHelper.accessibilitySettingsIntent()
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("2. Turn ON Accessibility Monitor (Instant)")
                        }
                    }
                    if (!permissions.usageStatsGranted) {
                        OutlinedButton(
                            onClick = {
                                launchExternalPermissionSettings(
                                    PermissionHelper.usageAccessSettingsIntent()
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Or Grant Usage Access")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    pendingPermissionPromptApp = null
                    refreshAndSyncEngine()
                }) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingPermissionPromptApp = null
                    onOpenPermissions()
                }) {
                    Text("Open Permissions")
                }
            }
        )
    }
}
