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
    private val activeVisitUnlocked = ConcurrentHashMap.newKeySet<String>()

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
        if (timeoutSeconds <= 0) {
            activeVisitUnlocked.add(packageName)
            unlockedUntilMillis[packageName] = System.currentTimeMillis() + 600L
        } else {
            activeVisitUnlocked.remove(packageName)
            unlockedUntilMillis[packageName] = System.currentTimeMillis() + timeoutSeconds * 1000L
        }
    }

    fun onForegroundPackageObserved(
        observedPackage: String,
        ownPackage: String,
        timeoutSeconds: Int
    ) {
        if (observedPackage == ownPackage ||
            observedPackage == "com.android.systemui" ||
            observedPackage == "android" ||
            observedPackage.contains("inputmethod", ignoreCase = true)
        ) {
            return
        }
        if (timeoutSeconds <= 0 && activeVisitUnlocked.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val iterator = activeVisitUnlocked.iterator()
            while (iterator.hasNext()) {
                val unlockedPkg = iterator.next()
                if (unlockedPkg != observedPackage) {
                    val graceUntil = unlockedUntilMillis[unlockedPkg] ?: 0L
                    if (now > graceUntil) {
                        iterator.remove()
                        unlockedUntilMillis.remove(unlockedPkg)
                    }
                }
            }
        }
    }

    fun isAppTemporarilyUnlocked(packageName: String): Boolean {
        if (activeVisitUnlocked.contains(packageName)) {
            return true
        }
        val expiresAt = unlockedUntilMillis[packageName] ?: return false
        if (System.currentTimeMillis() <= expiresAt) {
            return true
        }
        unlockedUntilMillis.remove(packageName)
        return false
    }

    fun relockAllApps() {
        unlockedUntilMillis.clear()
        activeVisitUnlocked.clear()
        lockVault()
    }
}
