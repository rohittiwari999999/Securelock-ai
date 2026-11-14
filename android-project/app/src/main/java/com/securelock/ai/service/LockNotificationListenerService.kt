package com.securelock.ai.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.securelock.ai.SecureLockApplication
import com.securelock.ai.data.ProtectedNotificationRecord
import java.util.UUID

/**
 * Optional local-only NotificationListenerService.
 * When enabled by the user, hides sensitive notification previews inside SecureLock's
 * local privacy vault for selected packages. Never transmits notification data anywhere.
 */
class LockNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName == packageName) return

        val repo = (application as SecureLockApplication).storageRepository
        val settings = repo.settings.value
        if (!settings.lockNotificationsEnabled) return

        val shouldMask = settings.notificationMaskedPackages.contains(sbn.packageName) ||
            (settings.maskAllProtectedNotifications && settings.protectedPackages.contains(sbn.packageName))

        if (!shouldMask) return

        val extras = sbn.notification.extras
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val appLabel = resolveAppLabel(sbn.packageName)

        val record = ProtectedNotificationRecord(
            id = UUID.randomUUID().toString(),
            packageName = sbn.packageName,
            appName = appLabel,
            maskedTitle = if (rawTitle.isBlank()) "Protected Notification" else "${rawTitle.take(2)}••••",
            maskedBody = "Sensitive content hidden by SecureLock AI",
            timestampMillis = sbn.postTime,
            wasMasked = true
        )
        repo.addProtectedNotification(record)

        // Cancel original notification from shade when masking is active so preview content stays hidden
        runCatching { cancelNotification(sbn.key) }
    }

    private fun resolveAppLabel(pkg: String): String {
        return runCatching {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(pkg)
    }
}
