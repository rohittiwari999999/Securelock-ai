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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.securelock.ai.data.LockMethod
import com.securelock.ai.security.SecretDateUnlockEvaluator
import com.securelock.ai.security.SecretTapOutcome
import com.securelock.ai.security.SecretTapState
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.ui.components.NeuglassBackdrop
import com.securelock.ai.ui.components.PatternLockPad
import com.securelock.ai.ui.components.neuglassInset
import com.securelock.ai.ui.components.neuglassSurface
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
 * Shows visual 5-tap onboarding ONLY during initial first-time setup before Secret Date & AppLock are configured.
 * Once configured, contains zero AppLock branding or unlock hints.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarLaunchScreen(
    @Suppress("UNUSED_PARAMETER") activity: FragmentActivity? = null,
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

    // First-time onboarding state (disappears forever once AppLock & Secret Date are configured)
    var appLockConfigured by remember { mutableStateOf(repository.hasAppLockCredentialConfigured()) }
    var pendingSecretDate by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }

    // Reset tap counter after inactivity timeout
    LaunchedEffect(tapState.lastTapAtMillis, tapState.tapCount) {
        if (tapState.tapCount > 0) {
            delay(SecretDateUnlockEvaluator.TAP_TIMEOUT_MILLIS + 50L)
            tapState = SecretDateUnlockEvaluator.reset()
        }
    }

    // Reset tap counter whenever app goes to background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                tapState = SecretDateUnlockEvaluator.reset()
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
        NeuglassBackdrop(
            baseBackground = MaterialTheme.colorScheme.background,
            primaryGlow = MaterialTheme.colorScheme.primary,
            secondaryGlow = Color(0xFF38BDF8)
        ) {
            Scaffold(
                modifier = Modifier.safeDrawingPadding(),
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .neuglassSurface(
                                            cornerRadius = 12.dp,
                                            elevationScale = 0.75f
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Calendar",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        actions = {
                            Surface(
                                onClick = { jumpToToday() },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .neuglassSurface(cornerRadius = 14.dp, elevationScale = 0.7f)
                            ) {
                                Text(
                                    text = "Today",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
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
                // Visual First-Time Onboarding Card (shown ONLY before initial AppLock setup is completed)
                if (!appLockConfigured) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier.neuglassSurface(
                            cornerRadius = 22.dp,
                            surfaceColor = MaterialTheme.colorScheme.primaryContainer,
                            elevationScale = 0.9f
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Step 1: Select Secret Date",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Choose any date on the calendar below and tap it 5 times to continue to AppLock setup.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    repeat(SecretDateUnlockEvaluator.REQUIRED_TAPS) { idx ->
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (idx < tapState.tapCount) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)
                                                    }
                                                )
                                        )
                                    }
                                }
                                Text(
                                    text = "${tapState.tapCount} / 5 taps",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.neuglassSurface(cornerRadius = 24.dp, elevationScale = 1.0f)
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .neuglassSurface(cornerRadius = 19.dp, isCircular = true, elevationScale = 0.65f)
                                ) {
                                    IconButton(onClick = { changeMonth(-1) }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Previous month"
                                        )
                                    }
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .neuglassSurface(cornerRadius = 19.dp, isCircular = true, elevationScale = 0.65f)
                                ) {
                                    IconButton(onClick = { changeMonth(1) }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next month"
                                        )
                                    }
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
                                                    val nowMs = System.currentTimeMillis()
                                                    if (!appLockConfigured) {
                                                        val isSameDate = tapState.lastTappedDay == cell.day &&
                                                            tapState.lastTappedMonth == displayedMonth &&
                                                            tapState.lastTappedYear == displayedYear
                                                        val withinTimeout = tapState.lastTapAtMillis > 0L &&
                                                            (nowMs - tapState.lastTapAtMillis) <= SecretDateUnlockEvaluator.TAP_TIMEOUT_MILLIS
                                                        val nextCount = if (isSameDate && withinTimeout) {
                                                            tapState.tapCount + 1
                                                        } else {
                                                            1
                                                        }
                                                        if (nextCount >= SecretDateUnlockEvaluator.REQUIRED_TAPS) {
                                                            tapState = SecretDateUnlockEvaluator.reset()
                                                            pendingSecretDate = Triple(cell.day, displayedMonth, displayedYear)
                                                        } else {
                                                            tapState = SecretTapState(
                                                                lastTappedDay = cell.day,
                                                                lastTappedMonth = displayedMonth,
                                                                lastTappedYear = displayedYear,
                                                                tapCount = nextCount,
                                                                lastTapAtMillis = nowMs
                                                            )
                                                        }
                                                    } else {
                                                        val outcome = SecretDateUnlockEvaluator.registerDateTap(
                                                            currentState = tapState,
                                                            config = settings.secretDateConfig,
                                                            hasSecretPin = repository.hasSecretDatePinConfigured() ||
                                                                repository.hasAppLockCredentialConfigured(),
                                                            day = cell.day,
                                                            month = displayedMonth,
                                                            year = displayedYear,
                                                            nowMillis = nowMs
                                                        )
                                                        when (outcome) {
                                                            is SecretTapOutcome.Counting -> {
                                                                tapState = outcome.nextState
                                                            }
                                                            is SecretTapOutcome.TriggerPinPrompt -> {
                                                                tapState = outcome.resetState
                                                                onSecretUnlocked()
                                                            }
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
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.neuglassSurface(cornerRadius = 22.dp, elevationScale = 0.95f)
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
                            Box(modifier = Modifier.neuglassInset(cornerRadius = 10.dp)) {
                                Text(
                                    text = selectedSummary.second,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Box(modifier = Modifier.neuglassInset(cornerRadius = 10.dp)) {
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
        }

        // Step 2: Direct App Lock Setup Window (Pattern, PIN & Fingerprint)
        pendingSecretDate?.let { (chosenDay, chosenMonth, _) ->
            var selectedMethod by remember { mutableStateOf(LockMethod.PATTERN) }
            var pinInput by remember { mutableStateOf("") }
            var pinConfirm by remember { mutableStateOf("") }
            var firstPattern by remember { mutableStateOf<List<Int>?>(null) }
            var confirmedPattern by remember { mutableStateOf<List<Int>?>(null) }
            var patternHint by remember { mutableStateOf("Draw your Lock Pattern (4+ dots)") }
            var enableFingerprint by remember { mutableStateOf(true) }
            var appLockErr by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { pendingSecretDate = null },
                title = { Text("Step 2: Set App Lock") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Choose Pattern or PIN and enable Fingerprint unlock for your locked apps:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = selectedMethod == LockMethod.PATTERN,
                                onClick = {
                                    selectedMethod = LockMethod.PATTERN
                                    appLockErr = null
                                },
                                label = { Text("Pattern") }
                            )
                            FilterChip(
                                selected = selectedMethod == LockMethod.PIN_4,
                                onClick = {
                                    selectedMethod = LockMethod.PIN_4
                                    appLockErr = null
                                },
                                label = { Text("PIN") }
                            )
                        }

                        if (selectedMethod == LockMethod.PATTERN) {
                            Text(
                                text = patternHint,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (confirmedPattern != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                PatternLockPad(
                                    onPatternComplete = { nodes ->
                                        appLockErr = null
                                        if (nodes.size < 4) {
                                            patternHint = "Connect at least 4 dots"
                                        } else if (firstPattern == null) {
                                            firstPattern = nodes
                                            confirmedPattern = null
                                            patternHint = "Draw the same pattern again to confirm"
                                        } else if (firstPattern == nodes) {
                                            confirmedPattern = nodes
                                            patternHint = "Lock Pattern confirmed!"
                                        } else {
                                            firstPattern = null
                                            confirmedPattern = null
                                            patternHint = "Patterns did not match. Draw again."
                                        }
                                    }
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { if (it.length <= 12 && it.all(Char::isDigit)) pinInput = it },
                                label = { Text("App Lock PIN (4–12 digits)") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = pinConfirm,
                                onValueChange = { if (it.length <= 12 && it.all(Char::isDigit)) pinConfirm = it },
                                label = { Text("Confirm PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Also unlock with Fingerprint",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Switch(
                                checked = enableFingerprint,
                                onCheckedChange = { enableFingerprint = it }
                            )
                        }

                        appLockErr?.let { err ->
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (selectedMethod == LockMethod.PATTERN) {
                            val pattern = confirmedPattern
                            if (pattern == null || pattern.size < 4) {
                                appLockErr = "Draw and confirm your pattern twice"
                                return@Button
                            }
                            repository.setPattern(pattern)
                            repository.updateSettings {
                                it.copy(
                                    preferredLockMethod = LockMethod.PATTERN,
                                    biometricEnabled = enableFingerprint,
                                    protectionScreenConfig = it.protectionScreenConfig.copy(enabled = false)
                                )
                            }
                        } else {
                            if (pinInput.length < 4) {
                                appLockErr = "Enter a 4–12 digit PIN"
                                return@Button
                            }
                            if (pinInput != pinConfirm) {
                                appLockErr = "PINs do not match"
                                return@Button
                            }
                            repository.setPin(pinInput)
                            repository.updateSettings {
                                it.copy(
                                    biometricEnabled = enableFingerprint,
                                    protectionScreenConfig = it.protectionScreenConfig.copy(enabled = false)
                                )
                            }
                        }

                        if (!repository.hasRecoveryCodeConfigured()) {
                            repository.generateAndStoreRecoveryCode()
                        }
                        repository.saveSecretDateConfiguration(
                            day = chosenDay,
                            month = chosenMonth,
                            year = null,
                            enabled = true,
                            newSecretPin = null
                        )
                        appLockConfigured = true
                        pendingSecretDate = null
                    }) {
                        Text("Save & Finish")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingSecretDate = null }) {
                        Text("Change Date")
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
