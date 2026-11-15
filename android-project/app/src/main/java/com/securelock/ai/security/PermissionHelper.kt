package com.securelock.ai.security

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.securelock.ai.service.AppLockAccessibilityService
import com.securelock.ai.service.LockNotificationListenerService

data class PermissionStatusSnapshot(
    val cameraGranted: Boolean,
    val postNotificationsGranted: Boolean,
    val usageStatsGranted: Boolean,
    val overlayGranted: Boolean,
    val accessibilityEnabled: Boolean,
    val notificationListenerEnabled: Boolean,
    val ignoringBatteryOptimizations: Boolean
)

/**
 * Inspects standard runtime permissions and user-granted Android special permissions
 * across Android 8.0 (API 26) through Android 16 (API 36).
 */
object PermissionHelper {

    fun getSnapshot(context: Context): PermissionStatusSnapshot {
        return PermissionStatusSnapshot(
            cameraGranted = hasCameraPermission(context),
            postNotificationsGranted = hasPostNotificationsPermission(context),
            usageStatsGranted = hasUsageStatsPermission(context),
            overlayGranted = Settings.canDrawOverlays(context),
            accessibilityEnabled = isAccessibilityServiceEnabled(context),
            notificationListenerEnabled = isNotificationListenerEnabled(context),
            ignoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
        )
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPostNotificationsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val comp = ComponentName(context, AppLockAccessibilityService::class.java)
        val fullComponent = comp.flattenToString()
        val shortComponent = comp.flattenToShortString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any {
            it.equals(fullComponent, ignoreCase = true) ||
                it.equals(shortComponent, ignoreCase = true)
        }
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName) ||
            Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )?.contains(LockNotificationListenerService::class.java.name) == true
    }

    fun usageAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun overlaySettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun accessibilityServiceComponentName(context: Context): String =
        ComponentName(context, AppLockAccessibilityService::class.java).flattenToString()

    fun hasAccessibilityServiceDeclared(context: Context): Boolean {
        return runCatching {
            val comp = ComponentName(context, AppLockAccessibilityService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getServiceInfo(
                    comp,
                    PackageManager.ComponentInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getServiceInfo(comp, 0)
            }
            true
        }.getOrDefault(false)
    }

    fun hasNotificationListenerDeclared(context: Context): Boolean {
        return runCatching {
            val comp = ComponentName(context, LockNotificationListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getServiceInfo(
                    comp,
                    PackageManager.ComponentInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getServiceInfo(comp, 0)
            }
            true
        }.getOrDefault(false)
    }

    fun accessibilitySettingsIntent(context: Context? = null): Intent {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (context != null) {
            val componentKey = accessibilityServiceComponentName(context)
            val bundle = Bundle().apply {
                putString(":settings:fragment_args_key", componentKey)
            }
            intent.putExtra(":settings:fragment_args_key", componentKey)
            intent.putExtra(":settings:show_fragment_args", bundle)
        }
        return intent
    }

    fun launchAccessibilitySettings(context: Context): Boolean {
        val targeted = runCatching {
            context.startActivity(accessibilitySettingsIntent(context))
            true
        }.getOrDefault(false)
        if (targeted) return true
        return runCatching {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }.getOrDefault(false)
    }

    fun notificationListenerSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun appPrivacySettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun systemPrivacySettingsIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_PRIVACY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun batteryOptimizationSettingsIntent(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
