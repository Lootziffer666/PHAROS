package com.flow.pharos.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flow.pharos.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onNavigateToFolders: () -> Unit = {}, onNavigateToProjects: () -> Unit = {}) {
    val totalFiles by viewModel.totalFileCount.collectAsState()
    val changedFiles by viewModel.changedFileCount.collectAsState()
    val analyzedFiles by viewModel.analyzedFileCount.collectAsState()
    val hasApiKey by viewModel.hasApiKey.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isUpdating by viewModel.isUpdatingMasterfiles.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val analysisProgress by viewModel.analysisProgress.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)

        // Stats cards
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("Indexed", "$totalFiles", Modifier.weight(1f))
            StatCard("New/Changed", "$changedFiles", Modifier.weight(1f))
            StatCard("Analyzed", "$analyzedFiles", Modifier.weight(1f))
        }

        // Scan progress
        if (isScanning && scanProgress != null) {
            val p = scanProgress!!
            Column {
                Text("Scanning: ${p.currentFileName}", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { p.current.toFloat() / p.total.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
                Text("${p.current}/${p.total} files", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Analysis progress
        if (isAnalyzing && analysisProgress != null) {
            val p = analysisProgress!!
            Column {
                Text("Analyzing: ${p.currentFileName}", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { p.current.toFloat() / p.total.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
                Text("${p.succeeded} OK, ${p.failed} failed", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Status message
        statusMessage?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Action buttons
        Button(onClick = { viewModel.scanFolders() }, enabled = !isScanning && !isAnalyzing, modifier = Modifier.fillMaxWidth()) {
            Text(if (isScanning) "Scanning..." else "Scan Folder")
        }
        if (isScanning) {
            OutlinedButton(onClick = { viewModel.cancelScan() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel Scan") }
        }

        Button(onClick = { viewModel.startAnalysis() }, enabled = !isScanning && !isAnalyzing && hasApiKey && changedFiles > 0, modifier = Modifier.fillMaxWidth()) {
            Text(if (isAnalyzing) "Analyzing..." else "Start Analysis ($changedFiles files)")
        }
        if (isAnalyzing) {
            OutlinedButton(onClick = { viewModel.cancelAnalysis() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel Analysis") }
        }

        Button(onClick = { viewModel.updateMasterfiles() }, enabled = !isUpdating && !isScanning && !isAnalyzing, modifier = Modifier.fillMaxWidth()) {
            Text(if (isUpdating) "Updating..." else "Update Masterfiles")
        }

        if (!hasApiKey) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text("No API key configured. Go to Settings.", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        // Quick access to Folders and Projects
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onNavigateToFolders, modifier = Modifier.weight(1f)) {
                Text("Folders")
            }
            OutlinedButton(onClick = onNavigateToProjects, modifier = Modifier.weight(1f)) {
                Text("Projects")
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
