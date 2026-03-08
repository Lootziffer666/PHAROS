package com.flow.pharos.provider.ollama

import com.flow.pharos.core.llm.LlmGateway
import com.flow.pharos.core.model.FreeModelCatalog

class OllamaProvider(private val baseUrl:String): LlmGateway { override suspend fun ping() = if (baseUrl.isBlank()) Result.failure(IllegalStateException("Missing Ollama base URL")) else Result.success("Ollama configured at $baseUrl"); fun catalog() = FreeModelCatalog.presets; fun pullRequestBody(model:String) = "{\"model\":\"$model\"}" }
