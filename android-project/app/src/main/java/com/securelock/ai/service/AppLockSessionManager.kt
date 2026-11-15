package com.securelock.ai.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates automatic relocking, lock-on-screen-off, configurable grace timeouts,
 * and single-flight LockOverlayActivity launches across Accessibility & Monitor services
 * so both services can never launch conflicting overlays simultaneously.
 */
object AppLockSessionManager {

    private val unlockedUntilMillis = ConcurrentHashMap<String, Long>()
    private val activeVisitUnlocked = ConcurrentHashMap.newKeySet<String>()
    private val launchLock = Any()

    @Volatile
    private var lastOverlayLaunchPackage: String? = null

    @Volatile
    private var lastOverlayLaunchAtMillis: Long = 0L

    @Volatile
    var activeOverlayPackage: String? = null
        private set

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

    fun setOverlayActive(packageName: String?) {
        activeOverlayPackage = packageName
    }

    fun tryAcquireOverlayLaunch(packageName: String, cooldownMillis: Long = 1_200L): Boolean {
        synchronized(launchLock) {
            if (activeOverlayPackage == packageName) return false
            val now = System.currentTimeMillis()
            if (lastOverlayLaunchPackage == packageName &&
                now - lastOverlayLaunchAtMillis < cooldownMillis
            ) {
                return false
            }
            lastOverlayLaunchPackage = packageName
            lastOverlayLaunchAtMillis = now
            return true
        }
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
        synchronized(launchLock) {
            if (activeOverlayPackage == packageName) {
                activeOverlayPackage = null
            }
            if (lastOverlayLaunchPackage == packageName) {
                lastOverlayLaunchPackage = null
                lastOverlayLaunchAtMillis = 0L
            }
        }
        if (timeoutSeconds <= 0) {
            activeVisitUnlocked.add(packageName)
            unlockedUntilMillis[packageName] = System.currentTimeMillis() + 600L
        } else {
            activeVisitUnlocked.remove(packageName)
            unlockedUntilMillis[packageName] = System.currentTimeMillis() + timeoutSeconds * 1000L
        }
    }

    fun isSystemTransientPackage(packageName: String, ownPackage: String): Boolean {
        return packageName == ownPackage ||
            packageName == "com.android.systemui" ||
            packageName == "android" ||
            packageName == "com.android.permissioncontroller" ||
            packageName == "com.google.android.permissioncontroller" ||
            packageName == "com.android.packageinstaller" ||
            packageName == "com.google.android.packageinstaller" ||
            packageName.contains("inputmethod", ignoreCase = true)
    }

    fun onForegroundPackageObserved(
        observedPackage: String,
        ownPackage: String,
        timeoutSeconds: Int
    ) {
        if (isSystemTransientPackage(observedPackage, ownPackage)) {
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
