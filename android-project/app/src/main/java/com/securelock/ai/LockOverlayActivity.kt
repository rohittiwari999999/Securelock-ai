package com.securelock.ai

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.securelock.ai.service.AppLockServiceController
import com.securelock.ai.service.AppLockSessionManager
import com.securelock.ai.ui.components.NeuglassBackdrop
import com.securelock.ai.ui.components.PatternLockPad
import com.securelock.ai.ui.components.PinKeypad
import com.securelock.ai.ui.components.neuglassSurface
import com.securelock.ai.ui.theme.CrimsonAlert
import com.securelock.ai.ui.theme.SecureLockTheme
import java.io.File
import java.util.UUID

private enum class ActiveOverlayMode {
    PATTERN,
    PIN
}

/**
 * Single FLAG_SECURE Lock Overlay Activity shown when a protected app opens.
 * Supports Fingerprint (BiometricPrompt), PIN, and Pattern together cleanly
 * without conflicting decoy gates or duplicate overlays.
 */
class LockOverlayActivity : FragmentActivity() {

    private var activeTargetPackage by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()

        val repository = (application as SecureLockApplication).storageRepository
        activeTargetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: ""
        if (activeTargetPackage.isBlank() || activeTargetPackage == packageName) {
            finishOverlayQuietly()
            return
        }

