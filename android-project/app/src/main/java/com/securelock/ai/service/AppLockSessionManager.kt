package com.securelock.ai.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates automatic relocking, lock-on-screen-off, and configurable grace timeouts
 * for unlocked apps and for SecureLock AI & Secret Gallery Vault across lifecycle transitions.
 */
object AppLockSessionManager {

    private val unlockedUntilMillis = ConcurrentHashMap<String, Long>()

    private val _vaultUnlockedFlow = MutableStateFlow(false)
    val vaultUnlockedFlow: StateFlow<Boolean> = _vaultUnlockedFlow.asStateFlow()

    @Volatile
    var isVaultUnlocked: Boolean = false
        private set

    @Volatile
    var isMediaVaultUnlocked: Boolean = false
        private set

    @Volatile
    var isInternalFlowSuspendedForPicker: Boolean = false
        private set

    @Volatile
    private var vaultUnlockedAtMillis: Long = 0L

    fun setInternalFlowSuspendedForPicker(active: Boolean) {
        isInternalFlowSuspendedForPicker = active
    }

    fun unlockVault() {
        isVaultUnlocked = true
        vaultUnlockedAtMillis = System.currentTimeMillis()
        _vaultUnlockedFlow.value = true
    }

    fun lockVault() {
        isVaultUnlocked = false
        isMediaVaultUnlocked = false
        vaultUnlockedAtMillis = 0L
        _vaultUnlockedFlow.value = false
    }

    fun unlockMediaVault() {
        isMediaVaultUnlocked = true
    }

    fun lockMediaVault() {
        isMediaVaultUnlocked = false
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
