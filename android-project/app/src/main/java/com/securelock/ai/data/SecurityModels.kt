package com.securelock.ai.data

import kotlinx.serialization.Serializable

@Serializable
enum class LockMethod(val label: String) {
    PIN_4("4-Digit PIN"),
    PIN_6("6-Digit PIN"),
    PIN_CUSTOM("Custom PIN"),
    PATTERN("Pattern Lock"),
    PASSWORD("Alphanumeric Password")
}

@Serializable
enum class AppThemeId(val displayName: String) {
    MIDNIGHT_BLUE("Midnight Blue"),
    BLACK_GOLD("Black Gold"),
    NEON_PURPLE("Neon Purple"),
    PURE_WHITE("Pure White"),
    DARK_AMOLED("Dark AMOLED"),
    CYBER_GREEN("Cyber Green"),
    OCEAN_BLUE("Ocean Blue"),
    ROSE_GOLD("Rose Gold")
}

@Serializable
enum class CardStyleMode(val displayName: String) {
    ELEVATED("Rounded Card"),
    OUTLINED("Crisp Outlined"),
    COMPACT("Compact Tile")
}

@Serializable
enum class AppIconStyle(val displayName: String, val aliasClassName: String) {
    DEFAULT_SHIELD("SecureLock Shield", "com.securelock.ai.MainActivityDefault"),
    VAULT_SAFE("Midnight Vault", "com.securelock.ai.MainActivityVault"),
    MINIMAL_LOCK("Minimalist Lock", "com.securelock.ai.MainActivityMinimal"),
    UTILITY_CALC("Utility Disguise", "com.securelock.ai.MainActivityUtility")
}

@Serializable
enum class DecoyMode(val label: String, val defaultTitle: String, val defaultMessage: String) {
    APP_STOPPED_SIMULATED(
        "App Has Stopped (Simulated)",
        "Application Paused (Simulated)",
        "This simulated notice is part of SecureLock protection. Verify your identity to continue."
    ),
    APPLICATION_UNAVAILABLE(
        "Application Unavailable",
        "Application Unavailable",
        "This application is currently protected by SecureLock AI. Authenticate to continue."
    ),
    LOADING_ERROR(
        "Loading Error",
        "Unable to Load Content",
        "Simulated protection cover active. Authenticate to resume your session."
    ),
    NETWORK_ERROR(
        "Network Error",
        "Connection Offline",
        "Simulated network interruption screen. Unlock SecureLock AI to open this app."
    ),
    BLANK_SCREEN(
        "Blank Screen",
        "",
        ""
    ),
    CUSTOM_MESSAGE(
        "Custom Error Message",
        "Protected Space",
        "Custom decoy message configured in SecureLock AI."
    )
}

@Serializable
enum class VaultMediaType {
    PHOTO,
    VIDEO
}

@Serializable
data class VaultAlbum(
    val id: String,
    val name: String,
    val createdAtMillis: Long
)

@Serializable
data class VaultMediaItem(
    val id: String,
    val albumId: String,
    val displayName: String,
    val mimeType: String,
    val mediaType: VaultMediaType,
    val encryptedFilePath: String,
    val originalSizeBytes: Long,
    val addedAtMillis: Long
)

@Serializable
data class VaultBackupEntry(
    val item: VaultMediaItem,
    val encryptedPayloadBase64: String
)

@Serializable
data class VaultBackupPayload(
    val formatVersion: Int = 1,
    val exportedAtMillis: Long,
    val albums: List<VaultAlbum>,
    val entries: List<VaultBackupEntry>
)

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
    PASSWORD_CHANGED,
    VAULT_IMPORT,
    VAULT_EXPORT,
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
    val methodAttempted: String = "PIN",
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
    val decoyMode: DecoyMode = DecoyMode.APPLICATION_UNAVAILABLE,
    val title: String = "Application Unavailable",
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
data class SecretDateConfig(
    val enabled: Boolean = true,
    val day: Int = 15,
    val month: Int = 6,
    val year: Int? = null
)

@Serializable
data class LocalSettingsState(
    val isSetupComplete: Boolean = false,
    val preferredLockMethod: LockMethod = LockMethod.PIN_4,
    val customPinLength: Int = 4,
    val perAppLockMethods: Map<String, LockMethod> = emptyMap(),
    val biometricEnabled: Boolean = true,
    val relockTimeoutSeconds: Int = -1,
    val lockOnScreenOff: Boolean = true,
    val protectSettingsScreen: Boolean = true,
    val foregroundIntruderPromptEnabled: Boolean = true,
    val intruderAttemptThreshold: Int = 3,
    val lockNotificationsEnabled: Boolean = false,
    val maskAllProtectedNotifications: Boolean = true,
    val safeBrowsingWarningsEnabled: Boolean = true,
    val themeId: AppThemeId = AppThemeId.MIDNIGHT_BLUE,
    val customAccentHex: Long? = null,
    val forceLightMode: Boolean? = null,
    val cardStyleMode: CardStyleMode = CardStyleMode.ELEVATED,
    val appIconStyle: AppIconStyle = AppIconStyle.DEFAULT_SHIELD,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val protectedPackages: Set<String> = emptySet(),
    val notificationMaskedPackages: Set<String> = emptySet(),
    val protectionScreenConfig: ProtectionScreenConfig = ProtectionScreenConfig(),
    val secretDateConfig: SecretDateConfig = SecretDateConfig()
)

@Serializable
data class LocalBackupPayload(
    val formatVersion: Int = 2,
    val exportedAtMillis: Long,
    val settings: LocalSettingsState,
    val statistics: LocalStatistics
)

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isLocked: Boolean,
    val overrideMethod: LockMethod? = null
)
