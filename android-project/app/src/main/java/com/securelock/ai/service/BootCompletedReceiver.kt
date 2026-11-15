package com.securelock.ai.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.securelock.ai.SecureLockApplication

/**
 * Re-arms the UsageStats app-lock monitor automatically after device reboot or app update.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context ?: return
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val app = ctx.applicationContext as? SecureLockApplication ?: return
            AppLockServiceController.syncMonitorService(ctx, app.storageRepository.settings.value)
        }
    }
}
