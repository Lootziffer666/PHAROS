package com.flow.pharos.provider.customopenai

import com.flow.pharos.core.llm.LlmGateway

class CustomOpenAiProvider(private val baseUrl:String, private val apiKey:String): LlmGateway { override suspend fun ping() = if (baseUrl.isBlank()) Result.failure(IllegalStateException("Missing custom gateway URL")) else Result.success("Custom gateway configured") }
