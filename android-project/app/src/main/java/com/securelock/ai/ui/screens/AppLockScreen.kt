package com.securelock.ai.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.securelock.ai.ui.components.AccessibilityHelpDialog
import com.securelock.ai.ui.components.PatternLockPad
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
    var hasPattern by remember { mutableStateOf(repository.hasPatternConfigured()) }
    var hasPin by remember { mutableStateOf(repository.hasPinConfigured()) }
    var showPatternSetupDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showAccessibilityHelpDialog by remember { mutableStateOf(false) }
    var pendingPermissionPromptApp by remember { mutableStateOf<String?>(null) }
    val accessibilityDeclared = remember(context) {
        PermissionHelper.hasAccessibilityServiceDeclared(context)
    }

    fun refreshAndSyncEngine() {
        permissions = PermissionHelper.getSnapshot(context)
        hasPattern = repository.hasPatternConfigured()
        hasPin = repository.hasPinConfigured()
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

    fun launchAccessibilityDirectly() {
        AppLockSessionManager.setInternalFlowSuspendedForPicker(true)
        PermissionHelper.launchAccessibilitySettings(context)
    }

    val engineReady = permissions.accessibilityEnabled ||
        (permissions.usageStatsGranted && permissions.overlayGranted)
    val needsProtection = AppLockServiceController.needsAppProtection(settings)
    val engineActive = permissions.accessibilityEnabled ||
        (permissions.usageStatsGranted && permissions.overlayGranted && monitorRunning)

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
                    isLocked = settings.protectedPackages.contains(pkg),
                    overrideMethod = settings.preferredLockMethod
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

    val activeMethodSummary = buildList {
        if (hasPattern) add("Pattern")
        if (hasPin) add("PIN")
        if (settings.biometricEnabled) add("Fingerprint")
    }.joinToString(" · ").ifEmpty { "Set Pattern or PIN" }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            SecureTopBar(
                title = "App Lock",
                subtitle = "${settings.protectedPackages.size} apps · $activeMethodSummary",
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
                                if (permissions.accessibilityEnabled) {
                                    "Accessibility engine active (single-engine mode — zero conflict)."
                                } else {
                                    "Usage Access + Overlay engine active (no Accessibility needed)."
                                }
                            } else if (accessibilityDeclared) {
                                "To lock WhatsApp and other apps, enable Accessibility Service (recommended) or Display Over Other Apps + Usage Access:"
                            } else {
                                "To lock WhatsApp and other apps, allow Display Over Other Apps + Usage Access below (no Accessibility needed in this APK):"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "• Display Over Other Apps: ${if (permissions.overlayGranted) "Granted" else "Missing"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (permissions.overlayGranted) EmeraldSafe else CrimsonAlert
                        )

                        val detectLabel = when {
                            permissions.accessibilityEnabled -> "Accessibility Active (Instant)"
                            permissions.usageStatsGranted && !accessibilityDeclared -> "Usage Access Active (Accessibility Not Needed)"
                            permissions.usageStatsGranted -> "Usage Access Granted"
                            else -> if (accessibilityDeclared) "Missing (Enable Accessibility or Usage Access)" else "Missing (Grant Usage Access)"
                        }
                        Text(
                            text = "• App Detection: $detectLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (permissions.accessibilityEnabled || permissions.usageStatsGranted) EmeraldSafe else CrimsonAlert
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (accessibilityDeclared && !permissions.accessibilityEnabled) {
                                item {
                                    Button(
                                        onClick = { launchAccessibilityDirectly() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Open Accessibility Directly")
                                    }
                                }
                                item {
                                    OutlinedButton(onClick = { showAccessibilityHelpDialog = true }) {
                                        Text("Accessibility Help")
                                    }
                                }
                            } else if (accessibilityDeclared && permissions.accessibilityEnabled) {
                                item {
                                    OutlinedButton(onClick = { showAccessibilityHelpDialog = true }) {
                                        Text("Accessibility Guide")
                                    }
                                }
                            } else if (!accessibilityDeclared && engineReady) {
                                item {
                                    OutlinedButton(onClick = { showAccessibilityHelpDialog = true }) {
                                        Text("OEM Pop-up Tip")
                                    }
                                }
                            }
                            if (!permissions.overlayGranted) {
                                item {
                                    OutlinedButton(onClick = {
                                        launchExternalPermissionSettings(
                                            PermissionHelper.overlaySettingsIntent(context)
                                        )
                                    }) {
                                        Text("Enable Overlay")
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
                        Text(
                            text = "Lock Methods (Pattern · PIN · Fingerprint)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Use Pattern, PIN, and Fingerprint together on one unified lock screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.preferredLockMethod == LockMethod.PATTERN,
                                onClick = {
                                    if (hasPattern) {
                                        repository.updateSettings {
                                            it.copy(
                                                preferredLockMethod = LockMethod.PATTERN,
                                                protectionScreenConfig = it.protectionScreenConfig.copy(enabled = false)
                                            )
                                        }
                                    } else {
                                        showPatternSetupDialog = true
                                    }
                                },
                                label = { Text("Default: Pattern") }
                            )
                            FilterChip(
                                selected = settings.preferredLockMethod == LockMethod.PIN_4 ||
                                    settings.preferredLockMethod == LockMethod.PIN_6 ||
                                    settings.preferredLockMethod == LockMethod.PIN_CUSTOM,
                                onClick = {
                                    if (hasPin) {
                                        val pinMethod = when (repository.configuredPinLength()) {
                                            6 -> LockMethod.PIN_6
                                            4 -> LockMethod.PIN_4
                                            else -> LockMethod.PIN_CUSTOM
                                        }
                                        repository.updateSettings {
                                            it.copy(
                                                preferredLockMethod = pinMethod,
                                                protectionScreenConfig = it.protectionScreenConfig.copy(enabled = false)
                                            )
                                        }
                                    } else {
                                        showPinSetupDialog = true
                                    }
                                },
                                label = { Text("Default: PIN") }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showPatternSetupDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(if (hasPattern) "Change Pattern" else "Set Pattern")
                            }
                            OutlinedButton(onClick = { showPinSetupDialog = true }) {
                                Text(if (hasPin) "Change PIN" else "Set PIN")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Fingerprint Unlock", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Allow Fingerprint alongside Pattern & PIN",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.biometricEnabled,
                                onCheckedChange = { enabled ->
                                    repository.updateSettings { it.copy(biometricEnabled = enabled) }
                                }
                            )
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
                                    "Require Pattern, PIN or Fingerprint when Settings opens",
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
                            text = "Auto-Relock Delay (counts after leaving the app)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(
                                listOf(
                                    0 to "Immediate",
                                    15 to "15s",
                                    30 to "30s",
                                    120 to "2m",
                                    300 to "5m",
                                    -1 to "Screen Off"
                                )
                            ) { (sec, label) ->
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appName, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (app.isLocked) "${app.packageName} · $activeMethodSummary" else app.packageName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (app.isLocked) EmeraldSafe else MaterialTheme.colorScheme.onSurfaceVariant
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
                                    current.copy(
                                        protectedPackages = updatedSet,
                                        protectionScreenConfig = current.protectionScreenConfig.copy(enabled = false)
                                    )
                                }
                                if (shouldLock && !repository.hasPatternConfigured() && !repository.hasPinConfigured()) {
                                    showPatternSetupDialog = true
                                } else if (shouldLock && !AppLockServiceController.hasRequiredOverlayOrAccessibility(context)) {
                                    pendingPermissionPromptApp = app.appName
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showPatternSetupDialog) {
        var firstPattern by remember { mutableStateOf<List<Int>?>(null) }
        var hint by remember { mutableStateOf("Draw your Lock Pattern (4+ dots)") }
        AlertDialog(
            onDismissRequest = { showPatternSetupDialog = false },
            title = { Text(if (hasPattern) "Change Lock Pattern" else "Set Lock Pattern") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        PatternLockPad(
                            onPatternComplete = { nodes ->
                                if (nodes.size < 4) {
                                    hint = "Connect at least 4 dots"
                                } else if (firstPattern == null) {
                                    firstPattern = nodes
                                    hint = "Draw the same pattern again to confirm"
                                } else if (firstPattern == nodes) {
                                    repository.setPattern(nodes)
                                    repository.updateSettings {
                                        it.copy(
                                            preferredLockMethod = LockMethod.PATTERN,
                                            protectionScreenConfig = it.protectionScreenConfig.copy(enabled = false)
                                        )
                                    }
                                    hasPattern = true
                                    showPatternSetupDialog = false
                                    if (!AppLockServiceController.hasRequiredOverlayOrAccessibility(context)) {
                                        pendingPermissionPromptApp = "Protected Apps"
                                    }
                                } else {
                                    firstPattern = null
                                    hint = "Patterns did not match. Draw again."
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPatternSetupDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPinSetupDialog) {
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinErr by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = { Text(if (hasPin) "Change App Lock PIN" else "Set App Lock PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 12 && it.all(Char::isDigit)) newPin = it },
                        label = { Text("PIN (4–12 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 12 && it.all(Char::isDigit)) confirmPin = it },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                    pinErr?.let { Text(it, color = CrimsonAlert, style = MaterialTheme.typography.labelMedium) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPin.length < 4) {
                        pinErr = "PIN must be 4–12 digits"
                    } else if (newPin != confirmPin) {
                        pinErr = "PINs do not match"
                    } else {
                        repository.setPin(newPin)
                        repository.updateSettings {
                            it.copy(protectionScreenConfig = it.protectionScreenConfig.copy(enabled = false))
                        }
                        hasPin = true
                        showPinSetupDialog = false
                        if (!AppLockServiceController.hasRequiredOverlayOrAccessibility(context)) {
                            pendingPermissionPromptApp = "Protected Apps"
                        }
                    }
                }) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) { Text("Cancel") }
            }
        )
    }

    pendingPermissionPromptApp?.let { appName ->
        AlertDialog(
            onDismissRequest = { pendingPermissionPromptApp = null },
            title = { Text("Enable Permissions to Lock $appName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "$appName is protected. Enable Accessibility Monitor (instant) or Display Over Other Apps + Usage Access below:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (accessibilityDeclared && !permissions.accessibilityEnabled) {
                        Button(
                            onClick = { launchAccessibilityDirectly() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("1. Open Accessibility Directly (Instant)")
                        }
                        OutlinedButton(
                            onClick = {
                                pendingPermissionPromptApp = null
                                showAccessibilityHelpDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("How to Enable Accessibility & Restricted Settings")
                        }
                    }
                    if (!permissions.overlayGranted) {
                        OutlinedButton(
                            onClick = {
                                launchExternalPermissionSettings(
                                    PermissionHelper.overlaySettingsIntent(context)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Allow Display Over Other Apps")
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
                            Text("Grant Usage Access")
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

    if (showAccessibilityHelpDialog) {
        AccessibilityHelpDialog(
            isAlreadyEnabled = permissions.accessibilityEnabled,
            onDismiss = {
                showAccessibilityHelpDialog = false
                refreshAndSyncEngine()
            },
            onOpenAccessibilitySettings = { launchAccessibilityDirectly() },
            onOpenAppInfo = {
                launchExternalPermissionSettings(
                    PermissionHelper.appPrivacySettingsIntent(context)
                )
            },
            onOpenOverlaySettings = {
                launchExternalPermissionSettings(
                    PermissionHelper.overlaySettingsIntent(context)
                )
            },
            onOpenUsageAccessSettings = {
                launchExternalPermissionSettings(
                    PermissionHelper.usageAccessSettingsIntent()
                )
            }
        )
    }
}
