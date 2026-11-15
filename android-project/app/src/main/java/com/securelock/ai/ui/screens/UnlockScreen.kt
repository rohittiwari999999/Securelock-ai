package com.securelock.ai.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.securelock.ai.data.LockMethod
import com.securelock.ai.security.BiometricAuthManager
import com.securelock.ai.security.BiometricAvailability
import com.securelock.ai.security.PermissionHelper
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.ui.components.PatternLockPad
import com.securelock.ai.ui.components.PinKeypad
import com.securelock.ai.ui.theme.CrimsonAlert
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

@Composable
fun UnlockScreen(
    activity: FragmentActivity,
    repository: SecureStorageRepository,
    targetPackageLabel: String,
    targetPackageName: String? = null,
    isSetupFlow: Boolean,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val settings by repository.settings.collectAsState()
    val configuredLength = remember(settings.customPinLength) {
        repository.configuredPinLength().coerceIn(4, 12)
    }

    val resolvedDefaultMethod = remember(targetPackageName, settings.preferredLockMethod, settings.perAppLockMethods) {
        if (isSetupFlow) LockMethod.PIN_4
        else repository.resolveMethodForPackage(targetPackageName)
    }

    var activeMethod by remember(resolvedDefaultMethod) {
        mutableStateOf(resolvedDefaultMethod)
    }
    var enteredPin by remember { mutableStateOf("") }
    var enteredPassword by remember { mutableStateOf("") }
    var setupFirstPin by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember {
        mutableStateOf(
            if (isSetupFlow) "Create a 4–12 digit master PIN (stored hashed with Android Keystore)"
            else "Verify your identity to access $targetPackageLabel"
        )
    }
    var isError by remember { mutableStateOf(false) }
    var consecutiveFailures by remember { mutableIntStateOf(0) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var showGeneratedRecoveryDialog by remember { mutableStateOf<String?>(null) }
    var showForegroundCapturePrompt by remember { mutableStateOf(false) }
    var lastIntruderId by remember { mutableStateOf<String?>(null) }

    val biometricAvailability = remember(context) {
        BiometricAuthManager.checkAvailability(context)
    }
    val canUseBiometric = !isSetupFlow &&
        settings.biometricEnabled &&
        biometricAvailability is BiometricAvailability.Available

    val handleFailedAttempt = { methodLabel: String ->
        consecutiveFailures += 1
        repository.recordAuthAttempt(false, methodLabel, targetPackageLabel)
        isError = true
        statusMessage = "$methodLabel not recognized ($consecutiveFailures failed attempt(s))"
        val threshold = settings.intruderAttemptThreshold.coerceIn(1, 10)
        if (consecutiveFailures >= threshold) {
            val rec = repository.addIntruderRecord(
                failedAttemptCount = consecutiveFailures,
                targetPackage = targetPackageLabel,
                methodAttempted = methodLabel,
                imagePath = null
            )
            lastIntruderId = rec.id
            if (settings.foregroundIntruderPromptEnabled) {
                showForegroundCapturePrompt = true
            }
        }
    }

    val triggerBiometric = {
        BiometricAuthManager.authenticate(
            activity = activity,
            title = "Unlock $targetPackageLabel",
            subtitle = "Use your enrolled fingerprint or biometric credential",
            onSuccess = {
                repository.recordAuthAttempt(true, "Biometric", targetPackageLabel)
                consecutiveFailures = 0
                onUnlocked()
            },
            onFailedAttempt = {
                handleFailedAttempt("Biometric")
            },
            onError = { msg ->
                isError = true
                statusMessage = msg
            }
        )
    }

    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric) {
            triggerBiometric()
        }
    }

    val bg = MaterialTheme.colorScheme.background
    val fg = MaterialTheme.colorScheme.onBackground
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.secondary

    val pulseTransition = rememberInfiniteTransition(label = "fp_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fp_scale"
    )

    val availableMethods = remember(
        isSetupFlow,
        settings.preferredLockMethod,
        repository.hasPinConfigured(),
        repository.hasPatternConfigured(),
        repository.hasPasswordConfigured()
    ) {
        if (isSetupFlow) emptyList()
        else buildList {
            if (repository.hasPinConfigured()) {
                val pinMethod = when (configuredLength) {
                    6 -> LockMethod.PIN_6
                    4 -> LockMethod.PIN_4
                    else -> LockMethod.PIN_CUSTOM
                }
                add(pinMethod to "PIN")
            }
            if (repository.hasPatternConfigured()) add(LockMethod.PATTERN to "Pattern")
            if (repository.hasPasswordConfigured()) add(LockMethod.PASSWORD to "Password")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = surfaceVariant,
                modifier = Modifier
                    .size(96.dp)
                    .scale(if (canUseBiometric) pulseScale else 1f)
                    .clickable(enabled = canUseBiometric) { triggerBiometric() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (canUseBiometric) Icons.Default.Fingerprint else Icons.Default.Lock,
                        contentDescription = "Fingerprint Unlock",
                        tint = accent,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isSetupFlow) "Set Up SecureLock AI" else "SecureLock AI",
                style = MaterialTheme.typography.headlineMedium,
                color = fg
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) CrimsonAlert else fg.copy(alpha = 0.78f),
                textAlign = TextAlign.Center
            )

            if (availableMethods.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableMethods.forEach { (method, label) ->
                        val isPinActive = activeMethod != LockMethod.PATTERN && activeMethod != LockMethod.PASSWORD &&
                            method != LockMethod.PATTERN && method != LockMethod.PASSWORD
                        val selected = activeMethod == method || isPinActive
                        Surface(
                            onClick = {
                                activeMethod = method
                                isError = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) primary else surfaceVariant
                        ) {
                            Text(
                                text = label,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else fg,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        when {
            activeMethod == LockMethod.PATTERN && !isSetupFlow -> {
                PatternLockPad(
                    onPatternComplete = { nodes ->
                        if (nodes.size < 4) {
                            isError = true
                            statusMessage = "Connect at least 4 nodes"
                        } else if (repository.verifyPattern(nodes)) {
                            repository.recordAuthAttempt(true, "Pattern", targetPackageLabel)
                            consecutiveFailures = 0
                            onUnlocked()
                        } else {
                            handleFailedAttempt("Pattern")
                        }
                    }
                )
            }

            activeMethod == LockMethod.PASSWORD && !isSetupFlow -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = enteredPassword,
                        onValueChange = { enteredPassword = it },
                        label = { Text("Alphanumeric Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (repository.verifyPassword(enteredPassword)) {
                                repository.recordAuthAttempt(true, "Password", targetPackageLabel)
                                enteredPassword = ""
                                consecutiveFailures = 0
                                onUnlocked()
                            } else {
                                enteredPassword = ""
                                handleFailedAttempt("Password")
                            }
                        },
                        enabled = enteredPassword.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unlock with Password")
                    }
                }
            }

            else -> {
                val maxDigits = if (isSetupFlow) 12 else configuredLength
                PinKeypad(
                    enteredLength = enteredPin.length,
                    maxLength = maxDigits,
                    onDigit = { digit ->
                        if (enteredPin.length < maxDigits) {
                            enteredPin += digit
                            isError = false
                            if (!isSetupFlow && enteredPin.length >= 4 && repository.verifyPin(enteredPin)) {
                                repository.recordAuthAttempt(true, "PIN", targetPackageLabel)
                                enteredPin = ""
                                consecutiveFailures = 0
                                onUnlocked()
                            } else if (!isSetupFlow && enteredPin.length == maxDigits) {
                                enteredPin = ""
                                handleFailedAttempt("PIN")
                            }
                        }
                    },
                    onBackspace = {
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                        }
                    },
                    onBiometricClick = if (canUseBiometric) triggerBiometric else null,
                    biometricIcon = if (canUseBiometric) Icons.Default.Fingerprint else null
                )

                if (isSetupFlow) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (enteredPin.length < 4) {
                                isError = true
                                statusMessage = "PIN must be 4 to 12 digits"
                            } else if (setupFirstPin == null) {
                                setupFirstPin = enteredPin
                                enteredPin = ""
                                isError = false
                                statusMessage = "Re-enter your ${setupFirstPin!!.length}-digit PIN to confirm"
                            } else if (setupFirstPin == enteredPin) {
                                repository.setPin(enteredPin)
                                val recovery = repository.generateAndStoreRecoveryCode()
                                enteredPin = ""
                                showGeneratedRecoveryDialog = recovery
                            } else {
                                setupFirstPin = null
                                enteredPin = ""
                                isError = true
                                statusMessage = "PINs did not match. Enter a new PIN."
                            }
                        },
                        enabled = enteredPin.length >= 4,
                        colors = ButtonDefaults.buttonColors(containerColor = primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (setupFirstPin == null) "Continue" else "Confirm & Save PIN")
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            if (!isSetupFlow && repository.hasRecoveryCodeConfigured()) {
                TextButton(onClick = { showRecoveryDialog = true }) {
                    Text("Forgot PIN? Use Emergency Recovery", color = accent)
                }
            }
            if (canUseBiometric) {
                OutlinedButton(
                    onClick = triggerBiometric,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("  Prompt Biometric Scanner", color = fg)
                }
            }
        }
    }

    if (showGeneratedRecoveryDialog != null) {
        val code = showGeneratedRecoveryDialog!!
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Save Emergency Recovery Code") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Store this one-time recovery key safely offline. SecureLock AI cannot recover your PIN without it.")
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = code,
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showGeneratedRecoveryDialog = null
                    onUnlocked()
                }) {
                    Text("I Saved It")
                }
            }
        )
    }

    if (showRecoveryDialog) {
        var recoveryInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var recoveryError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text("Emergency Credential Recovery") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = recoveryInput,
                        onValueChange = { recoveryInput = it },
                        label = { Text("Recovery Code (XXXX-XXXX-XXXX)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 12 && it.all(Char::isDigit)) newPinInput = it },
                        label = { Text("New 4–12 Digit PIN") },
                        singleLine = true
                    )
                    recoveryError?.let {
                        Text(it, color = CrimsonAlert, style = MaterialTheme.typography.labelMedium)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPinInput.length < 4) {
                        recoveryError = "New PIN must be 4–12 digits"
                    } else if (repository.verifyRecoveryCode(recoveryInput)) {
                        repository.setPin(newPinInput)
                        showRecoveryDialog = false
                        onUnlocked()
                    } else {
                        recoveryError = "Invalid recovery code"
                    }
                }) {
                    Text("Reset PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showForegroundCapturePrompt && lastIntruderId != null) {
        ForegroundIntruderCaptureDialog(
            repository = repository,
            intruderRecordId = lastIntruderId!!,
            onDismiss = { showForegroundCapturePrompt = false }
        )
    }
}

