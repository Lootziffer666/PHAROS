package com.flow.pharos.core.llm

interface AiApiProvider {
    val name: String
    suspend fun testApiKey(apiKey: String): String
    suspend fun analyzeDocument(
        apiKey: String,
        fileName: String,
        mimeType: String,
        textContent: String
    ): String
}
