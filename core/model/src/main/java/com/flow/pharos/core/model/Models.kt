package com.flow.pharos.core.model

data class ArtifactRecord(
    val id: String,
    val title: String,
    val status: String,
    val timestamp: String,
    val path: String,
    val tags: List<String>,
    val summary: String,
)

data class RelationEdge(
    val fromId: String,
    val toId: String,
    val type: String,
    val note: String,
)

data class PharosIndex(
    val artifacts: List<ArtifactRecord>,
    val relations: List<RelationEdge>,
)

enum class LlmProviderKind { PERPLEXITY, OLLAMA, CUSTOM_OPENAI }

data class LlmProviderState(
    val kind: LlmProviderKind,
    val title: String,
    val enabled: Boolean = false,
    val configured: Boolean = false,
    val note: String = "",
)

data class LocalModelPreset(
    val id: String,
    val displayName: String,
    val remoteName: String,
    val sizeHint: String,
    val codingSuitable: Boolean = false,
    val recommendedFor3060_12gb: Boolean = false,
)

data class BudgetPolicy(
    val dailyLimitUsd: Float = 5f,
    val usePaidUntilDailyLimit: Boolean = true,
    val cheapFirst: Boolean = true,
)

data class SpendStatus(
    val spentTodayUsd: Float = 0f,
    val remainingTodayUsd: Float = 5f,
)

data class ModelDownloadState(
    val selectedPresetId: String = "gemma3_4b",
    val downloadedModelNames: List<String> = emptyList(),
    val lastPullMessage: String = "No local model pulled yet.",
    val localRuntimeReachable: Boolean = false,
)

data class PharosUiState(
    val archive: PharosIndex,
    val llmProviders: List<LlmProviderState>,
    val budgetPolicy: BudgetPolicy = BudgetPolicy(),
    val spendStatus: SpendStatus = SpendStatus(),
    val statusText: String = "Ready",
    val modelDownload: ModelDownloadState = ModelDownloadState(),
)

/** Unified UI result wrapping loading / success / error states. */
sealed class UiResult<out T> {
    data object Loading : UiResult<Nothing>()
    data class Success<T>(val data: T) : UiResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : UiResult<Nothing>()
}

object FreeModelCatalog {
    val presets = listOf(
        LocalModelPreset(
            id = "gemma3_4b",
            displayName = "Gemma 3 4B",
            remoteName = "gemma3",
            sizeHint = "safe on 3060 12GB",
            recommendedFor3060_12gb = true,
        ),
        LocalModelPreset(
            id = "qwen3_8b",
            displayName = "Qwen 3 8B",
            remoteName = "qwen3:8b",
            sizeHint = "good fit on 3060 12GB when quantized",
            recommendedFor3060_12gb = true,
        ),
        LocalModelPreset(
            id = "qwen3_coder",
            displayName = "Qwen 3 Coder",
            remoteName = "qwen3-coder",
            sizeHint = "coding-focused; watch memory",
            codingSuitable = true,
            recommendedFor3060_12gb = true,
        ),
        LocalModelPreset(
            id = "gpt_oss_20b",
            displayName = "gpt-oss 20B",
            remoteName = "gpt-oss:20b",
            sizeHint = "optional, likely heavy for 12GB",
            codingSuitable = true,
        ),
    )
}

