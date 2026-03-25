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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flow.pharos.core.sync.ManifestComparator
import com.flow.pharos.core.sync.SyncDiff
import com.flow.pharos.core.sync.SyncEngine
import com.flow.pharos.core.sync.SyncManifest
import com.flow.pharos.core.sync.SyncResult
import kotlinx.coroutines.Dispatchers
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
    var statusMessage by remember { mutableStateOf("Select folders to begin.") }
    var isWorking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                        withContext(Dispatchers.IO) {
                            val engine = SyncEngine(File(localPath), "windows-desktop")
                            manifest = engine.generateManifest()
                            engine.writeManifest(manifest!!)
                        }
                        statusMessage =
                            "Manifest generated: ${manifest!!.entries.size} files indexed."
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
                        withContext(Dispatchers.IO) {
                            val engine = SyncEngine(File(localPath), "windows-desktop")
                            val remoteEngine = SyncEngine(File(remotePath), "remote")
                            val localManifest = engine.generateManifest()
                            val remoteManifest = remoteEngine.generateManifest()
                            syncDiff =
                                ManifestComparator.compare(localManifest, remoteManifest)
                        }
                        val d = syncDiff!!
                        statusMessage =
                            "Comparison: ${d.added.size} new, ${d.modified.size} modified, " +
                                "${d.deleted.size} deleted, ${d.unchanged.size} unchanged."
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

        // Status
        if (isWorking) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
            Text(statusMessage, modifier = Modifier.padding(12.dp))
        }

        Divider()

        // Manifest / Diff display
        if (syncDiff != null) {
            val d = syncDiff!!
            Text("Sync Diff", style = MaterialTheme.typography.h6)
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (d.added.isNotEmpty()) {
                    item {
                        Text(
                            "+ New (${d.added.size})",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                    }
                    items(d.added) { entry ->
                        Text("  + ${entry.relativePath} (${formatSize(entry.size)})")
                    }
                }
                if (d.modified.isNotEmpty()) {
                    item {
                        Text(
                            "~ Modified (${d.modified.size})",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.secondary
                        )
                    }
                    items(d.modified) { (_, remote) ->
                        Text("  ~ ${remote.relativePath} (${formatSize(remote.size)})")
                    }
                }
                if (d.deleted.isNotEmpty()) {
                    item {
                        Text(
                            "- Deleted (${d.deleted.size})",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.error
                        )
                    }
                    items(d.deleted) { entry ->
                        Text("  - ${entry.relativePath}")
                    }
                }
                if (d.unchanged.isNotEmpty()) {
                    item {
                        Text("= Unchanged (${d.unchanged.size})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (manifest != null) {
            Text(
                "Local Manifest (${manifest!!.entries.size} files)",
                style = MaterialTheme.typography.h6
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(manifest!!.entries) { entry ->
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