/**
 * Explicit, visible, permission-based foreground camera capture prompt.
 * Never activates the camera silently or in the background.
 */
@Composable
private fun ForegroundIntruderCaptureDialog(
    repository: SecureStorageRepository,
    intruderRecordId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraGranted by remember { mutableStateOf(PermissionHelper.hasCameraPermission(context)) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Failed Attempts Recorded") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Multiple failed attempts reached your configured threshold. Android requires an explicit foreground capture—you may save an encrypted verification photo now or skip."
                )
                if (cameraGranted) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val future = ProcessCameraProvider.getInstance(ctx)
                            future.addListener({
                                val provider = future.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                runCatching {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_FRONT_CAMERA,
                                        preview,
                                        imageCapture
                                    )
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        }
                    )
                } else {
                    OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Text("  Grant Foreground Camera Permission")
                    }
                }
            }
        },
        confirmButton = {
            if (cameraGranted) {
                Button(onClick = {
                    val tempFile = File(context.cacheDir, "tmp_cap_${UUID.randomUUID()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                val bytes = runCatching { tempFile.readBytes() }.getOrNull()
                                runCatching { tempFile.delete() }
                                if (bytes != null) {
                                    val encryptedPath = repository.saveEncryptedIntruderImage(bytes)
                                    repository.attachIntruderPhoto(intruderRecordId, encryptedPath)
                                }
                                onDismiss()
                            }

                            override fun onError(exception: ImageCaptureException) {
                                runCatching { tempFile.delete() }
                                onDismiss()
                            }
                        }
                    )
                }) {
                    Text("Capture & Encrypt")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Skip") }
        }
    )
}
