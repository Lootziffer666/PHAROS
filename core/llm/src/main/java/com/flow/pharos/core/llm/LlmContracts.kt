package com.flow.pharos.core.llm

data class ChatMessage(
    val role: String,
    val content: String,
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    val maxTokens: Int = 1024,
)

data class ChatResponse(
    val content: String,
    val model: String,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
)

interface LlmGateway {
    /** Lightweight connectivity check — returns a human-readable status string. */
    suspend fun ping(): Result<String>

    /** List available model IDs on this backend. */
    suspend fun models(): Result<List<String>>

    /** Send a chat-completion request and return the assistant message. */
    suspend fun chat(request: ChatRequest): Result<ChatResponse>
}

