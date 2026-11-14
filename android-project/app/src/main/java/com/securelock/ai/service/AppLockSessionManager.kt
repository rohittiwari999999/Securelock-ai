package com.securelock.ai.service

import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates automatic relocking and configurable grace timeouts for unlocked apps
 * and for SecureLock AI itself across Activity/Process lifecycle transitions.
 */
object AppLockSessionManager {

    private val unlockedUntilMillis = ConcurrentHashMap<String, Long>()

    @Volatile
    var isVaultUnlocked: Boolean = false
        private set

    @Volatile
    private var vaultUnlockedAtMillis: Long = 0L

    fun unlockVault() {
        isVaultUnlocked = true
        vaultUnlockedAtMillis = System.currentTimeMillis()
    }

    fun lockVault() {
        isVaultUnlocked = false
        vaultUnlockedAtMillis = 0L
    }

    fun evaluateVaultTimeout(timeoutSeconds: Int) {
        if (!isVaultUnlocked) return
        if (timeoutSeconds <= 0) {
            lockVault()
            return
        }
        val elapsed = System.currentTimeMillis() - vaultUnlockedAtMillis
        if (elapsed >= timeoutSeconds * 1000L) {
            lockVault()
        }
    }

    fun markAppUnlocked(packageName: String, timeoutSeconds: Int) {
        val graceMs = timeoutSeconds.coerceAtLeast(5) * 1000L
        unlockedUntilMillis[packageName] = System.currentTimeMillis() + graceMs
    }

    fun isAppTemporarilyUnlocked(packageName: String): Boolean {
        val expiresAt = unlockedUntilMillis[packageName] ?: return false
        if (System.currentTimeMillis() <= expiresAt) {
            return true
        }
        unlockedUntilMillis.remove(packageName)
        return false
    }

    fun relockAllApps() {
        unlockedUntilMillis.clear()
        lockVault()
    }
}
