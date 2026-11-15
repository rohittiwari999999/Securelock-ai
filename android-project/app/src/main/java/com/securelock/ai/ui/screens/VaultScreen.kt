package com.securelock.ai.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import com.securelock.ai.data.SecurityEventType
import com.securelock.ai.data.VaultAlbum
import com.securelock.ai.data.VaultMediaItem
import com.securelock.ai.data.VaultMediaType
import com.securelock.ai.security.BiometricAuthManager
import com.securelock.ai.security.BiometricAvailability
import com.securelock.ai.security.SecureStorageRepository
import com.securelock.ai.security.VaultRepository
import com.securelock.ai.service.AppLockSessionManager
import com.securelock.ai.ui.components.PatternLockPad
import com.securelock.ai.ui.components.SecureTopBar
import com.securelock.ai.ui.components.WhiteSecurityCard
import com.securelock.ai.ui.theme.CrimsonAlert
import com.securelock.ai.ui.theme.EmeraldSafe
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultScreen(
    activity: FragmentActivity,
    repository: SecureStorageRepository,
    vaultRepository: VaultRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by repository.settings.collectAsState()
    val albums by vaultRepository.albums.collectAsState()
    val mediaItems by vaultRepository.mediaItems.collectAsState()

    var vaultGateUnlocked by remember { mutableStateOf(AppLockSessionManager.isMediaVaultUnlocked) }
    var gatePinInput by remember { mutableStateOf("") }
    var gateError by remember { mutableStateOf<String?>(null) }

    var selectedAlbumId by remember(albums) {
        mutableStateOf(albums.firstOrNull()?.id ?: VaultRepository.DEFAULT_ALBUM_ID)
    }
    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var renameTargetAlbum by remember { mutableStateOf<VaultAlbum?>(null) }
    var deleteTargetAlbum by remember { mutableStateOf<VaultAlbum?>(null) }
    var viewingItem by remember { mutableStateOf<VaultMediaItem?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<VaultMediaItem?>(null) }
    var pendingExportItem by remember { mutableStateOf<VaultMediaItem?>(null) }

    val storageInfo = remember(mediaItems.size) { vaultRepository.getStorageInfo() }

    val multiImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) {
            statusMessage = "Import cancelled."
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val res = vaultRepository.importMedia(uris, selectedAlbumId)
            if (res.importedCount > 0) {
                repository.logEvent(
                    SecurityEventType.VAULT_IMPORT,
                    "Vault Media Encrypted",
                    "Encrypted ${res.importedCount} item(s) into Keystore vault"
                )
            }
            statusMessage = buildString {
                append("Encrypted ${res.importedCount} item(s).")
                if (res.skippedCount > 0) append(" Skipped ${res.skippedCount}.")
                res.errorMessage?.let { append(" ($it)") }
            }
        }
    }

    val exportMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val item = pendingExportItem ?: return@rememberLauncherForActivityResult
        pendingExportItem = null
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = vaultRepository.exportDecryptedMedia(item, uri)
            if (ok) {
                repository.logEvent(
                    SecurityEventType.VAULT_EXPORT,
                    "Vault Media Exported",
                    "Authenticated export of ${item.displayName}"
                )
            }
            statusMessage = if (ok) "Exported ${item.displayName} securely." else "Export failed."
        }
    }

    val backupVaultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = vaultRepository.exportEncryptedBackup(uri)
            statusMessage = if (ok) "Encrypted vault backup saved." else "Vault backup failed."
        }
    }

    val restoreVaultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val restored = vaultRepository.restoreEncryptedBackup(uri)
            statusMessage = if (restored > 0) "Restored $restored encrypted item(s)." else "No items restored or wrong Keystore key."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            vaultRepository.wipeTempPlaybackFiles()
        }
    }

    if (!vaultGateUnlocked) {
        val hasPattern = remember(settings.preferredLockMethod) { repository.hasPatternConfigured() }
        val hasPin = remember(settings.preferredLockMethod) { repository.hasPinConfigured() }
        val bioAvailable = remember(activity) {
            BiometricAuthManager.checkAvailability(activity) is BiometricAvailability.Available
        }
        val fingerprintReady = settings.biometricEnabled && bioAvailable
        var usePatternMode by remember(settings.preferredLockMethod, hasPattern, hasPin) {
            mutableStateOf(
                when {
                    settings.preferredLockMethod == com.securelock.ai.data.LockMethod.PATTERN && hasPattern -> true
                    hasPin -> false
                    else -> hasPattern
                }
            )
        }

        fun launchVaultBiometric() {
            if (!fingerprintReady) return
            BiometricAuthManager.authenticate(
                activity = activity,
                title = "Unlock Secret Gallery Vault",
                subtitle = "Verify fingerprint to decrypt your private albums",
                negativeButtonText = "Use Pattern / PIN",
                onSuccess = {
                    AppLockSessionManager.unlockMediaVault()
                    vaultGateUnlocked = true
                },
                onFailedAttempt = { gateError = "Fingerprint did not match" },
                onError = { gateError = it }
            )
        }

        LaunchedEffect(fingerprintReady) {
            if (fingerprintReady) {
                launchVaultBiometric()
            }
        }

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                SecureTopBar(title = "Secret Gallery Vault", subtitle = "Authentication required", onBack = onBack)
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Vault Locked",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Use Pattern, PIN, or Fingerprint to access AES-256-GCM encrypted photos and videos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
                )

                if (hasPattern && hasPin) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = usePatternMode,
                            onClick = {
                                usePatternMode = true
                                gateError = null
                            },
                            label = { Text("Pattern") }
                        )
                        FilterChip(
                            selected = !usePatternMode,
                            onClick = {
                                usePatternMode = false
                                gateError = null
                            },
                            label = { Text("PIN") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (usePatternMode && hasPattern) {
                    PatternLockPad(
                        onPatternComplete = { nodes ->
                            if (nodes.size < 4) {
                                gateError = "Connect at least 4 dots"
                            } else if (repository.verifyPattern(nodes)) {
                                gateError = null
                                AppLockSessionManager.unlockMediaVault()
                                vaultGateUnlocked = true
                            } else {
                                gateError = "Incorrect pattern"
                            }
                        }
                    )
                    gateError?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(it, color = CrimsonAlert, style = MaterialTheme.typography.labelMedium)
                    }
                    if (fingerprintReady) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { launchVaultBiometric() }) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Text("  Use Fingerprint")
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = gatePinInput,
                        onValueChange = { if (it.length <= 12 && it.all(Char::isDigit)) gatePinInput = it },
                        label = { Text("App Lock PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    gateError?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(it, color = CrimsonAlert, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (repository.verifyPin(gatePinInput)) {
                                    AppLockSessionManager.unlockMediaVault()
                                    vaultGateUnlocked = true
                                    gatePinInput = ""
                                } else {
                                    gateError = "Incorrect PIN"
                                    gatePinInput = ""
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Unlock Vault")
                        }
                        if (fingerprintReady) {
                            OutlinedButton(onClick = { launchVaultBiometric() }) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null)
                                Text("  Fingerprint")
                            }
                        }
                    }
                }
            }
        }
        return
    }

    val filteredItems = remember(mediaItems, selectedAlbumId, searchQuery) {
        mediaItems.filter { item ->
            val inAlbum = item.albumId == selectedAlbumId
            val matchesQuery = searchQuery.isBlank() ||
                item.displayName.contains(searchQuery, ignoreCase = true) ||
                item.mediaType.name.contains(searchQuery, ignoreCase = true)
            inAlbum && matchesQuery
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            SecureTopBar(
                title = "Secret Gallery Vault",
                subtitle = "${mediaItems.size} encrypted items · ${formatBytes(storageInfo.vaultUsedBytes)} used",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle grid/list"
                        )
                    }
                    IconButton(onClick = { showCreateAlbumDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Create album")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WhiteSecurityCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Private AES-256-GCM Storage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${formatBytes(storageInfo.availableInternalBytes)} free",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldSafe
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                AppLockSessionManager.setInternalFlowSuspendedForPicker(true)
                                multiImportLauncher.launch(arrayOf("image/*", "video/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("  Import Photos/Videos")
                        }
                        OutlinedButton(onClick = {
                            AppLockSessionManager.setInternalFlowSuspendedForPicker(true)
                            backupVaultLauncher.launch("securelock_vault_backup.slvbak")
                        }) {
                            Text("Backup")
                        }
                        OutlinedButton(onClick = {
                            AppLockSessionManager.setInternalFlowSuspendedForPicker(true)
                            restoreVaultLauncher.launch(arrayOf("*/*"))
                        }) {
                            Text("Restore")
                        }
                    }
                    statusMessage?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Albums row with Rename & Delete actions
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(albums, key = { it.id }) { album ->
                    val selected = album.id == selectedAlbumId
                    FilterChip(
                        selected = selected,
                        onClick = { selectedAlbumId = album.id },
                        label = { Text(album.name) },
                        trailingIcon = if (selected) {
                            {
                                Row {
                                    IconButton(
                                        onClick = { renameTargetAlbum = album },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename album", modifier = Modifier.size(14.dp))
                                    }
                                    if (albums.size > 1) {
                                        IconButton(
                                            onClick = { deleteTargetAlbum = album },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete album", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        } else null
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search private media...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (filteredItems.isEmpty()) {
                WhiteSecurityCard {
                    Text(
                        text = "No encrypted photos or videos in this album yet. Tap Import Photos/Videos to select multiple files from your gallery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        VaultMediaGridTile(
                            item = item,
                            vaultRepository = vaultRepository,
                            onClick = { viewingItem = item },
                            onDelete = { pendingDeleteItem = item }
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        WhiteSecurityCard(onClick = { viewingItem = item }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${item.mediaType.name} · ${formatBytes(item.originalSizeBytes)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { pendingDeleteItem = item }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonAlert)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateAlbumDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateAlbumDialog = false },
            title = { Text("Create Private Album") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Album Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val created = vaultRepository.createAlbum(name)
                    selectedAlbumId = created.id
                    showCreateAlbumDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAlbumDialog = false }) { Text("Cancel") }
            }
        )
    }

    renameTargetAlbum?.let { target ->
        var name by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renameTargetAlbum = null },
            title = { Text("Rename Album") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("New Album Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    vaultRepository.renameAlbum(target.id, name)
                    renameTargetAlbum = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetAlbum = null }) { Text("Cancel") }
            }
        )
    }

    deleteTargetAlbum?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTargetAlbum = null },
            title = { Text("Delete Album \"${target.name}\"?") },
            text = {
                Text("Protect against accidental loss: choose whether to move encrypted items to another album or permanently erase them.")
            },
            confirmButton = {
                Button(onClick = {
                    vaultRepository.deleteAlbum(target.id, deleteContainedMedia = false)
                    selectedAlbumId = albums.firstOrNull { it.id != target.id }?.id ?: VaultRepository.DEFAULT_ALBUM_ID
                    deleteTargetAlbum = null
                }) { Text("Move Items & Delete Album") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        vaultRepository.deleteAlbum(target.id, deleteContainedMedia = true)
                        selectedAlbumId = albums.firstOrNull { it.id != target.id }?.id ?: VaultRepository.DEFAULT_ALBUM_ID
                        deleteTargetAlbum = null
                    }) { Text("Delete All", color = CrimsonAlert) }
                    TextButton(onClick = { deleteTargetAlbum = null }) { Text("Cancel") }
                }
            }
        )
    }

    pendingDeleteItem?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            title = { Text("Permanently Delete Media?") },
            text = { Text("Remove ${target.displayName} from encrypted vault storage? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        vaultRepository.deleteMediaItems(setOf(target.id))
                        pendingDeleteItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteItem = null }) { Text("Cancel") }
            }
        )
    }

    viewingItem?.let { item ->
        VaultMediaViewerDialog(
            activity = activity,
            repository = repository,
            vaultRepository = vaultRepository,
            item = item,
            onRequestExport = {
                pendingExportItem = item
                AppLockSessionManager.setInternalFlowSuspendedForPicker(true)
                exportMediaLauncher.launch(item.displayName)
            },
            onDismiss = {
                vaultRepository.wipeTempPlaybackFiles()
                viewingItem = null
            }
        )
    }
}

