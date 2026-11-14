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
import androidx.compose.foundation.text.KeyboardOptions
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
import com.securelock.ai.security.SecureStorageRepository
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
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var regeneratedRecovery by remember { mutableStateOf<String?>(null) }
    var backupStatus by remember { mutableStateOf<String?>(null) }

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
                        Text("Local Backup, Permissions & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        backupStatus?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium, color = EmeraldSafe)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { exportBackupLauncher.launch("securelock_local_backup.json") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Export Backup")
                            }
                            OutlinedButton(onClick = { importBackupLauncher.launch(arrayOf("application/json")) }) {
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
                    onConfigure = { context.startActivity(PermissionHelper.overlaySettingsIntent(context)) }
                )
            }
            item {
                PermissionRowCard(
                    title = "Usage Access (UsageStatsManager)",
                    description = "Detects which user-selected app opened in the foreground without reading window content.",
                    granted = snapshot.usageStatsGranted,
                    onConfigure = { context.startActivity(PermissionHelper.usageAccessSettingsIntent()) }
                )
            }
            item {
                PermissionRowCard(
                    title = "Accessibility Service (Optional Alternative)",
                    description = "Receives TYPE_WINDOW_STATE_CHANGED only (canRetrieveWindowContent = false).",
                    granted = snapshot.accessibilityEnabled,
                    onConfigure = { context.startActivity(PermissionHelper.accessibilitySettingsIntent()) }
                )
            }
            item {
                PermissionRowCard(
                    title = "Notification Listener (Optional)",
                    description = "Allows local-only masking of sensitive notification previews for protected apps.",
                    granted = snapshot.notificationListenerEnabled,
                    onConfigure = { context.startActivity(PermissionHelper.notificationListenerSettingsIntent()) }
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
                        description = "Required if you start the optional foreground Usage Access monitor notification.",
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
