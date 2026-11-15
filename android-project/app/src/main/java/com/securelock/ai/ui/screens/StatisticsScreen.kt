package com.securelock.ai.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.securelock.ai.data.DailyActivityStat
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.ui.components.SecureTopBar
import com.securelock.ai.ui.components.WhiteSecurityCard
import com.securelock.ai.ui.theme.CrimsonAlert
import com.securelock.ai.ui.theme.ElectricBlue
import com.securelock.ai.ui.theme.EmeraldSafe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    repository: SecureStorageRepository,
    onBack: () -> Unit
) {
    val settings by repository.settings.collectAsState()
    val stats by repository.statistics.collectAsState()

    val lastActivityText = remember(stats.lastActivityMillis) {
        if (stats.lastActivityMillis == 0L) "No activity recorded yet"
        else SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
            .format(Date(stats.lastActivityMillis))
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            SecureTopBar(
                title = "Security Statistics",
                subtitle = "Local-only analytics",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WhiteSecurityCard(modifier = Modifier.weight(1f)) {
                        Column {
                            Text("Protected Apps", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = settings.protectedPackages.size.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    WhiteSecurityCard(modifier = Modifier.weight(1f)) {
                        Column {
                            Text("Total Attempts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = stats.totalAttempts.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WhiteSecurityCard(modifier = Modifier.weight(1f)) {
                        Column {
                            Text("Successful Unlocks", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = stats.successfulUnlocks.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = EmeraldSafe
                            )
                        }
                    }
                    WhiteSecurityCard(modifier = Modifier.weight(1f)) {
                        Column {
                            Text("Failed Attempts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = stats.failedAttempts.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = CrimsonAlert
                            )
                        }
                    }
                }
            }

            item {
                WhiteSecurityCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Weekly Activity (Success vs Failed)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Last activity: $lastActivityText",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        WeeklyActivityBarChart(history = stats.dailyHistory)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            stats.dailyHistory.forEach { day ->
                                Text(day.dayLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Security Events",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (stats.recentEvents.isNotEmpty()) {
                        OutlinedButton(onClick = { repository.resetStatistics() }) {
                            Text("Reset Stats")
                        }
                    }
                }
            }

            if (stats.recentEvents.isEmpty()) {
                item {
                    WhiteSecurityCard {
                        Text(
                            text = "No security events logged yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(stats.recentEvents, key = { it.id }) { event ->
                    val timeStr = remember(event.timestampMillis) {
                        SimpleDateFormat("MMM dd · HH:mm:ss", Locale.getDefault())
                            .format(Date(event.timestampMillis))
                    }
                    WhiteSecurityCard {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(event.title, style = MaterialTheme.typography.titleMedium)
                                Text(timeStr, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(event.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyActivityBarChart(history: List<DailyActivityStat>) {
    val maxVal = (history.maxOfOrNull { it.successCount + it.failedCount } ?: 1).coerceAtLeast(1)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val count = history.size.coerceAtLeast(1)
        val slotWidth = size.width / count
        val barWidth = (slotWidth * 0.45f).coerceAtMost(36f)

        history.forEachIndexed { index, day ->
            val centerX = slotWidth * index + slotWidth / 2f
            val totalHeight = ((day.successCount + day.failedCount).toFloat() / maxVal) * (size.height - 16f)
            val failedHeight = (day.failedCount.toFloat() / maxVal) * (size.height - 16f)
            val successHeight = (totalHeight - failedHeight).coerceAtLeast(0f)

            val topSuccess = size.height - totalHeight
            if (successHeight > 0f) {
                drawRoundRect(
                    color = ElectricBlue,
                    topLeft = Offset(centerX - barWidth / 2f, topSuccess),
                    size = Size(barWidth, successHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
            if (failedHeight > 0f) {
                drawRoundRect(
                    color = CrimsonAlert,
                    topLeft = Offset(centerX - barWidth / 2f, size.height - failedHeight),
                    size = Size(barWidth, failedHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }
    }
}
