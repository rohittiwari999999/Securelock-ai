package com.securelock.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build
import com.securelock.ai.R
import com.securelock.ai.data.CardStyleMode
import com.securelock.ai.security.PermissionHelper
import com.securelock.ai.ui.theme.AmberWarn
import com.securelock.ai.ui.theme.CrimsonAlert
import com.securelock.ai.ui.theme.EmeraldSafe
import com.securelock.ai.ui.theme.LocalCardStyleMode
import com.securelock.ai.ui.theme.LocalThemePalette
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    val bg = MaterialTheme.colorScheme.background
    val fg = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.secondary

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = bg,
            titleContentColor = fg,
            navigationIconContentColor = fg,
            actionIconContentColor = fg
        ),
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = fg
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                }
            }
        },
        actions = { actions() }
    )
}

@Composable
fun WhiteSecurityCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardStyle = LocalCardStyleMode.current
    val shape = when (cardStyle) {
        CardStyleMode.ELEVATED -> RoundedCornerShape(20.dp)
        CardStyleMode.OUTLINED -> RoundedCornerShape(18.dp)
        CardStyleMode.COMPACT -> RoundedCornerShape(14.dp)
    }
    val elevation = when (cardStyle) {
        CardStyleMode.ELEVATED -> CardDefaults.cardElevation(defaultElevation = 2.dp)
        CardStyleMode.OUTLINED -> CardDefaults.cardElevation(defaultElevation = 0.dp)
        CardStyleMode.COMPACT -> CardDefaults.cardElevation(defaultElevation = 1.dp)
    }
    val border = if (cardStyle == CardStyleMode.OUTLINED) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    } else null
    val pad = if (cardStyle == CardStyleMode.COMPACT) 12.dp else 16.dp

    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = cardColors,
            elevation = elevation,
            border = border
        ) {
            Box(modifier = Modifier.padding(pad)) {
                content()
            }
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = cardColors,
            elevation = elevation,
            border = border
        ) {
            Box(modifier = Modifier.padding(pad)) {
                content()
            }
        }
    }
}

@Composable
fun PinKeypad(
    enteredLength: Int,
    maxLength: Int = 6,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometricClick: (() -> Unit)? = null,
    biometricIcon: ImageVector? = null
) {
    val haptic = LocalHapticFeedback.current
    val fg = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.secondary
    val primary = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(maxLength.coerceAtMost(12)) { index ->
                val filled = index < enteredLength
                Box(
                    modifier = Modifier
                        .size(if (maxLength > 8) 10.dp else 14.dp)
                        .clip(CircleShape)
                        .background(if (filled) accent else fg.copy(alpha = 0.25f))
                )
            }
        }

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )

        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { digit ->
                    PinKeyButton(
                        label = digit,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDigit(digit)
                        }
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBiometricClick != null && biometricIcon != null) {
                Surface(
                    onClick = onBiometricClick,
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    color = primary.copy(alpha = 0.25f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = biometricIcon,
                            contentDescription = "Biometric unlock",
                            tint = accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(68.dp))
            }

            PinKeyButton(
                label = "0",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDigit("0")
                }
            )

            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackspace()
                },
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                color = fg.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Delete digit",
                        tint = fg
                    )
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    label: String,
    onClick: () -> Unit
) {
    val fg = MaterialTheme.colorScheme.onBackground
    Surface(
        onClick = onClick,
        modifier = Modifier.size(68.dp),
        shape = CircleShape,
        color = fg.copy(alpha = 0.12f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
        }
    }
}

