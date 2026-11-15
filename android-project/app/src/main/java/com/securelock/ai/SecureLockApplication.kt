package com.securelock.ai

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.security.VaultRepository
import com.securelock.ai.service.AppLockServiceController
import com.securelock.ai.service.AppLockSessionManager

/**
 * Application entry point managing encrypted storage, encrypted media vault,
 * lock-on-screen-off receiver, and ProcessLifecycleOwner auto-relock.
 */
class SecureLockApplication : Application(), DefaultLifecycleObserver {

    lateinit var storageRepository: SecureStorageRepository
        private set

    lateinit var vaultRepository: VaultRepository
        private set

    private var backgroundTimestampMillis: Long = 0L

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF &&
                storageRepository.settings.value.lockOnScreenOff
            ) {
                AppLockSessionManager.relockAllApps()
            }
        }
    }

    override fun onCreate() {
        super<Application>.onCreate()
        storageRepository = SecureStorageRepository(this)
        vaultRepository = VaultRepository(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        AppLockServiceController.syncMonitorService(this, storageRepository.settings.value)
    }

    override fun onStop(owner: LifecycleOwner) {
        backgroundTimestampMillis = System.currentTimeMillis()
        vaultRepository.wipeTempPlaybackFiles()
        val timeoutSeconds = storageRepository.settings.value.relockTimeoutSeconds
        val secretDateActive = storageRepository.isSecretDateUnlockActive()
        if (timeoutSeconds == 0 ||
            (secretDateActive && !AppLockSessionManager.isInternalFlowSuspendedForPicker)
        ) {
            AppLockSessionManager.lockVault()
        } else {
            AppLockSessionManager.lockMediaVault()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        AppLockSessionManager.setInternalFlowSuspendedForPicker(false)
        AppLockServiceController.syncMonitorService(this, storageRepository.settings.value)
        if (backgroundTimestampMillis == 0L) return
        val timeoutSeconds = storageRepository.settings.value.relockTimeoutSeconds
        if (timeoutSeconds <= 0) return
        val elapsedMillis = System.currentTimeMillis() - backgroundTimestampMillis
        if (elapsedMillis >= timeoutSeconds * 1000L) {
            AppLockSessionManager.lockVault()
        }
    }
}
