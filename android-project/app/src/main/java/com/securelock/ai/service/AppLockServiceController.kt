package com.securelock.ai.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.securelock.ai.data.LocalSettingsState
import com.securelock.ai.security.PermissionHelper

/**
 * Coordinates the single active Android app-lock detection engine.
 * When AccessibilityService is enabled, it handles locking directly and stops the polling
 * foreground service so two engines never race or conflict.
 */
object AppLockServiceController {

    fun needsAppProtection(settings: LocalSettingsState): Boolean {
        return settings.protectedPackages.isNotEmpty() || settings.protectSettingsScreen
    }

    fun hasRequiredOverlayOrAccessibility(context: Context): Boolean {
        val accessibilityOn = PermissionHelper.isAccessibilityServiceEnabled(context)
        val usageAndOverlayOn = PermissionHelper.hasUsageStatsPermission(context) &&
            Settings.canDrawOverlays(context)
        return accessibilityOn || usageAndOverlayOn
    }

    fun isProtectionEngineActive(context: Context, settings: LocalSettingsState): Boolean {
        if (!needsAppProtection(settings)) return false
        if (PermissionHelper.isAccessibilityServiceEnabled(context)) return true
        return PermissionHelper.hasUsageStatsPermission(context) &&
            Settings.canDrawOverlays(context) &&
            AppLockMonitorService.isRunning
    }

    fun syncMonitorService(context: Context, settings: LocalSettingsState) {
        val appContext = context.applicationContext
        val shouldProtect = needsAppProtection(settings)
        val accessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(appContext)
        val usageGranted = PermissionHelper.hasUsageStatsPermission(appContext)
        val overlayGranted = Settings.canDrawOverlays(appContext)

        if (shouldProtect && !accessibilityEnabled && usageGranted && overlayGranted) {
            runCatching {
                val intent = Intent(appContext, AppLockMonitorService::class.java)
                ContextCompat.startForegroundService(appContext, intent)
            }
        } else if (AppLockMonitorService.isRunning) {
            runCatching {
                val stopIntent = Intent(appContext, AppLockMonitorService::class.java).apply {
                    action = AppLockMonitorService.ACTION_STOP
                }
                appContext.startService(stopIntent)
            }
        }
    }

    fun resolveAppLabel(context: Context, packageName: String): String {
        if (packageName == "com.android.settings") return "Android Settings"
        val pm = context.packageManager
        return runCatching {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(info).toString()
        }.getOrDefault(packageName)
    }
}
