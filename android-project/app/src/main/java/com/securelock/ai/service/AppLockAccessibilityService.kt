package com.securelock.ai.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.securelock.ai.LockOverlayActivity
import com.securelock.ai.SecureLockApplication

/**
 * Optional, user-enabled AccessibilityService that observes TYPE_WINDOW_STATE_CHANGED
 * events only to detect when a protected package enters the foreground.
 * canRetrieveWindowContent is explicitly false in accessibility_service_config.xml.
 */
class AppLockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        if (AppLockSessionManager.isSystemTransientPackage(packageName, this.packageName)) return

        val repo = (application as SecureLockApplication).storageRepository
        val settings = repo.settings.value

        AppLockSessionManager.onForegroundPackageObserved(
            observedPackage = packageName,
            ownPackage = this.packageName,
            timeoutSeconds = settings.relockTimeoutSeconds
        )

        val isSettingsProtected = settings.protectSettingsScreen &&
            packageName == "com.android.settings"
        val isTargetProtected = settings.protectedPackages.contains(packageName) || isSettingsProtected

        if (!isTargetProtected) return
        if (AppLockSessionManager.isAppTemporarilyUnlocked(
                packageName = packageName,
                timeoutSeconds = settings.relockTimeoutSeconds,
                currentForegroundPackage = packageName
            )
        ) return
        if (!AppLockSessionManager.tryAcquireOverlayLaunch(packageName)) return

        val intent = Intent(this, LockOverlayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra(LockOverlayActivity.EXTRA_TARGET_PACKAGE, packageName)
        }
        runCatching { startActivity(intent) }
    }

    override fun onInterrupt() {
        // No continuous resources held.
    }
}
