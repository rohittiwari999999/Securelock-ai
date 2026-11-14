package com.securelock.ai

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.service.AppLockSessionManager

/**
 * Application entry point managing encrypted storage initialization and
 * lifecycle-aware automatic relocking across background/foreground transitions
 * and Android process restarts.
 */
class SecureLockApplication : Application(), DefaultLifecycleObserver {

    lateinit var storageRepository: SecureStorageRepository
        private set

    private var backgroundTimestampMillis: Long = 0L

    override fun onCreate() {
        super<Application>.onCreate()
        storageRepository = SecureStorageRepository(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        backgroundTimestampMillis = System.currentTimeMillis()
        val timeoutSeconds = storageRepository.settings.value.relockTimeoutSeconds
        if (timeoutSeconds == 0) {
            AppLockSessionManager.lockVault()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (backgroundTimestampMillis == 0L) return
        val timeoutSeconds = storageRepository.settings.value.relockTimeoutSeconds
        val elapsedMillis = System.currentTimeMillis() - backgroundTimestampMillis
        if (elapsedMillis >= timeoutSeconds * 1000L) {
            AppLockSessionManager.lockVault()
        }
    }
}
