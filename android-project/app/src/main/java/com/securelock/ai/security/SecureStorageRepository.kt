package com.securelock.ai.security

import android.content.Context
import android.content.SharedPreferences
import com.securelock.ai.data.DailyActivityStat
import com.securelock.ai.data.IntruderRecord
import com.securelock.ai.data.LocalBackupPayload
import com.securelock.ai.data.LocalSettingsState
import com.securelock.ai.data.LocalStatistics
import com.securelock.ai.data.ProtectedNotificationRecord
import com.securelock.ai.data.SecurityEvent
import com.securelock.ai.data.SecurityEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Offline-first repository persisting all configuration, hashed credentials,
 * intruder metadata, notification privacy logs, and local statistics encrypted
 * via Android Keystore AES-256-GCM.
 */
class SecureStorageRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(
        PREFS_FILE,
        Context.MODE_PRIVATE
    )
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<LocalSettingsState> = _settings.asStateFlow()

    private val _statistics = MutableStateFlow(loadStatistics())
    val statistics: StateFlow<LocalStatistics> = _statistics.asStateFlow()

    private val _intruders = MutableStateFlow(loadIntruders())
    val intruders: StateFlow<List<IntruderRecord>> = _intruders.asStateFlow()

    private val _notifications = MutableStateFlow(loadNotifications())
    val notifications: StateFlow<List<ProtectedNotificationRecord>> = _notifications.asStateFlow()

    fun hasPinConfigured(): Boolean = !prefs.getString(KEY_PIN_HASH, null).isNullOrEmpty()
    fun hasPatternConfigured(): Boolean = !prefs.getString(KEY_PATTERN_HASH, null).isNullOrEmpty()
    fun hasRecoveryCodeConfigured(): Boolean = !prefs.getString(KEY_RECOVERY_HASH, null).isNullOrEmpty()

    fun setPin(pin: String) {
        val chars = pin.toCharArray()
        val encryptedHash = KeystoreCryptoManager.encrypt(KeystoreCryptoManager.hashSecret(chars))
        chars.fill('\u0000')
        prefs.edit().putString(KEY_PIN_HASH, encryptedHash).apply()
        updateSettings { it.copy(isSetupComplete = true) }
        logEvent(SecurityEventType.PIN_CHANGED, "PIN Updated", "Master security PIN updated locally")
    }

    fun verifyPin(pin: String): Boolean {
        val encrypted = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedHash = KeystoreCryptoManager.decrypt(encrypted) ?: return false
        val chars = pin.toCharArray()
        val ok = KeystoreCryptoManager.verifySecret(chars, storedHash)
        chars.fill('\u0000')
        return ok
    }

    fun setPattern(patternNodes: List<Int>) {
        val serialized = patternNodes.joinToString("-")
        val chars = serialized.toCharArray()
        val encryptedHash = KeystoreCryptoManager.encrypt(KeystoreCryptoManager.hashSecret(chars))
        chars.fill('\u0000')
        prefs.edit().putString(KEY_PATTERN_HASH, encryptedHash).apply()
        updateSettings { it.copy(isSetupComplete = true) }
        logEvent(SecurityEventType.PATTERN_CHANGED, "Pattern Updated", "Security pattern updated locally")
    }

    fun verifyPattern(patternNodes: List<Int>): Boolean {
        val encrypted = prefs.getString(KEY_PATTERN_HASH, null) ?: return false
        val storedHash = KeystoreCryptoManager.decrypt(encrypted) ?: return false
        val chars = patternNodes.joinToString("-").toCharArray()
        val ok = KeystoreCryptoManager.verifySecret(chars, storedHash)
        chars.fill('\u0000')
        return ok
    }

    fun generateAndStoreRecoveryCode(): String {
        val code = KeystoreCryptoManager.generateRecoveryCode()
        val chars = code.replace("-", "").uppercase(Locale.US).toCharArray()
        val encryptedHash = KeystoreCryptoManager.encrypt(KeystoreCryptoManager.hashSecret(chars))
        chars.fill('\u0000')
        prefs.edit().putString(KEY_RECOVERY_HASH, encryptedHash).apply()
        return code
    }

    fun verifyRecoveryCode(input: String): Boolean {
        val encrypted = prefs.getString(KEY_RECOVERY_HASH, null) ?: return false
        val storedHash = KeystoreCryptoManager.decrypt(encrypted) ?: return false
        val normalized = input.replace("-", "").trim().uppercase(Locale.US).toCharArray()
        val ok = KeystoreCryptoManager.verifySecret(normalized, storedHash)
        normalized.fill('\u0000')
        if (ok) {
            logEvent(SecurityEventType.RECOVERY_USED, "Emergency Recovery Used", "PIN reset authorized via recovery code")
        }
        return ok
    }

    fun updateSettings(transform: (LocalSettingsState) -> LocalSettingsState) {
        val updated = transform(_settings.value)
        _settings.value = updated
        val encrypted = KeystoreCryptoManager.encrypt(json.encodeToString(updated))
        prefs.edit().putString(KEY_SETTINGS, encrypted).apply()
    }

    fun recordAuthAttempt(success: Boolean, methodLabel: String, targetPackage: String = appContext.packageName) {
        val now = System.currentTimeMillis()
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val dayLabel = SimpleDateFormat("EEE", Locale.US).format(Date(now))

        val current = _statistics.value
        val baseHistory = ensureSevenDayWindow(current.dailyHistory)
        val updatedHistory = baseHistory.map { stat ->
            if (stat.dateKey == dateKey) {
                if (success) stat.copy(successCount = stat.successCount + 1)
                else stat.copy(failedCount = stat.failedCount + 1)
            } else {
                stat
            }
        }

        val eventType = when {
            success && methodLabel == "Biometric" -> SecurityEventType.BIOMETRIC_SUCCESS
            !success && methodLabel == "Biometric" -> SecurityEventType.BIOMETRIC_FAILED
            success -> SecurityEventType.UNLOCK_SUCCESS
            else -> SecurityEventType.UNLOCK_FAILED
        }

        val newEvent = SecurityEvent(
            id = UUID.randomUUID().toString(),
            type = eventType,
            title = if (success) "$methodLabel unlock succeeded" else "$methodLabel unlock failed",
            detail = "Target: $targetPackage",
            timestampMillis = now
        )

        val updatedStats = current.copy(
            totalAttempts = current.totalAttempts + 1,
            successfulUnlocks = current.successfulUnlocks + if (success) 1 else 0,
            failedAttempts = current.failedAttempts + if (success) 0 else 1,
            lastActivityMillis = now,
            dailyHistory = updatedHistory,
            recentEvents = (listOf(newEvent) + current.recentEvents).take(50)
        )
        persistStatistics(updatedStats)
    }

    fun logEvent(type: SecurityEventType, title: String, detail: String) {
        val now = System.currentTimeMillis()
        val newEvent = SecurityEvent(
            id = UUID.randomUUID().toString(),
            type = type,
            title = title,
            detail = detail,
            timestampMillis = now
        )
        val current = _statistics.value
        persistStatistics(
            current.copy(
                lastActivityMillis = now,
                recentEvents = (listOf(newEvent) + current.recentEvents).take(50)
            )
        )
    }

    fun addIntruderRecord(failedAttemptCount: Int, targetPackage: String, imagePath: String?): IntruderRecord {
        val record = IntruderRecord(
            id = UUID.randomUUID().toString(),
            timestampMillis = System.currentTimeMillis(),
            failedAttemptCount = failedAttemptCount,
            targetPackage = targetPackage,
            imagePath = imagePath
        )
        val updated = listOf(record) + _intruders.value
        _intruders.value = updated
        persistIntruders(updated)
        if (imagePath != null) {
            logEvent(
                SecurityEventType.INTRUDER_CAPTURED,
                "Foreground Intruder Photo Saved",
                "Saved to private storage after $failedAttemptCount failed attempts"
            )
        }
        return record
    }

    fun attachIntruderPhoto(recordId: String, imagePath: String) {
        val updated = _intruders.value.map {
            if (it.id == recordId) it.copy(imagePath = imagePath) else it
        }
        _intruders.value = updated
        persistIntruders(updated)
        logEvent(
            SecurityEventType.INTRUDER_CAPTURED,
            "Foreground Intruder Photo Saved",
            "User-authorized foreground snapshot stored privately"
        )
    }

    fun deleteIntruderRecord(record: IntruderRecord) {
        record.imagePath?.let { path ->
            runCatching { File(path).delete() }
        }
        val updated = _intruders.value.filterNot { it.id == record.id }
        _intruders.value = updated
        persistIntruders(updated)
    }

    fun addProtectedNotification(record: ProtectedNotificationRecord) {
        val updated = (listOf(record) + _notifications.value).take(60)
        _notifications.value = updated
        val encrypted = KeystoreCryptoManager.encrypt(json.encodeToString(updated))
        prefs.edit().putString(KEY_NOTIFICATIONS, encrypted).apply()
    }

    fun clearProtectedNotifications() {
        _notifications.value = emptyList()
        prefs.edit().remove(KEY_NOTIFICATIONS).apply()
    }

    fun resetStatistics() {
        persistStatistics(
            LocalStatistics(dailyHistory = ensureSevenDayWindow(emptyList()))
        )
    }

    fun exportLocalBackupJson(): String {
        val payload = LocalBackupPayload(
            exportedAtMillis = System.currentTimeMillis(),
            settings = _settings.value,
            statistics = _statistics.value
        )
        return json.encodeToString(payload)
    }

    fun importLocalBackupJson(rawJson: String): Boolean {
        return runCatching {
            val payload = json.decodeFromString<LocalBackupPayload>(rawJson)
            updateSettings { current ->
                payload.settings.copy(isSetupComplete = current.isSetupComplete)
            }
            persistStatistics(payload.statistics)
            true
        }.getOrDefault(false)
    }

    fun getIntruderStorageDir(): File {
        return File(appContext.filesDir, "intruders").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun persistStatistics(stats: LocalStatistics) {
        _statistics.value = stats
        val encrypted = KeystoreCryptoManager.encrypt(json.encodeToString(stats))
        prefs.edit().putString(KEY_STATS, encrypted).apply()
    }

    private fun persistIntruders(records: List<IntruderRecord>) {
        val encrypted = KeystoreCryptoManager.encrypt(json.encodeToString(records))
        prefs.edit().putString(KEY_INTRUDERS, encrypted).apply()
    }

    private fun loadSettings(): LocalSettingsState {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return LocalSettingsState()
        val decrypted = KeystoreCryptoManager.decrypt(raw) ?: return LocalSettingsState()
        return runCatching { json.decodeFromString<LocalSettingsState>(decrypted) }
            .getOrDefault(LocalSettingsState())
    }

    private fun loadStatistics(): LocalStatistics {
        val raw = prefs.getString(KEY_STATS, null)
        val parsed = raw?.let { KeystoreCryptoManager.decrypt(it) }?.let {
            runCatching { json.decodeFromString<LocalStatistics>(it) }.getOrNull()
        } ?: LocalStatistics()
        return parsed.copy(dailyHistory = ensureSevenDayWindow(parsed.dailyHistory))
    }

    private fun loadIntruders(): List<IntruderRecord> {
        val raw = prefs.getString(KEY_INTRUDERS, null) ?: return emptyList()
        val decrypted = KeystoreCryptoManager.decrypt(raw) ?: return emptyList()
        return runCatching { json.decodeFromString<List<IntruderRecord>>(decrypted) }
            .getOrDefault(emptyList())
    }

    private fun loadNotifications(): List<ProtectedNotificationRecord> {
        val raw = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val decrypted = KeystoreCryptoManager.decrypt(raw) ?: return emptyList()
        return runCatching { json.decodeFromString<List<ProtectedNotificationRecord>>(decrypted) }
            .getOrDefault(emptyList())
    }

    private fun ensureSevenDayWindow(existing: List<DailyActivityStat>): List<DailyActivityStat> {
        val byDate = existing.associateBy { it.dateKey }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val cal = Calendar.getInstance()
        val result = mutableListOf<DailyActivityStat>()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        repeat(7) {
            val d = cal.time
            val key = dateFormat.format(d)
            val label = dayFormat.format(d)
            result += byDate[key] ?: DailyActivityStat(label, key, 0, 0)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    companion object {
        private const val PREFS_FILE = "securelock_keystore_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PATTERN_HASH = "pattern_hash"
        private const val KEY_RECOVERY_HASH = "recovery_hash"
        private const val KEY_SETTINGS = "settings_json"
        private const val KEY_STATS = "stats_json"
        private const val KEY_INTRUDERS = "intruders_json"
        private const val KEY_NOTIFICATIONS = "notifications_json"
    }
}
