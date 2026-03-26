package com.flow.pharos.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.flow.pharos.core.sync.ManifestComparator
import com.flow.pharos.core.sync.CloudSyncClient
import com.flow.pharos.core.sync.SyncConflict
import com.flow.pharos.core.sync.SyncConflictDetector
import com.flow.pharos.core.sync.SyncDiff
import com.flow.pharos.core.sync.SyncEngine
import com.flow.pharos.core.sync.SyncManifest
import com.flow.pharos.core.sync.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser

@Composable
fun SyncScreen() {
    var localPath by remember { mutableStateOf("") }
    var remotePath by remember { mutableStateOf("") }
    var manifest by remember { mutableStateOf<SyncManifest?>(null) }
    var syncDiff by remember { mutableStateOf<SyncDiff?>(null) }
    var syncConflicts by remember { mutableStateOf<List<SyncConflict>>(emptyList()) }
    var statusMessage by remember { mutableStateOf("Select folders to begin.") }
    var isWorking by remember { mutableStateOf(false) }
    var showConflicts by remember { mutableStateOf(true) }
    var watchModeEnabled by remember { mutableStateOf(false) }
    var watchIntervalSeconds by remember { mutableStateOf("30") }
    var watchJob by remember { mutableStateOf<Job?>(null) }
    var cloudManifestUrl by remember { mutableStateOf("") }
    var cloudToken by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    suspend fun compareLocalTo(path: String, remoteDeviceId: String): SyncDiff {
        return withContext(Dispatchers.IO) {
            val engine = SyncEngine(File(localPath), "windows-desktop")
            val remoteEngine = SyncEngine(File(path), remoteDeviceId)
            val localManifest = engine.generateManifest()
            val remoteManifest = remoteEngine.generateManifest()
            ManifestComparator.compare(localManifest, remoteManifest)
        }
    }

    DisposableEffect(Unit) {
        onDispose { watchJob?.cancel() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Pharos File Sync",
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
        Divider()

        // Local folder
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = localPath,
                onValueChange = { localPath = it },
                label = { Text("Local Folder") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(onClick = {
                val path = chooseFolder("Select Local Sync Folder")
                if (path != null) localPath = path
            }) { Text("Browse") }
        }

        // Remote folder
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = remotePath,
                onValueChange = { remotePath = it },
                label = { Text("Remote/Shared Folder") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(onClick = {
                val path = chooseFolder("Select Remote/Shared Folder")
                if (path != null) remotePath = path
            }) { Text("Browse") }
        }

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        isWorking = true
                        statusMessage = "Generating manifest\u2026"
                        val generated = withContext(Dispatchers.IO) {
                            val engine = SyncEngine(File(localPath), "windows-desktop")
                            val m = engine.generateManifest()
                            engine.writeManifest(m)
                            m
                        }
                        manifest = generated
                        statusMessage =
                            "Manifest generated: ${generated.entries.size} files indexed."
                        isWorking = false
                    }
                },
                enabled = localPath.isNotBlank() && !isWorking
            ) { Text("Generate Manifest") }

            Button(
                onClick = {
                    scope.launch {
                        isWorking = true
                        statusMessage = "Comparing folders\u2026"
                        val diff = compareLocalTo(remotePath, "remote")
                        val conflicts = SyncConflictDetector.detectConflicts(diff)
                        syncDiff = diff
                        syncConflicts = conflicts
                        statusMessage =
                            "Comparison: ${diff.added.size} new, ${diff.modified.size} modified, " +
                                "${diff.deleted.size} deleted, ${diff.unchanged.size} unchanged, " +
                                "${conflicts.size} conflicts."
                        isWorking = false
                    }
                },
                enabled = localPath.isNotBlank() && remotePath.isNotBlank() && !isWorking
            ) { Text("Compare") }

            Button(
                onClick = {
                    scope.launch {
                        isWorking = true
                        statusMessage = "Syncing\u2026"
                        val result = withContext(Dispatchers.IO) {
                            val engine = SyncEngine(File(localPath), "windows-desktop")
                            engine.sync(File(remotePath))
                        }
                        statusMessage = when (result) {
                            is SyncResult.Success ->
                                "Sync complete: ${result.filesTransferred} files transferred."
                            is SyncResult.NoChanges ->
                                "No changes (${result.totalFiles} files up to date)."
                            is SyncResult.Error ->
                                "Sync error: ${result.message}"
                        }
                        isWorking = false
                    }
                },
                enabled = localPath.isNotBlank() && remotePath.isNotBlank() && !isWorking
            ) { Text("Sync Now") }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = watchIntervalSeconds,
                onValueChange = { watchIntervalSeconds = it.filter(Char::isDigit) },
                label = { Text("Watch interval (s)") },
                modifier = Modifier.width(180.dp),
                singleLine = true
            )
            Button(
                onClick = {
                    val interval = watchIntervalSeconds.toLongOrNull()?.coerceAtLeast(5L) ?: 30L
                    if (!watchModeEnabled) {
                        watchModeEnabled = true
                        statusMessage = "Watch-mode started (every ${interval}s)."
                        watchJob?.cancel()
                        watchJob = scope.launch {
                            while (isActive && watchModeEnabled) {
                                runCatching {
                                    compareLocalTo(remotePath, "remote")
                                }.onSuccess { diff ->
                                    val conflicts = SyncConflictDetector.detectConflicts(diff)
                                    syncDiff = diff
                                    syncConflicts = conflicts
                                    statusMessage =
                                        "Watch compare: ${diff.added.size} new, ${diff.modified.size} modified, " +
                                            "${diff.deleted.size} deleted, ${conflicts.size} conflicts."
                                }.onFailure { error ->
                                    statusMessage = "Watch-mode compare failed: ${error.message}"
                                }
                                delay(interval * 1000)
                            }
                        }
                    } else {
                        watchModeEnabled = false
                        watchJob?.cancel()
                        watchJob = null
                        statusMessage = "Watch-mode stopped."
                    }
                },
                enabled = localPath.isNotBlank() && remotePath.isNotBlank()
            ) {
                Text(if (watchModeEnabled) "Stop Watch-Mode" else "Start Watch-Mode")
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = cloudManifestUrl,
                onValueChange = { cloudManifestUrl = it },
                label = { Text("Cloud Manifest URL") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = cloudToken,
                onValueChange = { cloudToken = it },
                label = { Text("Cloud Token (optional)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        isWorking = true
                        statusMessage = "Downloading cloud manifest…"
                        runCatching {
                            withContext(Dispatchers.IO) {
                                val cloudClient = CloudSyncClient()
                                val cloudManifest = cloudClient.downloadManifest(
                                    manifestUrl = cloudManifestUrl,
                                    bearerToken = cloudToken.ifBlank { null }
                                )
                                val localManifest = SyncEngine(File(localPath), "windows-desktop")
                                    .generateManifest()
                                ManifestComparator.compare(localManifest, cloudManifest)
                            }
                        }.onSuccess { diff ->
                            val conflicts = SyncConflictDetector.detectConflicts(diff)
                            syncDiff = diff
                            syncConflicts = conflicts
                            statusMessage = "Cloud compare: ${diff.added.size} new, " +
                                "${diff.modified.size} modified, ${diff.deleted.size} deleted, " +
                                "${conflicts.size} conflicts."
                        }.onFailure { error ->
                            statusMessage = "Cloud pull failed: ${error.message}"
                        }
                        isWorking = false
                    }
                },
                enabled = localPath.isNotBlank() && cloudManifestUrl.isNotBlank() && !isWorking
            ) { Text("Pull Cloud Manifest") }

            Button(
                onClick = {
                    scope.launch {
                        isWorking = true
                        statusMessage = "Uploading local manifest…"
                        runCatching {
                            withContext(Dispatchers.IO) {
                                val localManifest = SyncEngine(File(localPath), "windows-desktop")
                                    .generateManifest()
                                CloudSyncClient().uploadManifest(
                                    manifestUrl = cloudManifestUrl,
                                    manifest = localManifest,
                                    bearerToken = cloudToken.ifBlank { null }
                                )
                            }
                        }.onSuccess {
                            statusMessage = "Cloud manifest upload complete."
                        }.onFailure { error ->
                            statusMessage = "Cloud push failed: ${error.message}"
                        }
                        isWorking = false
                    }
                },
                enabled = localPath.isNotBlank() && cloudManifestUrl.isNotBlank() && !isWorking
            ) { Text("Push Local Manifest") }
        }

        // Status
        if (isWorking) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
            Text(statusMessage, modifier = Modifier.padding(12.dp))
        }

        Divider()

        // Manifest / Diff display
        val currentDiff = syncDiff
        val currentManifest = manifest
        if (currentDiff != null) {
            Text("Sync Diff", style = MaterialTheme.typography.h6)
            if (syncConflicts.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.08f),
                    elevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Conflicts detected (${syncConflicts.size})",
                                color = MaterialTheme.colors.error,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { showConflicts = !showConflicts }) {
                                Text(if (showConflicts) "Hide" else "Show")
                            }
                        }
                        if (showConflicts) {
                            syncConflicts.forEach { conflict ->
                                val winnerHint = when {
                                    conflict.remoteIsNewer -> "Remote version is newer"
                                    conflict.localIsNewer -> "Local version is newer"
                                    else -> "Both versions have same timestamp"
                                }
                                Text(
                                    "• ${conflict.relativePath} — ${conflict.conflictRecord.summary}. $winnerHint"
                                )
                            }
                        }
                    }
                }
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (currentDiff.added.isNotEmpty()) {
                    item {
                        Text(
                            "+ New (${currentDiff.added.size})",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                    }
                    items(currentDiff.added) { entry ->
                        Text("  + ${entry.relativePath} (${formatSize(entry.size)})")
                    }
                }
                if (currentDiff.modified.isNotEmpty()) {
                    item {
                        Text(
                            "~ Modified (${currentDiff.modified.size})",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.secondary
                        )
                    }
                    items(currentDiff.modified) { (_, remote) ->
                        Text("  ~ ${remote.relativePath} (${formatSize(remote.size)})")
                    }
                }
                if (currentDiff.deleted.isNotEmpty()) {
                    item {
                        Text(
                            "- Deleted (${currentDiff.deleted.size})",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.error
                        )
                    }
                    items(currentDiff.deleted) { entry ->
                        Text("  - ${entry.relativePath}")
                    }
                }
                if (currentDiff.unchanged.isNotEmpty()) {
                    item {
                        Text(
                            "= Unchanged (${currentDiff.unchanged.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else if (currentManifest != null) {
            Text(
                "Local Manifest (${currentManifest.entries.size} files)",
                style = MaterialTheme.typography.h6
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(currentManifest.entries) { entry ->
                    Text(
                        "${entry.relativePath} \u2013 ${formatSize(entry.size)} \u2013 " +
                            "${entry.sha256.take(8)}\u2026"
                    )
                }
            }
        }
    }
}

private fun chooseFolder(title: String): String? {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = title
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.absolutePath
    } else {
        null
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}
