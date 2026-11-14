package com.securelock.ai.data

import kotlinx.serialization.Serializable

@Serializable
enum class LockMethod {
    PIN,
    PATTERN
}

@Serializable
enum class AppThemeMode {
    NAVY_LIGHT_CARDS,
    NAVY_DARK,
    SYSTEM
}

@Serializable
enum class AppLanguage(val tag: String, val displayName: String) {
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    GERMAN("de", "Deutsch"),
    HINDI("hi", "हिन्दी"),
    JAPANESE("ja", "日本語")
}

@Serializable
enum class SecurityEventType {
    UNLOCK_SUCCESS,
    UNLOCK_FAILED,
    BIOMETRIC_SUCCESS,
    BIOMETRIC_FAILED,
    INTRUDER_CAPTURED,
    PROTECTED_APP_BLOCKED,
    PIN_CHANGED,
    PATTERN_CHANGED,
    RECOVERY_USED
}

@Serializable
data class SecurityEvent(
    val id: String,
    val type: SecurityEventType,
    val title: String,
    val detail: String,
    val timestampMillis: Long
)

@Serializable
data class IntruderRecord(
    val id: String,
    val timestampMillis: Long,
    val failedAttemptCount: Int,
    val targetPackage: String,
    val imagePath: String? = null
)

@Serializable
data class ProtectedNotificationRecord(
    val id: String,
    val packageName: String,
    val appName: String,
    val maskedTitle: String,
    val maskedBody: String,
    val timestampMillis: Long,
    val wasMasked: Boolean
)

@Serializable
data class ProtectionScreenConfig(
    val enabled: Boolean = false,
    val title: String = "App Unavailable",
    val message: String = "This application is currently protected by SecureLock AI. Authenticate to continue.",
    val accentHex: Long = 0xFF2563EB,
    val backgroundHex: Long = 0xFF0A1628,
    val iconKey: String = "shield"
)

@Serializable
data class DailyActivityStat(
    val dayLabel: String,
    val dateKey: String,
    val successCount: Int,
    val failedCount: Int
)

@Serializable
data class LocalStatistics(
    val totalAttempts: Int = 0,
    val successfulUnlocks: Int = 0,
    val failedAttempts: Int = 0,
    val lastActivityMillis: Long = 0L,
    val dailyHistory: List<DailyActivityStat> = emptyList(),
    val recentEvents: List<SecurityEvent> = emptyList()
)

@Serializable
data class LocalSettingsState(
    val isSetupComplete: Boolean = false,
    val preferredLockMethod: LockMethod = LockMethod.PIN,
    val biometricEnabled: Boolean = true,
    val relockTimeoutSeconds: Int = 30,
    val protectSettingsScreen: Boolean = true,
    val foregroundIntruderPromptEnabled: Boolean = true,
    val lockNotificationsEnabled: Boolean = false,
    val maskAllProtectedNotifications: Boolean = true,
    val safeBrowsingWarningsEnabled: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.NAVY_LIGHT_CARDS,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val protectedPackages: Set<String> = emptySet(),
    val notificationMaskedPackages: Set<String> = emptySet(),
    val protectionScreenConfig: ProtectionScreenConfig = ProtectionScreenConfig()
)

@Serializable
data class LocalBackupPayload(
    val formatVersion: Int = 1,
    val exportedAtMillis: Long,
    val settings: LocalSettingsState,
    val statistics: LocalStatistics
)

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isLocked: Boolean
)
