package com.securelock.ai.ui.screens

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.securelock.ai.security.PermissionHelper
import com.securelock.ai.ui.components.SecureTopBar
import com.securelock.ai.ui.components.WhiteSecurityCard
import com.securelock.ai.ui.theme.CrimsonAlert
import com.securelock.ai.ui.theme.ElectricBlue
import com.securelock.ai.ui.theme.EmeraldSafe

@Composable
fun CameraPrivacyMonitorScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraGranted by remember { mutableStateOf(PermissionHelper.hasCameraPermission(context)) }
    var testPreviewActive by remember { mutableStateOf(false) }

    val cameraStates = remember { mutableStateMapOf<String, Boolean>() }
    val cameraManager = remember(context) {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
    }

    DisposableEffect(cameraManager) {
        val handler = Handler(Looper.getMainLooper())
        val callback = object : CameraManager.AvailabilityCallback() {
            override fun onCameraAvailable(cameraId: String) {
                cameraStates[cameraId] = true
            }

            override fun onCameraUnavailable(cameraId: String) {
                cameraStates[cameraId] = false
            }
        }
        cameraManager.registerAvailabilityCallback(callback, handler)
        onDispose {
            cameraManager.unregisterAvailabilityCallback(callback)
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            SecureTopBar(
                title = "Camera Privacy Monitor",
                subtitle = "Supported hardware & OS indicators",
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
                            text = "Camera Permission & OS Privacy Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (cameraGranted) {
                                "Foreground Camera Permission: Granted (used only when you launch a live test or explicit capture)."
                            } else {
                                "Foreground Camera Permission: Denied. SecureLock AI cannot access your camera."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (cameraGranted) EmeraldSafe else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                "Android ${Build.VERSION.RELEASE} enforces hardware green privacy indicators whenever any app activates a camera. SecureLock AI respects and never bypasses OS indicators."
                            } else {
                                "Android CameraManager availability callbacks report live camera hardware availability."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!cameraGranted) {
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                                ) {
                                    Text("Grant Camera")
                                }
                            } else {
                                Button(
                                    onClick = { testPreviewActive = !testPreviewActive },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                                ) {
                                    Text(if (testPreviewActive) "Stop Camera Test" else "Run Camera Test")
                                }
                            }
                            OutlinedButton(onClick = {
                                context.startActivity(PermissionHelper.systemPrivacySettingsIntent())
                            }) {
                                Text("System Privacy")
                            }
                        }
                    }
                }
            }

            if (testPreviewActive && cameraGranted) {
                item {
                    WhiteSecurityCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "User-Controlled Camera Self-Test",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Observe the Android system green camera indicator in the status bar while preview is open.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AndroidView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
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
                                                preview
                                            )
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                    previewView
                                }
                            )
                        }
                    }
                }
            }

            items(cameraStates.entries.toList(), key = { it.key }) { (cameraId, available) ->
                WhiteSecurityCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Camera Sensor #$cameraId", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (available) "Idle & Available" else "In Use by Active Foreground Process",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (available) EmeraldSafe else CrimsonAlert
                            )
                        }
                    }
                }
            }
        }
    }
}
