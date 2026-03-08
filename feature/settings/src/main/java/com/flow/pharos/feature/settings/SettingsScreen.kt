package com.flow.pharos.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flow.pharos.core.model.*

@Composable fun SettingsScreen(state:PharosUiState) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("LLM Providers", style = MaterialTheme.typography.titleMedium); state.llmProviders.forEach { Text("${it.title}: ${if (it.enabled) "on" else "off"} · ${it.note}") }; Text("Free local models (3060 12GB aware)", style = MaterialTheme.typography.titleMedium); FreeModelCatalog.presets.forEach { Text("${it.displayName} — ${it.sizeHint}") }; Text("Budget: ${state.spendStatus.spentTodayUsd}/${state.budgetPolicy.dailyLimitUsd} USD today") } }
