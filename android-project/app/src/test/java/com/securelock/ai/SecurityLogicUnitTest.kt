package com.securelock.ai

import com.securelock.ai.data.LocalSettingsState
import com.securelock.ai.data.LockMethod
import com.securelock.ai.service.AppLockSessionManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityLogicUnitTest {

    @Test
    fun appLockSessionManager_grantsAndRevokesTemporaryAccess() {
        val pkg = "com.example.protectedapp"
        AppLockSessionManager.relockAllApps()
        assertFalse(AppLockSessionManager.isAppTemporarilyUnlocked(pkg))

        AppLockSessionManager.markAppUnlocked(pkg, timeoutSeconds = 30)
        assertTrue(AppLockSessionManager.isAppTemporarilyUnlocked(pkg))

        AppLockSessionManager.relockAllApps()
        assertFalse(AppLockSessionManager.isAppTemporarilyUnlocked(pkg))
    }

    @Test
    fun defaultSettings_enforceOfflineSafeDefaults() {
        val defaults = LocalSettingsState()
        assertTrue(defaults.biometricEnabled)
        assertTrue(defaults.safeBrowsingWarningsEnabled)
        assertTrue(defaults.foregroundIntruderPromptEnabled)
        assertFalse(defaults.protectionScreenConfig.enabled)
        assertTrue(defaults.preferredLockMethod == LockMethod.PIN)
    }
}