        setContent {
            val settings by repository.settings.collectAsState()
            val targetPackage = activeTargetPackage
            val targetLabel = remember(targetPackage) {
                AppLockServiceController.resolveAppLabel(this, targetPackage)
            }

            BackHandler {
                exitToHomeScreen()
            }

            SecureLockTheme(
                themeId = settings.themeId,
                customAccentHex = settings.customAccentHex,
                forceLightMode = settings.forceLightMode,
                cardStyleMode = settings.cardStyleMode
            ) {
                UnifiedAppLockOverlay(
                    activity = this,
                    repository = repository,
                    targetPackage = targetPackage,
                    targetLabel = targetLabel,
                    onUnlocked = {
                        AppLockSessionManager.markAppUnlocked(
                            packageName = targetPackage,
                            timeoutSeconds = settings.relockTimeoutSeconds
                        )
                        revealUnlockedTargetApp(targetPackage)
                    },
                    onCancelHome = { exitToHomeScreen() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (activeTargetPackage.isNotBlank()) {
            AppLockSessionManager.setOverlayActive(activeTargetPackage)
        }
    }

    override fun onStop() {
        AppLockSessionManager.setOverlayActive(null)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val nextPkg = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        if (!nextPkg.isNullOrBlank() && nextPkg != packageName) {
            activeTargetPackage = nextPkg
            AppLockSessionManager.setOverlayActive(nextPkg)
        }
    }

    private fun revealUnlockedTargetApp(targetPkg: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(targetPkg)?.apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        }
        if (launchIntent != null) {
            runCatching { startActivity(launchIntent) }
        }
        finishOverlayQuietly()
    }

    private fun finishOverlayQuietly() {
        finishAndRemoveTask()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun exitToHomeScreen() {
        if (activeTargetPackage.isNotBlank()) {
            AppLockSessionManager.noteOverlayDismissedToHome(activeTargetPackage)
        } else {
            AppLockSessionManager.setOverlayActive(null)
        }
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        runCatching { startActivity(homeIntent) }
        finishOverlayQuietly()
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
    }
}

@Composable
private fun UnifiedAppLockOverlay(
    activity: FragmentActivity,
    repository: SecureStorageRepository,
    targetPackage: String,
    targetLabel: String,
    onUnlocked: () -> Unit,
    onCancelHome: () -> Unit
) {
    val settings by repository.settings.collectAsState()
    val hasPattern = remember { repository.hasPatternConfigured() }
    val hasPin = remember { repository.hasPinConfigured() }
    val hasAnyCredential = hasPattern || hasPin

    val bioAvailability = remember { BiometricAuthManager.checkAvailability(activity) }
    val fingerprintReady = settings.biometricEnabled && bioAvailability is BiometricAvailability.Available

    val initialMode = remember(targetPackage, settings.preferredLockMethod, hasPattern, hasPin) {
        val preferred = repository.resolveMethodForPackage(targetPackage)
        when {
            preferred == LockMethod.PATTERN && hasPattern -> ActiveOverlayMode.PATTERN
            (preferred == LockMethod.PIN_4 || preferred == LockMethod.PIN_6 || preferred == LockMethod.PIN_CUSTOM) && hasPin ->
                ActiveOverlayMode.PIN
            hasPattern -> ActiveOverlayMode.PATTERN
            hasPin -> ActiveOverlayMode.PIN
            else -> ActiveOverlayMode.PATTERN
        }
    }

    var activeMode by remember(targetPackage) { mutableStateOf(initialMode) }
    var enteredPin by remember(targetPackage) { mutableStateOf("") }
    var firstSetupPattern by remember(targetPackage) { mutableStateOf<List<Int>?>(null) }
    var patternConfiguredNow by remember { mutableStateOf(hasPattern) }
    var isError by remember(targetPackage) { mutableStateOf(false) }
    var consecutiveFailures by remember(targetPackage) { mutableIntStateOf(0) }
    var statusMessage by remember(targetPackage, activeMode, hasAnyCredential) {
        mutableStateOf(
            when {
                !hasAnyCredential -> "Draw a Lock Pattern (4+ dots) to protect $targetLabel"
                activeMode == ActiveOverlayMode.PATTERN -> "Draw pattern or use fingerprint to open $targetLabel"
                else -> "Enter PIN or use fingerprint to open $targetLabel"
            }
        )
    }
    var showForegroundCapturePrompt by remember { mutableStateOf(false) }
    var lastIntruderId by remember { mutableStateOf<String?>(null) }

    fun handleFailedAttempt(methodName: String, customText: String) {
        consecutiveFailures += 1
        repository.recordAuthAttempt(false, methodName, targetLabel)
        isError = true
        statusMessage = "$customText ($consecutiveFailures failed attempt(s))"
        val threshold = settings.intruderAttemptThreshold.coerceIn(1, 10)
        if (consecutiveFailures >= threshold) {
            val rec = repository.addIntruderRecord(
                failedAttemptCount = consecutiveFailures,
                targetPackage = targetLabel,
                methodAttempted = methodName,
                imagePath = null
            )
            lastIntruderId = rec.id
            if (settings.foregroundIntruderPromptEnabled) {
                showForegroundCapturePrompt = true
            }
        }
    }

    fun triggerFingerprint() {
        if (!fingerprintReady) return
        BiometricAuthManager.authenticate(
            activity = activity,
            title = "Unlock $targetLabel",
            subtitle = "Confirm fingerprint to continue",
            negativeButtonText = "Use Pattern / PIN",
            onSuccess = {
                isError = false
                consecutiveFailures = 0
                repository.recordAuthAttempt(true, "Fingerprint", targetLabel)
                onUnlocked()
            },
            onFailedAttempt = {
                handleFailedAttempt("Fingerprint", "Fingerprint not recognized")
            },
            onError = { err ->
                isError = true
                statusMessage = err
            }
        )
    }

    // Launch BiometricPrompt cleanly once when Fingerprint is enabled (Pattern/PIN remain ready underneath)
    LaunchedEffect(targetPackage, fingerprintReady, hasAnyCredential) {
        if (fingerprintReady && hasAnyCredential) {
            triggerFingerprint()
        }
    }

    val bg = MaterialTheme.colorScheme.background
    val fg = MaterialTheme.colorScheme.onBackground
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.secondary

    val configuredPinLen = remember { repository.configuredPinLength().coerceIn(4, 12) }

    NeuglassBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .neuglassSurface(
                            cornerRadius = 40.dp,
                            surfaceColor = surfaceVariant,
                            accentColor = accent,
                            isCircular = true,
                            elevationScale = 1.05f
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(38.dp)
                    )
                }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = targetLabel,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = fg,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) CrimsonAlert else fg.copy(alpha = 0.78f),
                textAlign = TextAlign.Center
            )

