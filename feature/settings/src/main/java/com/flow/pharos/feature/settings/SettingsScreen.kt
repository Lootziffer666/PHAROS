package com.flow.pharos.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.flow.pharos.core.model.BudgetPolicy
import com.flow.pharos.core.model.FreeModelCatalog
import com.flow.pharos.core.model.LlmProviderState
import com.flow.pharos.core.model.PharosUiState

@Composable
fun SettingsScreen(
    state: PharosUiState,
    onSaveBudget: (BudgetPolicy) -> Unit = {},
    onPingProviders: () -> Unit = {},
    onRefreshLocalModels: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // LLM Providers
        Text("LLM Providers", style = MaterialTheme.typography.titleMedium)
        state.llmProviders.forEach { provider ->
            ProviderRow(provider)
        }
        Button(onClick = onPingProviders, modifier = Modifier.fillMaxWidth()) {
            Text("Check Provider Connectivity")
        }

        // Local models
        Text("Free local models (3060 12 GB aware)", style = MaterialTheme.typography.titleMedium)
        FreeModelCatalog.presets.forEach { preset ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(preset.displayName, modifier = Modifier.weight(1f))
                Text(
                    text = preset.sizeHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(onClick = onRefreshLocalModels, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh loaded models")
        }
        if (state.modelDownload.lastPullMessage.isNotBlank()) {
            Text(
                text = state.modelDownload.lastPullMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Budget
        Text("Budget Policy", style = MaterialTheme.typography.titleMedium)

        var dailyLimitInput by remember(state.budgetPolicy.dailyLimitUsd) {
            mutableStateOf(state.budgetPolicy.dailyLimitUsd.toString())
        }
        Text(
            text = "Spent today: \$${state.spendStatus.spentTodayUsd} / \$${state.budgetPolicy.dailyLimitUsd}",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = dailyLimitInput,
            onValueChange = { dailyLimitInput = it },
            label = { Text("Daily limit (USD)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(
            onClick = {
                val limit = dailyLimitInput.toFloatOrNull() ?: state.budgetPolicy.dailyLimitUsd
                onSaveBudget(state.budgetPolicy.copy(dailyLimitUsd = limit))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save Budget")
        }

        // Status
        if (state.statusText.isNotBlank()) {
            Text(
                text = state.statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProviderRow(provider: LlmProviderState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(provider.title, style = MaterialTheme.typography.bodyMedium)
            Text(provider.note, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = if (provider.configured) "✓" else "✗",
            color = if (provider.configured) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
    }
}

