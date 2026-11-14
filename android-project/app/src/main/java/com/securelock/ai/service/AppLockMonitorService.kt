package com.securelock.ai.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.securelock.ai.LockOverlayActivity
import com.securelock.ai.MainActivity
import com.securelock.ai.R
import com.securelock.ai.SecureLockApplication
import com.securelock.ai.security.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Opt-in UsageStatsManager monitor for users who prefer Usage Access over Accessibility.
 * Respects battery budget by querying only recent UsageEvents while active and stopping cleanly
 * when disabled or when Usage Access is revoked.
 */
class AppLockMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var lastLaunchedPackage: String? = null
    private var lastLaunchTimestamp: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!PermissionHelper.hasUsageStatsPermission(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startMonitoringLoop()
        return START_STICKY
    }

    private fun startMonitoringLoop() {
        if (monitorJob?.isActive == true) return
        monitorJob = serviceScope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val repo = (application as SecureLockApplication).storageRepository

            while (isActive) {
                if (!PermissionHelper.hasUsageStatsPermission(this@AppLockMonitorService)) {
                    stopSelf()
                    break
                }

                val foregroundPackage = detectForegroundPackage(usageStatsManager)
                if (foregroundPackage != null && foregroundPackage != packageName) {
                    val settings = repo.settings.value
                    val isSettingsLocked = settings.protectSettingsScreen &&
                        foregroundPackage == "com.android.settings"
                    val isProtected = settings.protectedPackages.contains(foregroundPackage) ||
                        isSettingsLocked

                    if (isProtected &&
                        !AppLockSessionManager.isAppTemporarilyUnlocked(foregroundPackage) &&
                        Settings.canDrawOverlays(this@AppLockMonitorService)
                    ) {
                        val now = System.currentTimeMillis()
                        if (foregroundPackage != lastLaunchedPackage || now - lastLaunchTimestamp > 1500L) {
                            lastLaunchedPackage = foregroundPackage
                            lastLaunchTimestamp = now
                            val lockIntent = Intent(this@AppLockMonitorService, LockOverlayActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra(LockOverlayActivity.EXTRA_TARGET_PACKAGE, foregroundPackage)
                            }
                            startActivity(lockIntent)
                        }
                    }
                }
                delay(750L)
            }
        }
    }

    private fun detectForegroundPackage(usageStatsManager: UsageStatsManager): String? {
        val end = System.currentTimeMillis()
        val begin = end - 3_000L
        val events = usageStatsManager.queryEvents(begin, end)
        val event = UsageEvents.Event()
        var latestPackage: String? = null
        var latestTime = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED && event.timeStamp >= latestTime) {
                latestTime = event.timeStamp
                latestPackage = event.packageName
            }
        }
        return latestPackage
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.monitor_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.monitor_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("SecureLock AI Protection Active")
            .setContentText("Monitoring selected apps locally via Usage Access")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.securelock.ai.STOP_MONITOR"
        private const val CHANNEL_ID = "securelock_monitor_channel"
        private const val NOTIFICATION_ID = 4102
    }
}
