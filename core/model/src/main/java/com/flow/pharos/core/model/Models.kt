package com.flow.pharos.core.model

data class ArtifactRecord(val id:String,val title:String,val status:String,val timestamp:String,val path:String,val tags:List<String>,val summary:String)
data class RelationEdge(val fromId:String,val toId:String,val type:String,val note:String)
data class PharosIndex(val artifacts:List<ArtifactRecord>,val relations:List<RelationEdge>)
enum class LlmProviderKind { PERPLEXITY, OLLAMA, CUSTOM_OPENAI }
data class LlmProviderState(val kind:LlmProviderKind,val title:String,val enabled:Boolean=false,val configured:Boolean=false,val note:String="")
data class LocalModelPreset(val id:String,val displayName:String,val remoteName:String,val sizeHint:String,val codingSuitable:Boolean=false,val recommendedFor3060_12gb:Boolean=false)
data class BudgetPolicy(val dailyLimitUsd:Float=5f,val usePaidUntilDailyLimit:Boolean=true,val cheapFirst:Boolean=true)
data class SpendStatus(val spentTodayUsd:Float=0f,val remainingTodayUsd:Float=5f)
data class ModelDownloadState(val selectedPresetId:String="gemma3_4b",val downloadedModelNames:List<String> = emptyList(),val lastPullMessage:String="No local model pulled yet.",val localRuntimeReachable:Boolean=false)
data class PharosUiState(val archive:PharosIndex,val llmProviders:List<LlmProviderState>,val budgetPolicy:BudgetPolicy=BudgetPolicy(),val spendStatus:SpendStatus=SpendStatus(),val statusText:String="Ready",val modelDownload:ModelDownloadState=ModelDownloadState())
object FreeModelCatalog { val presets = listOf(LocalModelPreset("gemma3_4b","Gemma 3 4B","gemma3","safe on 3060 12GB", recommendedFor3060_12gb=true),LocalModelPreset("qwen3_8b","Qwen 3 8B","qwen3:8b","good fit on 3060 12GB when quantized", recommendedFor3060_12gb=true),LocalModelPreset("qwen3_coder","Qwen 3 Coder","qwen3-coder","coding-focused; watch memory", codingSuitable=true, recommendedFor3060_12gb=true),LocalModelPreset("gpt_oss_20b","gpt-oss 20B","gpt-oss:20b","optional, likely heavy for 12GB", codingSuitable=true)) }
