package com.securelock.ai.security

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import android.util.Base64
import com.securelock.ai.data.VaultAlbum
import com.securelock.ai.data.VaultBackupEntry
import com.securelock.ai.data.VaultBackupPayload
import com.securelock.ai.data.VaultMediaItem
import com.securelock.ai.data.VaultMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

data class VaultStorageInfo(
    val availableInternalBytes: Long,
    val vaultUsedBytes: Long
)

data class VaultImportResult(
    val importedCount: Int,
    val skippedCount: Int,
    val errorMessage: String? = null
)

/**
 * Encrypted local Secret Gallery Vault repository.
 * - Stores encrypted media exclusively inside app-private filesDir/vault/
 * - Never leaves unprotected copies in the vault directory
 * - Supports multiple albums, multi-select photo/video import, search, SAF export, and encrypted backup/restore.
 */
class VaultRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val vaultDir = File(appContext.filesDir, "vault").apply {
        if (!exists()) mkdirs()
    }
    private val tempPlaybackDir = File(appContext.cacheDir, "vault_playback").apply {
        if (!exists()) mkdirs()
    }

    private val _albums = MutableStateFlow(loadAlbums())
    val albums: StateFlow<List<VaultAlbum>> = _albums.asStateFlow()

    private val _mediaItems = MutableStateFlow(loadMediaItems())
    val mediaItems: StateFlow<List<VaultMediaItem>> = _mediaItems.asStateFlow()

    init {
        if (_albums.value.isEmpty()) {
            val defaultAlbum = VaultAlbum(
                id = DEFAULT_ALBUM_ID,
                name = "Private Roll",
                createdAtMillis = System.currentTimeMillis()
            )
            persistAlbums(listOf(defaultAlbum))
        }
        wipeTempPlaybackFiles()
    }

    fun createAlbum(name: String): VaultAlbum {
        val trimmed = name.trim().ifBlank { "New Album" }
        val album = VaultAlbum(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            createdAtMillis = System.currentTimeMillis()
        )
        persistAlbums(_albums.value + album)
        return album
    }

    fun renameAlbum(albumId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        persistAlbums(_albums.value.map { if (it.id == albumId) it.copy(name = trimmed) else it })
    }

    fun deleteAlbum(albumId: String, deleteContainedMedia: Boolean) {
        if (albumId == DEFAULT_ALBUM_ID && _albums.value.size <= 1) return
        val fallbackId = _albums.value.firstOrNull { it.id != albumId }?.id ?: DEFAULT_ALBUM_ID

        if (deleteContainedMedia) {
            val toDelete = _mediaItems.value.filter { it.albumId == albumId }
            toDelete.forEach { item ->
                runCatching { File(item.encryptedFilePath).delete() }
            }
            persistMediaItems(_mediaItems.value.filterNot { it.albumId == albumId })
        } else {
            persistMediaItems(
                _mediaItems.value.map {
                    if (it.albumId == albumId) it.copy(albumId = fallbackId) else it
                }
            )
        }
        persistAlbums(_albums.value.filterNot { it.id == albumId })
    }

    suspend fun importMedia(uris: List<Uri>, albumId: String): VaultImportResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext VaultImportResult(0, 0)
        var imported = 0
        var skipped = 0
        var lastError: String? = null
        val addedItems = mutableListOf<VaultMediaItem>()

        for (uri in uris) {
            val result = runCatching {
                val resolver = appContext.contentResolver
                val mime = resolver.getType(uri) ?: "application/octet-stream"
                val isVideo = mime.startsWith("video/")
                val isImage = mime.startsWith("image/")
                if (!isVideo && !isImage) {
                    skipped++
                    return@runCatching
                }

                val displayName = resolveDisplayName(uri) ?: "vault_media_${System.currentTimeMillis()}"
                val plainBytes = resolver.openInputStream(uri)?.use { stream ->
                    val size = stream.available().toLong()
                    if (size > MAX_SINGLE_FILE_BYTES) {
                        throw IllegalStateException("File exceeds 64 MB local cipher limit")
                    }
                    stream.readBytes()
                } ?: throw IllegalStateException("Unable to read selected item")

                if (plainBytes.size > MAX_SINGLE_FILE_BYTES) {
                    plainBytes.fill(0)
                    throw IllegalStateException("File exceeds 64 MB local cipher limit")
                }

                val encryptedBytes = KeystoreCryptoManager.encryptBytes(plainBytes)
                plainBytes.fill(0)

                val itemId = UUID.randomUUID().toString()
                val outFile = File(vaultDir, "$itemId.slv")
                outFile.writeBytes(encryptedBytes)

                addedItems += VaultMediaItem(
                    id = itemId,
                    albumId = albumId,
                    displayName = displayName,
                    mimeType = mime,
                    mediaType = if (isVideo) VaultMediaType.VIDEO else VaultMediaType.PHOTO,
                    encryptedFilePath = outFile.absolutePath,
                    originalSizeBytes = plainBytes.size.toLong(),
                    addedAtMillis = System.currentTimeMillis()
                )
                imported++
            }
            result.onFailure { err ->
                skipped++
                lastError = err.message
            }
        }

        if (addedItems.isNotEmpty()) {
            persistMediaItems(addedItems + _mediaItems.value)
        }
        VaultImportResult(imported, skipped, lastError)
    }

    suspend fun decryptMediaBytes(item: VaultMediaItem): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(item.encryptedFilePath)
            if (!file.exists()) return@runCatching null
            KeystoreCryptoManager.decryptBytes(file.readBytes())
        }.getOrNull()
    }

    /**
     * Decrypts a video into a short-lived private cache file for MediaPlayer/VideoView playback.
     * Callers MUST invoke wipeTempPlaybackFiles() when playback finishes or screen disposes.
     */
    suspend fun createTempPlaybackFile(item: VaultMediaItem): File? = withContext(Dispatchers.IO) {
        wipeTempPlaybackFiles()
        val plain = decryptMediaBytes(item) ?: return@withContext null
        val ext = if (item.mimeType.contains("mp4")) "mp4" else "bin"
        val tempFile = File(tempPlaybackDir, "play_${item.id}.$ext")
        tempFile.writeBytes(plain)
        plain.fill(0)
        tempFile
    }

    fun wipeTempPlaybackFiles() {
        runCatching {
            tempPlaybackDir.listFiles()?.forEach { it.delete() }
        }
    }

    suspend fun exportDecryptedMedia(item: VaultMediaItem, destinationUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val plain = decryptMediaBytes(item) ?: return@runCatching false
                appContext.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    out.write(plain)
                } ?: false
                plain.fill(0)
                true
            }.getOrDefault(false)
        }

    fun deleteMediaItems(ids: Set<String>) {
        if (ids.isEmpty()) return
        val remaining = _mediaItems.value.filterNot { item ->
            if (ids.contains(item.id)) {
                runCatching { File(item.encryptedFilePath).delete() }
                true
            } else {
                false
            }
        }
        persistMediaItems(remaining)
    }

    suspend fun exportEncryptedBackup(destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val entries = _mediaItems.value.mapNotNull { item ->
                val file = File(item.encryptedFilePath)
                if (!file.exists()) null
                else VaultBackupEntry(
                    item = item,
                    encryptedPayloadBase64 = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                )
            }
            val payload = VaultBackupPayload(
                exportedAtMillis = System.currentTimeMillis(),
                albums = _albums.value,
                entries = entries
            )
            val sealedJson = KeystoreCryptoManager.encrypt(json.encodeToString(payload))
            appContext.contentResolver.openOutputStream(destinationUri)?.use { out ->
                out.write(sealedJson.toByteArray(Charsets.UTF_8))
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
    }

    suspend fun restoreEncryptedBackup(sourceUri: Uri): Int = withContext(Dispatchers.IO) {
        runCatching {
            val sealed = appContext.contentResolver.openInputStream(sourceUri)
                ?.bufferedReader()
                ?.use { it.readText() } ?: return@runCatching 0
            val rawJson = KeystoreCryptoManager.decrypt(sealed) ?: return@runCatching 0
            val payload = json.decodeFromString<VaultBackupPayload>(rawJson)

            val mergedAlbums = (_albums.value + payload.albums).distinctBy { it.id }
            persistAlbums(mergedAlbums)

            val existingIds = _mediaItems.value.map { it.id }.toMutableSet()
            val restoredItems = mutableListOf<VaultMediaItem>()
            for (entry in payload.entries) {
                if (existingIds.contains(entry.item.id)) continue
                val encryptedBytes = Base64.decode(entry.encryptedPayloadBase64, Base64.NO_WRAP)
                // Verify ciphertext integrity with local Keystore key before committing
                val verified = KeystoreCryptoManager.decryptBytes(encryptedBytes) ?: continue
                verified.fill(0)

                val targetFile = File(vaultDir, "${entry.item.id}.slv")
                targetFile.writeBytes(encryptedBytes)
                restoredItems += entry.item.copy(encryptedFilePath = targetFile.absolutePath)
                existingIds += entry.item.id
            }
            if (restoredItems.isNotEmpty()) {
                persistMediaItems(restoredItems + _mediaItems.value)
            }
            restoredItems.size
        }.getOrDefault(0)
    }

    fun getStorageInfo(): VaultStorageInfo {
        val statFs = StatFs(appContext.filesDir.absolutePath)
        val available = statFs.availableBytes
        val used = vaultDir.listFiles()?.sumOf { it.length() } ?: 0L
        return VaultStorageInfo(availableInternalBytes = available, vaultUsedBytes = used)
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return runCatching {
            appContext.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    private fun persistAlbums(list: List<VaultAlbum>) {
        _albums.value = list
        val encrypted = KeystoreCryptoManager.encrypt(json.encodeToString(list))
        prefs.edit().putString(KEY_ALBUMS, encrypted).apply()
    }

    private fun persistMediaItems(list: List<VaultMediaItem>) {
        _mediaItems.value = list
        val encrypted = KeystoreCryptoManager.encrypt(json.encodeToString(list))
        prefs.edit().putString(KEY_MEDIA, encrypted).apply()
    }

    private fun loadAlbums(): List<VaultAlbum> {
        val raw = prefs.getString(KEY_ALBUMS, null) ?: return emptyList()
        val dec = KeystoreCryptoManager.decrypt(raw) ?: return emptyList()
        return runCatching { json.decodeFromString<List<VaultAlbum>>(dec) }.getOrDefault(emptyList())
    }

    private fun loadMediaItems(): List<VaultMediaItem> {
        val raw = prefs.getString(KEY_MEDIA, null) ?: return emptyList()
        val dec = KeystoreCryptoManager.decrypt(raw) ?: return emptyList()
        return runCatching { json.decodeFromString<List<VaultMediaItem>>(dec) }.getOrDefault(emptyList())
    }

    companion object {
        const val DEFAULT_ALBUM_ID = "default_private_roll"
        private const val VAULT_PREFS = "securelock_vault_index"
        private const val KEY_ALBUMS = "albums_json"
        private const val KEY_MEDIA = "media_json"
        private const val MAX_SINGLE_FILE_BYTES = 64L * 1024L * 1024L
    }
}
