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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.securelock.ai.ui.theme.DeepNavy
import com.securelock.ai.ui.theme.ElectricBlue
import com.securelock.ai.ui.theme.SlateText
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
        containerColor = DeepNavy,
        topBar = {
            SecureTopBar(
                title = "Intruder Selfie",
                subtitle = "Foreground-only & private storage",
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
                                    text = "Foreground Intruder Prompt",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Offers an explicit camera prompt after 3 failed attempts. Never captures secretly or in background.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateText
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

                        Button(
                            onClick = {
                                if (cameraGranted) showLiveCaptureDialog = true
                                else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
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
                            text = "No failed-attempt intruder events recorded yet. All captured photos stay inside private app storage (filesDir/intruders) and never appear in the public gallery.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateText
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
                                    text = "${record.failedAttemptCount} Failed Attempts",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CrimsonAlert
                                )
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SlateText
                                )
                                Text(
                                    text = if (record.imagePath != null) "Private photo saved" else "Log entry only (no photo)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateText
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
                    val file = File(repository.getIntruderStorageDir(), "intruder_${UUID.randomUUID()}.jpg")
                    val opts = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture.takePicture(
                        opts,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                repository.addIntruderRecord(
                                    failedAttemptCount = 1,
                                    targetPackage = context.packageName,
                                    imagePath = file.absolutePath
                                )
                                showLiveCaptureDialog = false
                            }

                            override fun onError(exception: ImageCaptureException) {
                                showLiveCaptureDialog = false
                            }
                        }
                    )
                }) {
                    Text("Save Privately")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLiveCaptureDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (previewRecord != null) {
        val rec = previewRecord!!
        val bitmap = remember(rec.imagePath) {
            rec.imagePath?.let { BitmapFactory.decodeFile(it) }
        }
        AlertDialog(
            onDismissRequest = { previewRecord = null },
            title = { Text("Private Intruder Snapshot") },
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
                    Text("Image file is no longer available.")
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
                    Text("Delete Photo")
                }
            },
            dismissButton = {
                TextButton(onClick = { previewRecord = null }) {
                    Text("Close")
                }
            }
        )
    }
}
