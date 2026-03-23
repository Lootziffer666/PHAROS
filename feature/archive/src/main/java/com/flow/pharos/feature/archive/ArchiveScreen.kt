package com.flow.pharos.feature.archive

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flow.pharos.core.model.ArtifactRecord

@Composable fun ArchiveScreen(items:List<ArtifactRecord>) { LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(items) { item -> ElevatedCard { Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(item.title, style = MaterialTheme.typography.titleMedium); Text(item.summary); Text("${item.status} · ${item.timestamp}", style = MaterialTheme.typography.bodySmall) } } } } }
