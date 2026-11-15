package com.securelock.ai.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.securelock.ai.data.AppLanguage
import com.securelock.ai.data.LockMethod
import com.securelock.ai.security.PermissionHelper
import com.securelock.ai.security.SecretDateUnlockEvaluator
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.service.AppLockServiceController
import com.securelock.ai.service.AppLockSessionManager
import com.securelock.ai.ui.components.PatternLockPad
import com.securelock.ai.ui.components.SecureTopBar
import com.securelock.ai.ui.components.WhiteSecurityCard
import com.securelock.ai.ui.theme.CrimsonAlert
import com.securelock.ai.ui.theme.EmeraldSafe

@Composable
fun SettingsScreen(
    repository: SecureStorageRepository,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenThemes: () -> Unit
) {
    val context = LocalContext.current
    val settings by repository.settings.collectAsState()

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showChangePatternDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showSecretDateDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var regeneratedRecovery by remember { mutableStateOf<String?>(null) }
    var backupStatus by remember { mutableStateOf<String?>(null) }
    val hasSecretPin = remember(settings.secretDateConfig) {
        repository.hasSecretDatePinConfigured() || repository.hasAppLockCredentialConfigured()
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(repository.exportLocalBackupJson().toByteArray(Charsets.UTF_8))
            }
            backupStatus = "Local backup exported successfully."
        }.onFailure {
            backupStatus = "Failed to export backup."
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (content != null && repository.importLocalBackupJson(content)) {
                backupStatus = "Local backup restored successfully."
            } else {
                backupStatus = "Invalid backup file."
            }
        }.onFailure {
            backupStatus = "Unable to read backup file."
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SecureTopBar(
                title = "Settings & Recovery",
                subtitle = "Lock systems, timing & offline backup",
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
                        Text("Default Lock Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(LockMethod.entries) { method ->
                                FilterChip(
                                    selected = settings.preferredLockMethod == method,
                                    onClick = {
                                        when (method) {
                                            LockMethod.PATTERN -> if (!repository.hasPatternConfigured()) showChangePatternDialog = true else repository.updateSettings { it.copy(preferredLockMethod = method) }
                                            LockMethod.PASSWORD -> if (!repository.hasPasswordConfigured()) showChangePasswordDialog = true else repository.updateSettings { it.copy(preferredLockMethod = method) }
                                            else -> repository.updateSettings { it.copy(preferredLockMethod = method) }
                                        }
                                    },
                                    label = { Text(method.label) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showChangePinDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Change PIN")
                            }
                            OutlinedButton(onClick = { showChangePatternDialog = true }) {
                                Text("Set Pattern")
                            }
                            OutlinedButton(onClick = { showChangePasswordDialog = true }) {
                                Text("Set Password")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Fingerprint / Biometric Unlock", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Uses Android BiometricPrompt API",
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

                        OutlinedButton(onClick = {
                            regeneratedRecovery = repository.generateAndStoreRecoveryCode()
                        }) {
                            Text("Generate New Emergency Recovery Code")
                        }
                    }
                }
            }

            item {
                WhiteSecurityCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Auto-Lock & Screen-Off", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Lock on Screen Off", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Relock vault & apps when display locks",
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0 to "Immediate", 15 to "15s", 30 to "30s", 120 to "2m", 300 to "5m").forEach { (sec, label) ->
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
                WhiteSecurityCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Themes, Icons & Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = onOpenThemes,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Open Theme Studio (8 Themes & Icons)")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.entries.take(3).forEach { lang ->
                                FilterChip(
                                    selected = settings.language == lang,
                                    onClick = { repository.updateSettings { it.copy(language = lang) } },
                                    label = { Text(lang.displayName) }
                                )
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
                                    text = "Secret Date Unlock (Calendar Stealth)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (settings.secretDateConfig.enabled && hasSecretPin) {
                                        "Active: Launches as a standalone Calendar; tap your secret date 5 times to open SecureLock AI."
                                    } else {
                                        "Launch as a realistic Calendar app and open SecureLock AI by tapping a secret date 5 times."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.secretDateConfig.enabled && hasSecretPin,
                                onCheckedChange = { enable ->
                                    if (enable && !hasSecretPin) {
                                        showSecretDateDialog = true
                                    } else {
                                        repository.setSecretDateUnlockEnabled(enable)
                                    }
                                }
                            )
                        }

                        Button(
                            onClick = { showSecretDateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (hasSecretPin) "Update Secret Date & PIN" else "Configure Secret Date & PIN")
                        }
                    }
                }
            }

            item {
                WhiteSecurityCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Local Backup, Permissions & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        backupStatus?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium, color = EmeraldSafe)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    AppLockSessionManager.setInternalFlowSuspendedForPicker(true)
                                    exportBackupLauncher.launch("securelock_local_backup.json")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Export Backup")
                            }
                            OutlinedButton(onClick = {
                                AppLockSessionManager.setInternalFlowSuspendedForPicker(true)
                                importBackupLauncher.launch(arrayOf("application/json"))
                            }) {
                                Text("Restore Backup")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onOpenPermissions) {
                                Text("Permissions Manager")
                            }
                            OutlinedButton(onClick = { showPrivacyPolicyDialog = true }) {
                                Text("Privacy Policy")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChangePinDialog) {
        var currentPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change PIN (4, 6, or Custom 4–12 Digits)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.length <= 12 && it.all(Char::isDigit)) currentPin = it },
                        label = { Text("Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 12 && it.all(Char::isDigit)) newPin = it },
                        label = { Text("New PIN (4–12 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 12 && it.all(Char::isDigit)) confirmPin = it },
                        label = { Text("Confirm New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    err?.let { Text(it, color = CrimsonAlert, style = MaterialTheme.typography.labelMedium) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (!repository.verifyPin(currentPin)) {
                        err = "Current PIN is incorrect"
                    } else if (newPin.length < 4) {
                        err = "New PIN must be 4–12 digits"
                    } else if (newPin != confirmPin) {
                        err = "New PINs do not match"
                    } else {
                        repository.setPin(newPin)
                        showChangePinDialog = false
                    }
                }) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showChangePatternDialog) {
        var firstPattern by remember { mutableStateOf<List<Int>?>(null) }
        var patternStatus by remember { mutableStateOf("Draw a pattern connecting at least 4 dots") }
        AlertDialog(
            onDismissRequest = { showChangePatternDialog = false },
            title = { Text("Configure Security Pattern") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(patternStatus, style = MaterialTheme.typography.bodyMedium)
                    PatternLockPad(
                        onPatternComplete = { nodes ->
                            if (nodes.size < 4) {
                                patternStatus = "Connect at least 4 nodes"
                            } else if (firstPattern == null) {
                                firstPattern = nodes
                                patternStatus = "Draw the same pattern again to confirm"
                            } else if (firstPattern == nodes) {
                                repository.setPattern(nodes)
                                showChangePatternDialog = false
                            } else {
                                firstPattern = null
                                patternStatus = "Patterns did not match. Draw again."
                            }
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showChangePatternDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showChangePasswordDialog) {
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var passErr by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Set Alphanumeric Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Password (6+ chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                    passErr?.let { Text(it, color = CrimsonAlert, style = MaterialTheme.typography.labelMedium) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPass.length < 6) {
                        passErr = "Password must be at least 6 characters"
                    } else if (newPass != confirmPass) {
                        passErr = "Passwords do not match"
                    } else {
                        repository.setPassword(newPass)
                        repository.updateSettings { it.copy(preferredLockMethod = LockMethod.PASSWORD) }
                        showChangePasswordDialog = false
                    }
                }) {
                    Text("Save Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (regeneratedRecovery != null) {
        AlertDialog(
            onDismissRequest = { regeneratedRecovery = null },
            title = { Text("New Recovery Code") },
            text = { Text("Save this code offline in a safe place: ${regeneratedRecovery!!}") },
            confirmButton = {
                Button(onClick = { regeneratedRecovery = null }) { Text("Done") }
            }
        )
    }

    if (showSecretDateDialog) {
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        var selectedMonth by remember { mutableIntStateOf(settings.secretDateConfig.month.coerceIn(1, 12)) }
        var dayInput by remember { mutableStateOf(settings.secretDateConfig.day.toString()) }
        var yearInput by remember { mutableStateOf(settings.secretDateConfig.year?.toString().orEmpty()) }
        var secretPin by remember { mutableStateOf("") }
        var confirmSecretPin by remember { mutableStateOf("") }
        var enableOnSave by remember { mutableStateOf(true) }
        var configError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSecretDateDialog = false },
            title = { Text("Secret Date Unlock Setup") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select Secret Month",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(monthNames.indices.toList()) { idx ->
                            val monthNum = idx + 1
                            FilterChip(
                                selected = selectedMonth == monthNum,
                                onClick = { selectedMonth = monthNum },
                                label = { Text(monthNames[idx].take(3)) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = dayInput,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) dayInput = it },
                        label = { Text("Secret Day (1–31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = yearInput,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) yearInput = it },
                        label = { Text("Secret Year (Optional, e.g. 2026)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = secretPin,
                        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) secretPin = it },
                        label = {
                            Text(
                                if (hasSecretPin) "New Secret PIN (4–8 digits, optional)"
                                else "Secret PIN (4–8 digits)"
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = confirmSecretPin,
                        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) confirmSecretPin = it },
                        label = { Text("Confirm Secret PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Secret Date Unlock", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = enableOnSave, onCheckedChange = { enableOnSave = it })
                    }

                    configError?.let {
                        Text(it, color = CrimsonAlert, style = MaterialTheme.typography.labelMedium)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsedDay = dayInput.toIntOrNull()
                    val parsedYear = if (yearInput.isBlank()) null else yearInput.toIntOrNull()
                    if (parsedDay == null || parsedDay !in 1..31) {
                        configError = "Enter a valid day between 1 and 31"
                    } else if (yearInput.isNotBlank() && (parsedYear == null || yearInput.length != 4)) {
                        configError = "Optional year must be 4 digits (e.g. 2026)"
                    } else if (!SecretDateUnlockEvaluator.isValidCalendarDate(parsedDay, selectedMonth, parsedYear)) {
                        configError = "Selected day does not exist in ${monthNames[selectedMonth - 1]}"
                    } else if (!hasSecretPin && secretPin.length !in 4..8) {
                        configError = "Secret PIN must be 4–8 digits"
                    } else if (secretPin.isNotEmpty() && secretPin.length !in 4..8) {
                        configError = "Secret PIN must be 4–8 digits"
                    } else if (secretPin.isNotEmpty() && secretPin != confirmSecretPin) {
                        configError = "Secret PINs do not match"
                    } else {
                        repository.saveSecretDateConfiguration(
                            day = parsedDay,
                            month = selectedMonth,
                            year = parsedYear,
                            enabled = enableOnSave,
                            newSecretPin = secretPin.takeIf { it.isNotEmpty() }
                        )
                        showSecretDateDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecretDateDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text("Offline-First Privacy Policy") },
            text = {
                Text(
                    "SecureLock AI operates 100% offline. Credentials are salted and hashed with PBKDF2WithHmacSHA256, settings and vault files are encrypted with Android Keystore AES-256-GCM, camera access occurs strictly in the foreground with your consent, and zero private media or notifications leave your device."
                )
            },
            confirmButton = {
                Button(onClick = { showPrivacyPolicyDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun PermissionsManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var snapshot by remember { mutableStateOf(PermissionHelper.getSnapshot(context)) }
    val app = remember(context) { context.applicationContext as? com.securelock.ai.SecureLockApplication }

    fun openPermissionSettings(intent: android.content.Intent) {
        AppLockSessionManager.setInternalFlowSuspendedForPicker(true)
        runCatching { context.startActivity(intent) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        snapshot = PermissionHelper.getSnapshot(context)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        snapshot = PermissionHelper.getSnapshot(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                snapshot = PermissionHelper.getSnapshot(context)
                app?.let {
                    AppLockServiceController.syncMonitorService(context, it.storageRepository.settings.value)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SecureTopBar(
                title = "Permissions Manager",
                subtitle = "Android 8.0 – 16 special access",
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
                PermissionRowCard(
                    title = "Display Over Other Apps",
                    description = "Required on Android 10–16 to display the lock screen when a protected app opens.",
                    granted = snapshot.overlayGranted,
                    onConfigure = { openPermissionSettings(PermissionHelper.overlaySettingsIntent(context)) }
                )
            }
            item {
                PermissionRowCard(
                    title = "Usage Access (UsageStatsManager)",
                    description = "Detects which user-selected app opened in the foreground without reading window content.",
                    granted = snapshot.usageStatsGranted,
                    onConfigure = { openPermissionSettings(PermissionHelper.usageAccessSettingsIntent()) }
                )
            }
            item {
                PermissionRowCard(
                    title = "Accessibility Service (Instant Lock)",
                    description = "Receives TYPE_WINDOW_STATE_CHANGED only (canRetrieveWindowContent = false).",
                    granted = snapshot.accessibilityEnabled,
                    onConfigure = { openPermissionSettings(PermissionHelper.accessibilitySettingsIntent()) }
                )
            }
            item {
                PermissionRowCard(
                    title = "Notification Listener (Optional)",
                    description = "Allows local-only masking of sensitive notification previews for protected apps.",
                    granted = snapshot.notificationListenerEnabled,
                    onConfigure = { openPermissionSettings(PermissionHelper.notificationListenerSettingsIntent()) }
                )
            }
            item {
                PermissionRowCard(
                    title = "Battery Optimization Unrestricted (Recommended)",
                    description = "Prevents Android/OEM battery savers from stopping your lock monitor in the background.",
                    granted = snapshot.ignoringBatteryOptimizations,
                    onConfigure = { openPermissionSettings(PermissionHelper.batteryOptimizationSettingsIntent()) }
                )
            }
            item {
                PermissionRowCard(
                    title = "Foreground Camera (Optional)",
                    description = "Used only when you explicitly capture an intruder photo or run the Camera Privacy Test.",
                    granted = snapshot.cameraGranted,
                    onConfigure = { cameraLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    PermissionRowCard(
                        title = "Post Notifications (Android 13+)",
                        description = "Required if you start the foreground Usage Access monitor notification.",
                        granted = snapshot.postNotificationsGranted,
                        onConfigure = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRowCard(
    title: String,
    description: String,
    granted: Boolean,
    onConfigure: () -> Unit
) {
    WhiteSecurityCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    text = if (granted) "Granted" else "Not Granted",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (granted) EmeraldSafe else CrimsonAlert,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onConfigure) {
                Text(if (granted) "Review in Settings" else "Grant Permission")
            }
        }
    }
}
