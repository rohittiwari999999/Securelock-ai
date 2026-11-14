package com.securelock.ai

import com.securelock.ai.data.AppThemeId
import com.securelock.ai.data.DecoyMode
import com.securelock.ai.data.LocalSettingsState
import com.securelock.ai.data.LockMethod
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

        AppLockSessionManager.markAppUnlocked(pkg, timeoutSeconds = 30)
        AppLockSessionManager.unlockMediaVault()
        assertTrue(AppLockSessionManager.isAppTemporarilyUnlocked(pkg))
        assertTrue(AppLockSessionManager.isMediaVaultUnlocked)

        AppLockSessionManager.relockAllApps()
        assertFalse(AppLockSessionManager.isAppTemporarilyUnlocked(pkg))
        assertFalse(AppLockSessionManager.isMediaVaultUnlocked)
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
        assertEquals(LockMethod.PIN_4, defaults.preferredLockMethod)
        assertEquals(AppThemeId.MIDNIGHT_BLUE, defaults.themeId)
        assertEquals(DecoyMode.APPLICATION_UNAVAILABLE, defaults.protectionScreenConfig.decoyMode)
    }
}
