package com.securelock.ai

import android.Manifest
import android.content.Intent
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.securelock.ai.security.PermissionHelper
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.service.AppLockServiceController
import com.securelock.ai.service.AppLockSessionManager
import com.securelock.ai.ui.components.PatternLockPad
import com.securelock.ai.ui.theme.CrimsonAlert
import com.securelock.ai.ui.theme.SecureLockTheme
import java.io.File
import java.util.UUID

/**
 * Dedicated FLAG_SECURE Pattern Lock Activity shown when a protected app opens.
 * Uses Lock Pattern ONLY (no Unlock Gate, no Decoy Gate, no competing PIN/Biometric overlays).
 */
class LockOverlayActivity : FragmentActivity() {

    private var activeTargetPackage by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()

        activeTargetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: ""
        if (activeTargetPackage.isBlank() || activeTargetPackage == packageName) {
            finish()
            return
        }

        val repository = (application as SecureLockApplication).storageRepository

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
                AppPatternLockOverlay(
                    repository = repository,
                    targetLabel = targetLabel,
                    onUnlocked = {
                        AppLockSessionManager.markAppUnlocked(
                            packageName = targetPackage,
                            timeoutSeconds = settings.relockTimeoutSeconds
                        )
                        finish()
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

    private fun exitToHomeScreen() {
        AppLockSessionManager.setOverlayActive(null)
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(homeIntent) }
        finish()
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
    }
}

@Composable
private fun AppPatternLockOverlay(
    repository: SecureStorageRepository,
    targetLabel: String,
    onUnlocked: () -> Unit,
    onCancelHome: () -> Unit
) {
    val settings by repository.settings.collectAsState()
    var hasPattern by remember { mutableStateOf(repository.hasPatternConfigured()) }
    var firstSetupPattern by remember { mutableStateOf<List<Int>?>(null) }
    var isError by remember { mutableStateOf(false) }
    var consecutiveFailures by remember { mutableIntStateOf(0) }
    var statusMessage by remember(hasPattern, targetLabel) {
        mutableStateOf(
            if (hasPattern) "Draw your lock pattern to open $targetLabel"
            else "Create a lock pattern (4+ dots) to protect $targetLabel"
        )
    }
    var showForegroundCapturePrompt by remember { mutableStateOf(false) }
    var lastIntruderId by remember { mutableStateOf<String?>(null) }

    val bg = MaterialTheme.colorScheme.background
    val fg = MaterialTheme.colorScheme.onBackground
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = surfaceVariant,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
        }

        PatternLockPad(
            onPatternComplete = { nodes ->
                if (!hasPattern) {
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
                                biometricEnabled = false
                            )
                        }
                        if (!repository.hasRecoveryCodeConfigured()) {
                            repository.generateAndStoreRecoveryCode()
                        }
                        hasPattern = true
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
                        consecutiveFailures += 1
                        repository.recordAuthAttempt(false, "Pattern", targetLabel)
                        isError = true
                        statusMessage = "Incorrect pattern ($consecutiveFailures failed attempt(s))"
                        val threshold = settings.intruderAttemptThreshold.coerceIn(1, 10)
                        if (consecutiveFailures >= threshold) {
                            val rec = repository.addIntruderRecord(
                                failedAttemptCount = consecutiveFailures,
                                targetPackage = targetLabel,
                                methodAttempted = "Pattern",
                                imagePath = null
                            )
                            lastIntruderId = rec.id
                            if (settings.foregroundIntruderPromptEnabled) {
                                showForegroundCapturePrompt = true
                            }
                        }
                    }
                }
            }
        )

        TextButton(onClick = onCancelHome) {
            Text(
                text = "Return to Home Screen",
                color = accent
            )
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
                    "Multiple wrong pattern attempts reached your threshold. Capture an encrypted verification photo now or skip."
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