            if (hasAnyCredential && (hasPattern || hasPin || fingerprintReady)) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasPattern || patternConfiguredNow) {
                        FilterChip(
                            selected = activeMode == ActiveOverlayMode.PATTERN,
                            onClick = {
                                activeMode = ActiveOverlayMode.PATTERN
                                isError = false
                                statusMessage = "Draw pattern to open $targetLabel"
                            },
                            label = { Text("Pattern") }
                        )
                    }
                    if (hasPin) {
                        FilterChip(
                            selected = activeMode == ActiveOverlayMode.PIN,
                            onClick = {
                                activeMode = ActiveOverlayMode.PIN
                                enteredPin = ""
                                isError = false
                                statusMessage = "Enter PIN to open $targetLabel"
                            },
                            label = { Text("PIN") }
                        )
                    }
                    if (fingerprintReady) {
                        FilterChip(
                            selected = false,
                            onClick = { triggerFingerprint() },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Fingerprint") }
                        )
                    }
                }
            }
        }

        if (activeMode == ActiveOverlayMode.PIN && hasPin) {
            PinKeypad(
                enteredLength = enteredPin.length,
                maxLength = configuredPinLen,
                onDigit = { digit ->
                    if (enteredPin.length < 12) {
                        val next = enteredPin + digit
                        enteredPin = next
                        isError = false
                        if (repository.verifyPin(next)) {
                            enteredPin = ""
                            consecutiveFailures = 0
                            repository.recordAuthAttempt(true, "PIN", targetLabel)
                            onUnlocked()
                        } else if (next.length >= configuredPinLen) {
                            enteredPin = ""
                            handleFailedAttempt("PIN", "Incorrect PIN")
                        }
                    }
                },
                onBackspace = {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                    }
                },
                onBiometricClick = if (fingerprintReady) {
                    { triggerFingerprint() }
                } else null,
                biometricIcon = if (fingerprintReady) Icons.Default.Fingerprint else null
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PatternLockPad(
                    onPatternComplete = { nodes ->
                        if (!hasAnyCredential && !patternConfiguredNow) {
                            if (nodes.size < 4) {
                                isError = true
                                statusMessage = "Connect at least 4 dots"
                            } else if (firstSetupPattern == null) {
                                firstSetupPattern = nodes
                                isError = false
                                statusMessage = "Draw the same pattern again to confirm"
                            } else if (firstSetupPattern == nodes) {
                                repository.setPattern(nodes)
                                repository.updateSettings {
                                    it.copy(
                                        preferredLockMethod = LockMethod.PATTERN,
                                        protectionScreenConfig = it.protectionScreenConfig.copy(enabled = false)
                                    )
                                }
                                if (!repository.hasRecoveryCodeConfigured()) {
                                    repository.generateAndStoreRecoveryCode()
                                }
                                patternConfiguredNow = true
                                firstSetupPattern = null
                                isError = false
                                repository.recordAuthAttempt(true, "Pattern", targetLabel)
                                onUnlocked()
                            } else {
                                firstSetupPattern = null
                                isError = true
                                statusMessage = "Patterns did not match. Draw again."
                            }
                        } else {
                            if (nodes.size < 4) {
                                isError = true
                                statusMessage = "Connect at least 4 dots"
                            } else if (repository.verifyPattern(nodes)) {
                                isError = false
                                consecutiveFailures = 0
                                repository.recordAuthAttempt(true, "Pattern", targetLabel)
                                onUnlocked()
                            } else {
                                handleFailedAttempt("Pattern", "Incorrect pattern")
                            }
                        }
                    }
                )
                if (fingerprintReady) {
                    OutlinedButton(onClick = { triggerFingerprint() }) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Text("  Unlock with Fingerprint")
                    }
                }
            }
        }

            TextButton(onClick = onCancelHome) {
                Text(
                    text = "Return to Home Screen",
                    color = accent
                )
            }
        }
    }

    if (showForegroundCapturePrompt && lastIntruderId != null) {
        OverlayIntruderCaptureDialog(
            repository = repository,
            intruderRecordId = lastIntruderId!!,
            onDismiss = { showForegroundCapturePrompt = false }
        )
    }
}

@Composable
private fun OverlayIntruderCaptureDialog(
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
                    "Multiple wrong attempts reached your threshold. Capture an encrypted verification photo now or skip."
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
                        Text("  Grant Camera Permission")
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
