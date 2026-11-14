package com.securelock.ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securelock.ai.ui.theme.CrispWhite
import com.securelock.ai.ui.theme.DeepNavy
import com.securelock.ai.ui.theme.ElectricBlue
import com.securelock.ai.ui.theme.SkyAccent
import com.securelock.ai.ui.theme.SlateText
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DeepNavy,
            titleContentColor = CrispWhite,
            navigationIconContentColor = CrispWhite,
            actionIconContentColor = CrispWhite
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
                    color = CrispWhite
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = SkyAccent
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
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = cardColors,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = cardColors,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(maxLength) { index ->
                val filled = index < enteredLength
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (filled) SkyAccent else CrispWhite.copy(alpha = 0.25f))
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
                    color = ElectricBlue.copy(alpha = 0.25f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = biometricIcon,
                            contentDescription = "Biometric unlock",
                            tint = SkyAccent,
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
                color = CrispWhite.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Delete digit",
                        tint = CrispWhite
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
    Surface(
        onClick = onClick,
        modifier = Modifier.size(68.dp),
        shape = CircleShape,
        color = CrispWhite.copy(alpha = 0.12f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = CrispWhite
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
                    color = SkyAccent,
                    style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            centers.forEachIndexed { index, center ->
                val isSelected = selectedNodes.contains(index)
                drawCircle(
                    color = if (isSelected) ElectricBlue.copy(alpha = 0.35f) else CrispWhite.copy(alpha = 0.10f),
                    radius = 34f,
                    center = center
                )
                drawCircle(
                    color = if (isSelected) SkyAccent else CrispWhite.copy(alpha = 0.75f),
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
