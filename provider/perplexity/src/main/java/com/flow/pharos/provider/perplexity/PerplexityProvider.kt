package com.flow.pharos.provider.perplexity

import com.flow.pharos.core.llm.ChatRequest
import com.flow.pharos.core.llm.ChatResponse
import com.flow.pharos.core.llm.LlmGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PerplexityProvider(
    private val apiKey: String,
    private val httpClient: OkHttpClient = defaultClient(),
) : LlmGateway {

    override suspend fun ping(): Result<String> {
        if (apiKey.isBlank()) return Result.failure(IllegalStateException("Missing Perplexity API key"))
        return models().map { "Perplexity reachable — ${it.size} model(s) available" }
    }

    override suspend fun models(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.perplexity.ai/models")
                .header("Authorization", "Bearer $apiKey")
                .build()
            val body = httpClient.newCall(request).execute().use { it.body!!.string() }
            val arr = JSONObject(body).getJSONArray("data")
            (0 until arr.length()).map { arr.getJSONObject(it).getString("id") }
        }
    }

    override suspend fun chat(request: ChatRequest): Result<ChatResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = buildPayload(request)
                val req = Request.Builder()
                    .url("https://api.perplexity.ai/chat/completions")
                    .header("Authorization", "Bearer $apiKey")
                    .post(payload.toString().toRequestBody(JSON))
                    .build()
                val body = httpClient.newCall(req).execute().use { it.body!!.string() }
                parseResponse(body, request.model)
            }
        }

    companion object {
        private val JSON = "application/json".toMediaType()

        private fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        private fun buildPayload(request: ChatRequest) = JSONObject().apply {
            put("model", request.model)
            put("temperature", request.temperature)
            put("max_tokens", request.maxTokens)
            put("messages", JSONArray().also { arr ->
                request.messages.forEach { msg ->
                    arr.put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            })
        }

        private fun parseResponse(json: String, fallbackModel: String): ChatResponse {
            val root = JSONObject(json)
            val content = root.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            val usage = root.optJSONObject("usage")
            return ChatResponse(
                content = content,
                model = root.optString("model", fallbackModel),
                promptTokens = usage?.optInt("prompt_tokens") ?: 0,
                completionTokens = usage?.optInt("completion_tokens") ?: 0,
            )
        }
    }
}

