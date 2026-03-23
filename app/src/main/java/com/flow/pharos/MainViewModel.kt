package com.flow.pharos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flow.pharos.core.model.BudgetPolicy
import com.flow.pharos.core.model.LlmProviderState
import com.flow.pharos.core.model.LlmProviderKind
import com.flow.pharos.core.model.ModelDownloadState
import com.flow.pharos.core.model.PharosUiState
import com.flow.pharos.core.model.SpendStatus
import com.flow.pharos.core.model.UiResult
import com.flow.pharos.core.storage.BudgetRepository
import com.flow.pharos.core.storage.SeedRepository
import com.flow.pharos.provider.customopenai.CustomOpenAiProvider
import com.flow.pharos.provider.ollama.OllamaProvider
import com.flow.pharos.provider.perplexity.PerplexityProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val seedRepository: SeedRepository,
    private val budgetRepository: BudgetRepository,
    private val perplexityProvider: PerplexityProvider,
    private val ollamaProvider: OllamaProvider,
    private val customOpenAiProvider: CustomOpenAiProvider,
) : ViewModel() {

    private val _providerStates = MutableStateFlow(
        listOf(
            LlmProviderState(
                kind = LlmProviderKind.PERPLEXITY,
                title = "Perplexity",
                enabled = BuildConfig.PERPLEXITY_API_KEY.isNotBlank(),
                configured = BuildConfig.PERPLEXITY_API_KEY.isNotBlank(),
                note = "web-grounded cloud",
            ),
            LlmProviderState(
                kind = LlmProviderKind.OLLAMA,
                title = "Ollama / local",
                enabled = true,
                configured = BuildConfig.OLLAMA_BASE_URL.isNotBlank(),
                note = "free local models + pull menu",
            ),
            LlmProviderState(
                kind = LlmProviderKind.CUSTOM_OPENAI,
                title = "Custom gateway",
                enabled = true,
                configured = BuildConfig.CUSTOM_OPENAI_BASE_URL.isNotBlank(),
                note = "AnythingLLM-like no-code pipeline backend",
            ),
        )
    )

    private val _statusText = MutableStateFlow("Ready")
    private val _modelDownload = MutableStateFlow(ModelDownloadState())

    /** Single source of truth for the UI — emits Loading once, then Success on every update. */
    val uiState: StateFlow<UiResult<PharosUiState>> =
        combine(_providerStates, _statusText, _modelDownload, budgetRepository.budgetPolicy) {
                providers, status, download, budget ->
            UiResult.Success(
                PharosUiState(
                    archive = seedRepository.archive(),
                    llmProviders = providers,
                    budgetPolicy = budget,
                    spendStatus = SpendStatus(remainingTodayUsd = budget.dailyLimitUsd),
                    statusText = status,
                    modelDownload = download,
                )
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiResult.Loading,
        )

    /** Pings all configured providers and updates their reachability flag. */
    fun pingProviders() {
        viewModelScope.launch {
            _statusText.value = "Checking providers…"
            val results = mapOf(
                LlmProviderKind.PERPLEXITY to perplexityProvider.ping(),
                LlmProviderKind.OLLAMA to ollamaProvider.ping(),
                LlmProviderKind.CUSTOM_OPENAI to customOpenAiProvider.ping(),
            )
            _providerStates.update { current ->
                current.map { state ->
                    state.copy(configured = results[state.kind]?.isSuccess == true)
                }
            }
            val allOk = results.values.all { it.isSuccess }
            _statusText.value = if (allOk) "All providers reachable" else "Some providers unreachable"
        }
    }

    /** Refreshes the list of locally loaded Ollama models. */
    fun refreshLocalModels() {
        viewModelScope.launch {
            _modelDownload.update { it.copy(lastPullMessage = "Refreshing model list…") }
            ollamaProvider.models()
                .onSuccess { models ->
                    _modelDownload.update {
                        it.copy(
                            downloadedModelNames = models,
                            lastPullMessage = "${models.size} model(s) loaded.",
                            localRuntimeReachable = true,
                        )
                    }
                }
                .onFailure { err ->
                    _modelDownload.update {
                        it.copy(
                            lastPullMessage = "Refresh failed: ${err.message}",
                            localRuntimeReachable = false,
                        )
                    }
                }
        }
    }

    /** Persists a new budget policy via DataStore. */
    fun saveBudgetPolicy(policy: BudgetPolicy) {
        viewModelScope.launch { budgetRepository.update(policy) }
    }
}
