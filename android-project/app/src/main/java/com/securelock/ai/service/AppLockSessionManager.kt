package com.securelock.ai.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates automatic relocking, lock-on-screen-off, foreground-persistent sessions,
 * background grace timeouts, and single-flight LockOverlayActivity launches across
 * Accessibility & Monitor services so both services never conflict or prematurely relock.
 */
object AppLockSessionManager {

    private const val POST_UNLOCK_HANDOFF_GRACE_MS = 1_500L
    private const val IMMEDIATE_LEAVE_GRACE_MS = 1_500L

    private data class UnlockEntry(
        val unlockedAtMillis: Long,
        @Volatile var lastSeenForegroundAtMillis: Long,
        @Volatile var leftForegroundAtMillis: Long? = null
    )

    private val unlockedApps = ConcurrentHashMap<String, UnlockEntry>()
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

    fun noteOverlayDismissedToHome(packageName: String, nowMillis: Long = System.currentTimeMillis()) {
        synchronized(launchLock) {
            if (activeOverlayPackage == packageName) {
                activeOverlayPackage = null
            }
            lastOverlayLaunchPackage = packageName
            lastOverlayLaunchAtMillis = nowMillis
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

    fun evaluateVaultTimeout(timeoutSeconds: Int, nowMillis: Long = System.currentTimeMillis()) {
        if (!isVaultUnlocked) return
        if (timeoutSeconds < 0) return
        if (timeoutSeconds == 0) {
            lockVault()
            return
        }
        val elapsed = nowMillis - vaultUnlockedAtMillis
        if (elapsed >= timeoutSeconds * 1000L) {
            lockVault()
        }
    }

    fun markAppUnlocked(
        packageName: String,
        timeoutSeconds: Int = -1,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        synchronized(launchLock) {
            if (activeOverlayPackage == packageName) {
                activeOverlayPackage = null
            }
            if (lastOverlayLaunchPackage == packageName) {
                lastOverlayLaunchPackage = null
                lastOverlayLaunchAtMillis = 0L
            }
        }
        unlockedApps[packageName] = UnlockEntry(
            unlockedAtMillis = nowMillis,
            lastSeenForegroundAtMillis = nowMillis,
            leftForegroundAtMillis = null
        )
    }

    fun isSystemTransientPackage(packageName: String, ownPackage: String): Boolean {
        return packageName == ownPackage ||
            packageName == "com.android.systemui" ||
            packageName == "android" ||
            packageName == "com.android.permissioncontroller" ||
            packageName == "com.google.android.permissioncontroller" ||
            packageName == "com.android.packageinstaller" ||
            packageName == "com.google.android.packageinstaller" ||
            packageName == "com.android.intentresolver" ||
            packageName == "com.android.providers.media.module" ||
            packageName == "com.google.android.providers.media.module" ||
            packageName == "com.vivo.fingerprintui" ||
            packageName == "com.vivo.biometrics" ||
            packageName == "com.samsung.android.biometrics.app.setting" ||
            packageName.contains("inputmethod", ignoreCase = true)
    }

    fun onForegroundPackageObserved(
        observedPackage: String,
        ownPackage: String,
        timeoutSeconds: Int,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (isSystemTransientPackage(observedPackage, ownPackage)) {
            return
        }

        val iterator = unlockedApps.entries.iterator()
        while (iterator.hasNext()) {
            val (unlockedPkg, entry) = iterator.next()
            if (observedPackage == unlockedPkg) {
                val leftAt = entry.leftForegroundAtMillis
                if (leftAt == null) {
                    entry.lastSeenForegroundAtMillis = nowMillis
                } else if (timeoutSeconds < 0) {
                    entry.leftForegroundAtMillis = null
                    entry.lastSeenForegroundAtMillis = nowMillis
                } else {
                    val limitMillis = if (timeoutSeconds == 0) {
                        IMMEDIATE_LEAVE_GRACE_MS
                    } else {
                        timeoutSeconds * 1000L
                    }
                    if (nowMillis - leftAt <= limitMillis) {
                        entry.leftForegroundAtMillis = null
                        entry.lastSeenForegroundAtMillis = nowMillis
                    } else {
                        iterator.remove()
                    }
                }
            } else {
                // Keep unlocked while activity transition settles right after unlocking
                if (nowMillis - entry.unlockedAtMillis < POST_UNLOCK_HANDOFF_GRACE_MS) {
                    continue
                }
                // "Until Screen Off" (< 0): keep unlocked even when kept in background
                if (timeoutSeconds < 0) {
                    if (entry.leftForegroundAtMillis == null) {
                        entry.leftForegroundAtMillis = nowMillis
                    }
                    continue
                }
                val leftAt = entry.leftForegroundAtMillis
                if (leftAt == null) {
                    entry.leftForegroundAtMillis = nowMillis
                } else {
                    val limitMillis = if (timeoutSeconds == 0) {
                        IMMEDIATE_LEAVE_GRACE_MS
                    } else {
                        timeoutSeconds * 1000L
                    }
                    if (nowMillis - leftAt >= limitMillis) {
                        iterator.remove()
                    }
                }
            }
        }
    }

    fun isAppTemporarilyUnlocked(
        packageName: String,
        timeoutSeconds: Int = -1,
        currentForegroundPackage: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val entry = unlockedApps[packageName] ?: return false

        // "Until Screen Off" keeps apps unlocked until screen off / manual relock
        if (timeoutSeconds < 0) {
            return true
        }

        val leftAt = entry.leftForegroundAtMillis
        // Never relock while user is actively using the unlocked app in foreground
        if (leftAt == null) {
            entry.lastSeenForegroundAtMillis = nowMillis
            return true
        }

        val limitMillis = if (timeoutSeconds == 0) {
            IMMEDIATE_LEAVE_GRACE_MS
        } else {
            timeoutSeconds * 1000L
        }

        if (nowMillis - leftAt <= limitMillis) {
            if (currentForegroundPackage == packageName) {
                entry.leftForegroundAtMillis = null
                entry.lastSeenForegroundAtMillis = nowMillis
            }
            return true
        }

        unlockedApps.remove(packageName)
        return false
    }

    fun relockAllApps() {
        unlockedApps.clear()
        lockVault()
    }
}
