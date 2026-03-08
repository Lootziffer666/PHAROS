package com.flow.pharos.provider.perplexity

import com.flow.pharos.core.llm.LlmGateway

class PerplexityProvider(private val apiKey:String): LlmGateway { override suspend fun ping() = if (apiKey.isBlank()) Result.failure(IllegalStateException("Missing Perplexity API key")) else Result.success("Perplexity configured") }