@Composable
private fun VaultMediaGridTile(
    item: VaultMediaItem,
    vaultRepository: VaultRepository,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var bitmap by remember(item.id) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(item.id) {
        if (item.mediaType == VaultMediaType.PHOTO) {
            val bytes = vaultRepository.decryptMediaBytes(item)
            if (bytes != null) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                bytes.fill(0)
            }
        }
    }

    WhiteSecurityCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (item.mediaType == VaultMediaType.VIDEO) Icons.Default.PlayCircle else Icons.Default.Photo,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonAlert, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun VaultMediaViewerDialog(
    activity: FragmentActivity,
    repository: SecureStorageRepository,
    vaultRepository: VaultRepository,
    item: VaultMediaItem,
    onRequestExport: () -> Unit,
    onDismiss: () -> Unit
) {
    val settings by repository.settings.collectAsState()
    var decryptedBitmap by remember(item.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var tempVideoFile by remember(item.id) { mutableStateOf<File?>(null) }
    var authExportError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(item.id) {
        if (item.mediaType == VaultMediaType.PHOTO) {
            val bytes = vaultRepository.decryptMediaBytes(item)
            if (bytes != null) {
                decryptedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                bytes.fill(0)
            }
        } else {
            tempVideoFile = vaultRepository.createTempPlaybackFile(item)
        }
    }

    DisposableEffect(item.id) {
        onDispose { vaultRepository.wipeTempPlaybackFiles() }
    }

    val addedStr = remember(item.addedAtMillis) {
        SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()).format(Date(item.addedAtMillis))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.displayName, maxLines = 1) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.mediaType == VaultMediaType.PHOTO && decryptedBitmap != null) {
                    Image(
                        bitmap = decryptedBitmap!!.asImageBitmap(),
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else if (item.mediaType == VaultMediaType.VIDEO && tempVideoFile != null) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setMediaController(MediaController(ctx).also { it.setAnchorView(this) })
                                setVideoPath(tempVideoFile!!.absolutePath)
                                start()
                            }
                        }
                    )
                } else {
                    Text("Decrypting media securely in memory...")
                }
                Text("Added: $addedStr · ${formatBytes(item.originalSizeBytes)}", style = MaterialTheme.typography.labelMedium)
                authExportError?.let {
                    Text(it, color = CrimsonAlert, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (settings.biometricEnabled &&
                    BiometricAuthManager.checkAvailability(activity) is BiometricAvailability.Available
                ) {
                    BiometricAuthManager.authenticate(
                        activity = activity,
                        title = "Authenticate to Export",
                        subtitle = "Confirm identity before exporting decrypted file",
                        onSuccess = { onRequestExport() },
                        onFailedAttempt = { authExportError = "Biometric verification failed" },
                        onError = { authExportError = it }
                    )
                } else {
                    onRequestExport()
                }
            }) {
                Icon(Icons.Default.Download, contentDescription = null)
                Text("  Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}
