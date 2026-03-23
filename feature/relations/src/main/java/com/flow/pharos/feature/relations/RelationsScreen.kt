package com.flow.pharos.feature.relations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flow.pharos.core.model.RelationEdge

@Composable fun RelationsScreen(items:List<RelationEdge>) { LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(items) { item -> ElevatedCard { Column(Modifier.fillMaxWidth().padding(16.dp)) { Text("${item.fromId} → ${item.toId}", style = MaterialTheme.typography.titleSmall); Text(item.type); Text(item.note, style = MaterialTheme.typography.bodySmall) } } } } }
