package com.securelock.ai

import com.securelock.ai.data.AppThemeId
import com.securelock.ai.data.DecoyMode
import com.securelock.ai.data.LocalSettingsState
import com.securelock.ai.data.LockMethod
import com.securelock.ai.data.SecretDateConfig
import com.securelock.ai.security.SecretDateUnlockEvaluator
import com.securelock.ai.security.SecretTapOutcome
import com.securelock.ai.security.SecretTapState
import com.securelock.ai.service.AppLockSessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityLogicUnitTest {

    @Test
    fun appLockSessionManager_grantsAndRevokesTemporaryAndMediaVaultAccess() {
        val pkg = "com.example.protectedapp"
        AppLockSessionManager.relockAllApps()
        assertFalse(AppLockSessionManager.isAppTemporarilyUnlocked(pkg))
        assertFalse(AppLockSessionManager.isMediaVaultUnlocked)
        assertFalse(AppLockSessionManager.vaultUnlockedFlow.value)

        AppLockSessionManager.unlockVault()
        assertTrue(AppLockSessionManager.vaultUnlockedFlow.value)

        AppLockSessionManager.markAppUnlocked(pkg, timeoutSeconds = 30)
        AppLockSessionManager.unlockMediaVault()
        assertTrue(AppLockSessionManager.isAppTemporarilyUnlocked(pkg))
        assertTrue(AppLockSessionManager.isMediaVaultUnlocked)

        AppLockSessionManager.relockAllApps()
        assertFalse(AppLockSessionManager.isAppTemporarilyUnlocked(pkg))
        assertFalse(AppLockSessionManager.isMediaVaultUnlocked)
        assertFalse(AppLockSessionManager.vaultUnlockedFlow.value)
    }

    @Test
    fun defaultSettings_enforceOfflineSafeAndThemeDefaults() {
        val defaults = LocalSettingsState()
        assertTrue(defaults.biometricEnabled)
        assertTrue(defaults.lockOnScreenOff)
        assertTrue(defaults.safeBrowsingWarningsEnabled)
        assertTrue(defaults.foregroundIntruderPromptEnabled)
        assertEquals(3, defaults.intruderAttemptThreshold)
        assertFalse(defaults.protectionScreenConfig.enabled)
        assertTrue(defaults.secretDateConfig.enabled)
        assertEquals(LockMethod.PIN_4, defaults.preferredLockMethod)
        assertEquals(AppThemeId.MIDNIGHT_BLUE, defaults.themeId)
        assertEquals(DecoyMode.APPLICATION_UNAVAILABLE, defaults.protectionScreenConfig.decoyMode)
    }

    @Test
    fun secretDate_unconfiguredFallbackTodayWorksUntilCustomPinIsSet() {
        val config = SecretDateConfig(enabled = true, day = 15, month = 6, year = null)
        val today = Triple(14, 11, 2026)
        var state = SecretTapState()
        var now = 5_000L

        repeat(4) {
            state = (SecretDateUnlockEvaluator.registerDateTap(
                state, config, hasSecretPin = false, day = 14, month = 11, year = 2026, nowMillis = now, fallbackToday = today
            ) as SecretTapOutcome.Counting).nextState
            now += 200L
        }
        val fifth = SecretDateUnlockEvaluator.registerDateTap(
            state, config, hasSecretPin = false, day = 14, month = 11, year = 2026, nowMillis = now, fallbackToday = today
        )
        assertTrue(fifth is SecretTapOutcome.TriggerPinPrompt)

        // Once custom Secret PIN is configured, today fallback no longer triggers unless it matches config
        state = SecretTapState()
        repeat(5) {
            val outcome = SecretDateUnlockEvaluator.registerDateTap(
                state, config, hasSecretPin = true, day = 14, month = 11, year = 2026, nowMillis = now, fallbackToday = today
            )
            assertTrue(outcome is SecretTapOutcome.Counting)
            state = (outcome as SecretTapOutcome.Counting).nextState
            now += 200L
        }
    }

    @Test
    fun secretDate_exactFiveTapsOnConfiguredDateTriggersPinPrompt() {
        val config = SecretDateConfig(enabled = true, day = 14, month = 11, year = 2026)
        var state = SecretTapState()
        var now = 10_000L

        // First 4 taps must not trigger PIN prompt
        repeat(4) { idx ->
            val outcome = SecretDateUnlockEvaluator.registerDateTap(
                currentState = state,
                config = config,
                hasSecretPin = true,
                day = 14,
                month = 11,
                year = 2026,
                nowMillis = now
            )
            assertTrue(outcome is SecretTapOutcome.Counting)
            state = (outcome as SecretTapOutcome.Counting).nextState
            assertEquals(idx + 1, state.tapCount)
            now += 300L
        }

        // 5th tap on exact secret date must trigger PIN prompt and reset tap state
        val fifthOutcome = SecretDateUnlockEvaluator.registerDateTap(
            currentState = state,
            config = config,
            hasSecretPin = true,
            day = 14,
            month = 11,
            year = 2026,
            nowMillis = now
        )
        assertTrue(fifthOutcome is SecretTapOutcome.TriggerPinPrompt)
        assertEquals(0, (fifthOutcome as SecretTapOutcome.TriggerPinPrompt).resetState.tapCount)
    }

    @Test
    fun secretDate_wrongDateOrWrongMonthNeverTriggersOnFiveTaps() {
        val config = SecretDateConfig(enabled = true, day = 14, month = 11, year = 2026)
        var state = SecretTapState()
        var now = 20_000L

        // 5 taps on wrong day
        repeat(5) {
            val outcome = SecretDateUnlockEvaluator.registerDateTap(
                currentState = state,
                config = config,
                hasSecretPin = true,
                day = 15,
                month = 11,
                year = 2026,
                nowMillis = now
            )
            assertTrue(outcome is SecretTapOutcome.Counting)
            state = (outcome as SecretTapOutcome.Counting).nextState
            now += 250L
        }

        // 5 taps on wrong year when year is configured
        state = SecretDateUnlockEvaluator.reset()
        repeat(5) {
            val outcome = SecretDateUnlockEvaluator.registerDateTap(
                currentState = state,
                config = config,
                hasSecretPin = true,
                day = 14,
                month = 11,
                year = 2025,
                nowMillis = now
            )
            assertTrue(outcome is SecretTapOutcome.Counting)
            state = (outcome as SecretTapOutcome.Counting).nextState
            now += 250L
        }
    }

    @Test
    fun secretDate_timeoutAndResetAndOptionalYearWorkCorrectly() {
        val config = SecretDateConfig(enabled = true, day = 29, month = 2, year = null)
        var state = SecretTapState()
        var now = 30_000L

        // Tap 3 times, then wait past timeout -> counter must reset to 1
        repeat(3) {
            state = (SecretDateUnlockEvaluator.registerDateTap(
                state, config, true, day = 29, month = 2, year = 2024, nowMillis = now
            ) as SecretTapOutcome.Counting).nextState
            now += 200L
        }
        assertEquals(3, state.tapCount)

        now += SecretDateUnlockEvaluator.TAP_TIMEOUT_MILLIS + 100L
        state = (SecretDateUnlockEvaluator.registerDateTap(
            state, config, true, day = 29, month = 2, year = 2024, nowMillis = now
        ) as SecretTapOutcome.Counting).nextState
        assertEquals(1, state.tapCount)

        // Continue 4 more taps on any year when year == null -> triggers on 5th
        repeat(3) {
            now += 200L
            state = (SecretDateUnlockEvaluator.registerDateTap(
                state, config, true, day = 29, month = 2, year = 2024, nowMillis = now
            ) as SecretTapOutcome.Counting).nextState
        }
        now += 200L
        val fifth = SecretDateUnlockEvaluator.registerDateTap(
            state, config, true, day = 29, month = 2, year = 2024, nowMillis = now
        )
        assertTrue(fifth is SecretTapOutcome.TriggerPinPrompt)

        // Calendar date validation & rate-limit cooldowns
        assertTrue(SecretDateUnlockEvaluator.isValidCalendarDate(29, 2, 2024))
        assertFalse(SecretDateUnlockEvaluator.isValidCalendarDate(29, 2, 2025))
        assertFalse(SecretDateUnlockEvaluator.isValidCalendarDate(31, 4, null))
        assertEquals(0L, SecretDateUnlockEvaluator.cooldownSecondsForFailures(2))
        assertEquals(30L, SecretDateUnlockEvaluator.cooldownSecondsForFailures(3))
        assertEquals(60L, SecretDateUnlockEvaluator.cooldownSecondsForFailures(5))
        assertEquals(300L, SecretDateUnlockEvaluator.cooldownSecondsForFailures(7))
    }
}