@Composable
fun PatternLockPad(
    modifier: Modifier = Modifier,
    onPatternComplete: (List<Int>) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val selectedNodes = remember { mutableStateListOf<Int>() }
    var activePointer by remember { mutableStateOf<Offset?>(null) }
    val palette = LocalThemePalette.current
    val fg = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .size(264.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        selectedNodes.clear()
                        activePointer = startOffset
                        findNearestNode(startOffset, size.width.toFloat(), size.height.toFloat())?.let { node ->
                            selectedNodes.add(node)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        activePointer = change.position
                        findNearestNode(change.position, size.width.toFloat(), size.height.toFloat())?.let { node ->
                            if (!selectedNodes.contains(node)) {
                                selectedNodes.add(node)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                    onDragEnd = {
                        activePointer = null
                        if (selectedNodes.isNotEmpty()) {
                            onPatternComplete(selectedNodes.toList())
                        }
                        selectedNodes.clear()
                    },
                    onDragCancel = {
                        activePointer = null
                        selectedNodes.clear()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centers = nodeCenters(size.width, size.height)

            if (selectedNodes.isNotEmpty()) {
                val path = Path()
                val first = centers[selectedNodes.first()]
                path.moveTo(first.x, first.y)
                selectedNodes.drop(1).forEach { nodeIdx ->
                    val c = centers[nodeIdx]
                    path.lineTo(c.x, c.y)
                }
                activePointer?.let { ptr ->
                    path.lineTo(ptr.x, ptr.y)
                }
                drawPath(
                    path = path,
                    color = palette.secondaryAccent,
                    style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            centers.forEachIndexed { index, center ->
                val isSelected = selectedNodes.contains(index)
                drawCircle(
                    color = if (isSelected) palette.primaryAccent.copy(alpha = 0.35f) else fg.copy(alpha = 0.10f),
                    radius = 34f,
                    center = center
                )
                drawCircle(
                    color = if (isSelected) palette.secondaryAccent else fg.copy(alpha = 0.75f),
                    radius = if (isSelected) 14f else 10f,
                    center = center
                )
            }
        }
    }
}

private fun nodeCenters(width: Float, height: Float): List<Offset> {
    val stepX = width / 3f
    val stepY = height / 3f
    return (0 until 9).map { idx ->
        val row = idx / 3
        val col = idx % 3
        Offset(stepX * (col + 0.5f), stepY * (row + 0.5f))
    }
}

private fun findNearestNode(point: Offset, width: Float, height: Float): Int? {
    val centers = nodeCenters(width, height)
    val hitRadius = (width / 6f) * 0.85f
    centers.forEachIndexed { index, center ->
        if (hypot(point.x - center.x, point.y - center.y) <= hitRadius) {
            return index
        }
    }
    return null
}

@Composable
fun AccessibilityHelpDialog(
    isAlreadyEnabled: Boolean,
    onDismiss: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenOverlaySettings: (() -> Unit)? = null,
    onOpenUsageAccessSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val serviceDeclared = remember(context) {
        PermissionHelper.hasAccessibilityServiceDeclared(context)
    }
    val snapshot = remember(context, isAlreadyEnabled) {
        PermissionHelper.getSnapshot(context)
    }
    val sideloadEngineReady = snapshot.overlayGranted && snapshot.usageStatsGranted
    val serviceLabel = remember(context) {
        context.getString(R.string.accessibility_service_label)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when {
                    !serviceDeclared && sideloadEngineReady -> "Accessibility Not Required"
                    !serviceDeclared -> "Enable Overlay + Usage Access"
                    isAlreadyEnabled -> "Accessibility Service Active"
                    else -> "How to Enable Accessibility"
                }
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (!serviceDeclared) {
                    Text(
                        text = if (sideloadEngineReady) {
                            "You do NOT need to turn on anything in Android Accessibility Settings! This APK uses Usage Access + Display Over Other Apps so Google Play Protect allows direct installation."
                        } else {
                            "This APK uses Display Over Other Apps + Usage Access instead of Accessibility Service (so it won't appear in Android Accessibility Settings)."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = (if (sideloadEngineReady) EmeraldSafe else AmberWarn).copy(alpha = 0.12f)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "• Display Over Other Apps: ${if (snapshot.overlayGranted) "Granted (Ready)" else "Missing"}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (snapshot.overlayGranted) EmeraldSafe else CrimsonAlert
                            )
                            Text(
                                text = "• Usage Access: ${if (snapshot.usageStatsGranted) "Granted (Ready)" else "Missing"}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (snapshot.usageStatsGranted) EmeraldSafe else CrimsonAlert
                            )
                            Text(
                                text = "• Battery Unrestricted: ${if (snapshot.ignoringBatteryOptimizations) "Granted (Ready)" else "Recommended"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (snapshot.ignoringBatteryOptimizations) EmeraldSafe else AmberWarn
                            )
                        }
                    }

                    if (!snapshot.overlayGranted) {
                        onOpenOverlaySettings?.let { openOverlay ->
                            Button(
                                onClick = openOverlay,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("1. Allow Display Over Other Apps")
                            }
                        }
                    }
                    if (!snapshot.usageStatsGranted) {
                        onOpenUsageAccessSettings?.let { openUsage ->
                            Button(
                                onClick = openUsage,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("2. Grant Usage Access")
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Vivo / iQOO / Xiaomi / Oppo / Realme Tip",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "If a locked app (like WhatsApp) opens without the lock screen: tap \"Open App Info\" -> Permissions / Other permissions -> allow \"Display pop-up windows while running in background\" and enable Autostart / Background running.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedButton(
                                onClick = onOpenAppInfo,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open App Info (Pop-up / Autostart)")
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Follow these steps to turn ON instant app locking:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "1. Tap \"Open Accessibility Directly\" below — supported phones jump straight to $serviceLabel.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "2. If you see the main list, open \"Downloaded apps\" or \"Installed services\".",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "3. Select \"$serviceLabel\" and turn the switch ON.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AmberWarn.copy(alpha = 0.14f)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Toggle greyed out / \"Restricted setting\"?",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarn
                                )
                                Text(
                                    text = "On Android 13–16 APK installs: tap \"Open App Info\" below -> tap the ⋮ (top-right menu) -> \"Allow restricted settings\" -> confirm PIN/Fingerprint, then return here.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedButton(
                                    onClick = onOpenAppInfo,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Open App Info (Allow Restricted Settings)")
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onOpenAccessibilitySettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isAlreadyEnabled) "Open Accessibility Settings"
                            else "Open Accessibility Directly"
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

