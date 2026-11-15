package com.securelock.ai.ui.screens

import android.Manifest
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.securelock.ai.data.IntruderRecord
import com.securelock.ai.security.PermissionHelper
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.ui.components.SecureTopBar
import com.securelock.ai.ui.components.WhiteSecurityCard
import com.securelock.ai.ui.theme.CrimsonAlert
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun IntruderSelfieScreen(
    repository: SecureStorageRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by repository.settings.collectAsState()
    val intruders by repository.intruders.collectAsState()

    var cameraGranted by remember { mutableStateOf(PermissionHelper.hasCameraPermission(context)) }
    var showLiveCaptureDialog by remember { mutableStateOf(false) }
    var previewRecord by remember { mutableStateOf<IntruderRecord?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
        if (granted) showLiveCaptureDialog = true
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            SecureTopBar(
                title = "Intruder Detection",
                subtitle = "Threshold: ${settings.intruderAttemptThreshold} attempts · Encrypted",
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
                                    text = "Foreground Intruder Capture",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Offers a visible foreground camera prompt when failed attempts reach threshold. Never secretly activates camera.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.foregroundIntruderPromptEnabled,
                                onCheckedChange = { enabled ->
                                    repository.updateSettings {
                                        it.copy(foregroundIntruderPromptEnabled = enabled)
                                    }
                                }
                            )
                        }

                        Text(
                            text = "Attempt Threshold",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3, 5).forEach { count ->
                                FilterChip(
                                    selected = settings.intruderAttemptThreshold == count,
                                    onClick = {
                                        repository.updateSettings {
                                            it.copy(intruderAttemptThreshold = count)
                                        }
                                    },
                                    label = { Text("$count ${if (count == 1) "try" else "tries"}") }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (cameraGranted) showLiveCaptureDialog = true
                                else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Text("  Test Foreground Capture")
                        }
                    }
                }
            }

            if (intruders.isEmpty()) {
                item {
                    WhiteSecurityCard {
                        Text(
                            text = "No failed-attempt intruder events recorded yet. Captured snapshots are encrypted via Android Keystore inside private app storage.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(intruders, key = { it.id }) { record ->
                    val formattedTime = remember(record.timestampMillis) {
                        SimpleDateFormat("MMM dd, yyyy · HH:mm:ss", Locale.getDefault())
                            .format(Date(record.timestampMillis))
                    }
                    WhiteSecurityCard(
                        onClick = {
                            if (record.imagePath != null) previewRecord = record
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${record.failedAttemptCount} Failed Attempt(s) · ${record.methodAttempted}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CrimsonAlert
                                )
                                Text(
                                    text = "$formattedTime · Target: ${record.targetPackage}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (record.imagePath != null) "Encrypted private photo attached (tap to view)" else "Event log only",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { repository.deleteIntruderRecord(record) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete record",
                                    tint = CrimsonAlert
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLiveCaptureDialog) {
        val imageCapture = remember { ImageCapture.Builder().build() }
        AlertDialog(
            onDismissRequest = { showLiveCaptureDialog = false },
            title = { Text("Foreground Camera Capture") },
            text = {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp)),
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
            },
            confirmButton = {
                Button(onClick = {
                    val tempFile = File(context.cacheDir, "tmp_int_${UUID.randomUUID()}.jpg")
                    val opts = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                    imageCapture.takePicture(
                        opts,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                val bytes = runCatching { tempFile.readBytes() }.getOrNull()
                                runCatching { tempFile.delete() }
                                if (bytes != null) {
                                    val encryptedPath = repository.saveEncryptedIntruderImage(bytes)
                                    repository.addIntruderRecord(
                                        failedAttemptCount = 1,
                                        targetPackage = context.packageName,
                                        methodAttempted = "Foreground Test",
                                        imagePath = encryptedPath
                                    )
                                }
                                showLiveCaptureDialog = false
                            }

                            override fun onError(exception: ImageCaptureException) {
                                runCatching { tempFile.delete() }
                                showLiveCaptureDialog = false
                            }
                        }
                    )
                }) {
                    Text("Encrypt & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLiveCaptureDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (previewRecord != null) {
        val rec = previewRecord!!
        val bitmap = remember(rec.id) {
            val plain = repository.decryptIntruderImage(rec.imagePath)
            plain?.let {
                val bmp = BitmapFactory.decodeByteArray(it, 0, it.size)
                it.fill(0)
                bmp
            }
        }
        AlertDialog(
            onDismissRequest = { previewRecord = null },
            title = { Text("Encrypted Intruder Snapshot") },
            text = {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured snapshot",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                } else {
                    Text("Encrypted image file is no longer available.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteIntruderRecord(rec)
                        previewRecord = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert)
                ) {
                    Text("Delete Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { previewRecord = null }) { Text("Close") }
            }
        )
    }
}
