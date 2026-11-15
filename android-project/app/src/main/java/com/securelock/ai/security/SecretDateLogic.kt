package com.securelock.ai.security

import com.securelock.ai.data.SecretDateConfig

/**
 * Pure, deterministic state machine for the Secret Date 5-tap unlock sequence.
 * Enforces exact 5-tap threshold, tap timeout, month-change reset, and background reset.
 */
data class SecretTapState(
    val lastTappedDay: Int? = null,
    val lastTappedMonth: Int? = null,
    val lastTappedYear: Int? = null,
    val tapCount: Int = 0,
    val lastTapAtMillis: Long = 0L
)

sealed interface SecretTapOutcome {
    data class Counting(val nextState: SecretTapState) : SecretTapOutcome
    data class TriggerPinPrompt(val resetState: SecretTapState) : SecretTapOutcome
}

sealed interface SecretPinVerifyResult {
    data object Success : SecretPinVerifyResult
    data class Incorrect(val failedAttempts: Int) : SecretPinVerifyResult
    data class RateLimited(val remainingSeconds: Long) : SecretPinVerifyResult
}

object SecretDateUnlockEvaluator {

    const val REQUIRED_TAPS = 5
    const val TAP_TIMEOUT_MILLIS = 4_000L

    fun isValidCalendarDate(day: Int, month: Int, year: Int?): Boolean {
        if (month !in 1..12 || day !in 1..31) return false
        val maxDays = when (month) {
            2 -> if (year == null || isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return day <= maxDays
    }

    fun matchesSecretDate(
        config: SecretDateConfig,
        hasSecretPin: Boolean,
        day: Int,
        month: Int,
        year: Int
    ): Boolean {
        if (!config.enabled || !hasSecretPin) return false
        if (day != config.day || month != config.month) return false
        return config.year == null || config.year == year
    }

    fun registerDateTap(
        currentState: SecretTapState,
        config: SecretDateConfig,
        hasSecretPin: Boolean,
        day: Int,
        month: Int,
        year: Int,
        nowMillis: Long
    ): SecretTapOutcome {
        val isSameDate = currentState.lastTappedDay == day &&
            currentState.lastTappedMonth == month &&
            currentState.lastTappedYear == year
        val withinTimeout = currentState.lastTapAtMillis > 0L &&
            (nowMillis - currentState.lastTapAtMillis) <= TAP_TIMEOUT_MILLIS

        val nextCount = if (isSameDate && withinTimeout) {
            currentState.tapCount + 1
        } else {
            1
        }

        if (nextCount >= REQUIRED_TAPS &&
            matchesSecretDate(config, hasSecretPin, day, month, year)
        ) {
            return SecretTapOutcome.TriggerPinPrompt(SecretTapState())
        }

        return SecretTapOutcome.Counting(
            SecretTapState(
                lastTappedDay = day,
                lastTappedMonth = month,
                lastTappedYear = year,
                tapCount = nextCount,
                lastTapAtMillis = nowMillis
            )
        )
    }

    fun reset(): SecretTapState = SecretTapState()

    fun cooldownSecondsForFailures(consecutiveFailures: Int): Long = when {
        consecutiveFailures < 3 -> 0L
        consecutiveFailures < 5 -> 30L
        consecutiveFailures < 7 -> 60L
        else -> 300L
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}
