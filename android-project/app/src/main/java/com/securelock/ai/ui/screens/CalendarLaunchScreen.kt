package com.securelock.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.securelock.ai.security.SecretDateUnlockEvaluator
import com.securelock.ai.security.SecretPinVerifyResult
import com.securelock.ai.security.SecretTapOutcome
import com.securelock.ai.security.SecretTapState
import com.securelock.ai.security.SecureStorageRepository
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val CalendarLightColors = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD2E3FC),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF5F6368),
    background = Color(0xFFF8FAFD),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFEFF3F8),
    onSurfaceVariant = Color(0xFF5F6368),
    outline = Color(0xFFDADCE0)
)

private val CalendarDarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD2E3FC),
    secondary = Color(0xFF9AA0A6),
    background = Color(0xFF121316),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF1E1F23),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF2B2D33),
    onSurfaceVariant = Color(0xFF9AA0A6),
    outline = Color(0xFF3C4043)
)

private data class CalendarCell(
    val day: Int,
    val isCurrentMonth: Boolean
)

/**
 * Genuine standalone Material 3 monthly Calendar launch screen.
 * Contains zero AppLock branding or unlock hints.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarLaunchScreen(
    repository: SecureStorageRepository,
    onSecretUnlocked: () -> Unit
) {
    val settings by repository.settings.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = isSystemInDarkTheme()

    val todayCal = remember { Calendar.getInstance() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH) + 1
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    var displayedYear by remember { mutableIntStateOf(todayYear) }
    var displayedMonth by remember { mutableIntStateOf(todayMonth) } // 1..12
    var selectedDay by remember { mutableIntStateOf(todayDay) }

    var tapState by remember { mutableStateOf(SecretTapState()) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    // Reset tap counter after inactivity timeout
    LaunchedEffect(tapState.lastTapAtMillis, tapState.tapCount) {
        if (tapState.tapCount > 0) {
            delay(SecretDateUnlockEvaluator.TAP_TIMEOUT_MILLIS + 50L)
            tapState = SecretDateUnlockEvaluator.reset()
        }
    }

    // Reset tap counter & clear PIN dialog whenever app goes to background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                tapState = SecretDateUnlockEvaluator.reset()
                pinInput = ""
                showPinDialog = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun changeMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        displayedYear = cal.get(Calendar.YEAR)
        displayedMonth = cal.get(Calendar.MONTH) + 1
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        selectedDay = selectedDay.coerceIn(1, maxDay)
        tapState = SecretDateUnlockEvaluator.reset()
    }

    fun jumpToToday() {
        val now = Calendar.getInstance()
        displayedYear = now.get(Calendar.YEAR)
        displayedMonth = now.get(Calendar.MONTH) + 1
        selectedDay = now.get(Calendar.DAY_OF_MONTH)
        tapState = SecretDateUnlockEvaluator.reset()
    }

    fun submitSecretPin() {
        val candidate = pinInput
        pinInput = ""
        showPinDialog = false
        tapState = SecretDateUnlockEvaluator.reset()
        if (candidate.length !in 4..8) return

        when (repository.verifySecretDatePin(candidate)) {
            SecretPinVerifyResult.Success -> onSecretUnlocked()
            is SecretPinVerifyResult.Incorrect,
            is SecretPinVerifyResult.RateLimited -> {
                // Dismissed cleanly; remain on Calendar without revealing secret state
            }
        }
    }

    val monthCells = remember(displayedYear, displayedMonth) {
        buildMonthGrid(displayedYear, displayedMonth)
    }

    val monthTitle = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    val selectedSummary = remember(displayedYear, displayedMonth, selectedDay) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth - 1)
            set(Calendar.DAY_OF_MONTH, selectedDay.coerceIn(1, 28))
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, selectedDay.coerceIn(1, maxDay))
        val fullDate = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(cal.time)
        val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val daysInYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
        Triple(fullDate, "Week $weekOfYear", "Day $dayOfYear of $daysInYear")
    }

    MaterialTheme(colorScheme = if (isDark) CalendarDarkColors else CalendarLightColors) {
        Scaffold(
            modifier = Modifier.safeDrawingPadding(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Calendar",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { jumpToToday() }) {
                            Text("Today", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Month Navigation Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { changeMonth(-1) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous month"
                                    )
                                }
                                IconButton(onClick = { changeMonth(1) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Next month"
                                    )
                                }
                            }
                        }

                        // Weekday Header
                        val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        Row(modifier = Modifier.fillMaxWidth()) {
                            weekdays.forEachIndexed { index, label ->
                                Text(
                                    text = label,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (index == 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        // Monthly Date Grid (6 weeks x 7 days)
                        val rows = monthCells.chunked(7)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            rows.forEach { week ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    week.forEach { cell ->
                                        val isToday = cell.isCurrentMonth &&
                                            cell.day == todayDay &&
                                            displayedMonth == todayMonth &&
                                            displayedYear == todayYear
                                        val isSelected = cell.isCurrentMonth && cell.day == selectedDay

                                        val bgColor = when {
                                            isToday -> MaterialTheme.colorScheme.primary
                                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        }
                                        val textColor = when {
                                            !cell.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                            isToday -> MaterialTheme.colorScheme.onPrimary
                                            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(bgColor)
                                                .clickable(
                                                    enabled = cell.isCurrentMonth,
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    selectedDay = cell.day
                                                    val outcome = SecretDateUnlockEvaluator.registerDateTap(
                                                        currentState = tapState,
                                                        config = settings.secretDateConfig,
                                                        hasSecretPin = repository.hasSecretDatePinConfigured(),
                                                        day = cell.day,
                                                        month = displayedMonth,
                                                        year = displayedYear,
                                                        nowMillis = System.currentTimeMillis()
                                                    )
                                                    when (outcome) {
                                                        is SecretTapOutcome.Counting -> {
                                                            tapState = outcome.nextState
                                                        }
                                                        is SecretTapOutcome.TriggerPinPrompt -> {
                                                            tapState = outcome.resetState
                                                            pinInput = ""
                                                            showPinDialog = true
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cell.day.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Standalone Day Summary / Agenda Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = selectedSummary.first,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = selectedSummary.second,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = selectedSummary.third,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EventNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "No events scheduled for this day",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = {
                    pinInput = ""
                    showPinDialog = false
                    tapState = SecretDateUnlockEvaluator.reset()
                },
                title = {
                    Text(
                        text = "Enter PIN",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { updated ->
                            if (updated.length <= 8 && updated.all(Char::isDigit)) {
                                pinInput = updated
                            }
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitSecretPin() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { submitSecretPin() },
                        enabled = pinInput.length in 4..8
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pinInput = ""
                            showPinDialog = false
                            tapState = SecretDateUnlockEvaluator.reset()
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun buildMonthGrid(year: Int, month: Int): List<CalendarCell> {
    val firstOfMonth = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val startOffset = firstOfMonth.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY // 0..6
    val daysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

    val prevMonth = (firstOfMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
    val prevMonthMaxDay = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cells = ArrayList<CalendarCell>(42)
    for (i in (startOffset - 1) downTo 0) {
        cells += CalendarCell(day = prevMonthMaxDay - i, isCurrentMonth = false)
    }
    for (d in 1..daysInMonth) {
        cells += CalendarCell(day = d, isCurrentMonth = true)
    }
    var nextDay = 1
    while (cells.size < 42) {
        cells += CalendarCell(day = nextDay++, isCurrentMonth = false)
    }
    return cells
}
