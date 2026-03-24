package com.flow.pharos.provider.customopenai

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

class CustomOpenAiProvider(
    private val baseUrl: String,
    private val apiKey: String,
    private val httpClient: OkHttpClient = defaultClient(),
) : LlmGateway {

    override suspend fun ping(): Result<String> {
        if (baseUrl.isBlank()) return Result.failure(IllegalStateException("Missing custom gateway URL"))
        return models().map { "Custom gateway reachable at $baseUrl — ${it.size} model(s)" }
    }

    override suspend fun models(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/models")
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
                val payload = JSONObject().apply {
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
                val req = Request.Builder()
                    .url("${baseUrl.trimEnd('/')}/chat/completions")
                    .header("Authorization", "Bearer $apiKey")
                    .post(payload.toString().toRequestBody(JSON))
                    .build()
                val body = httpClient.newCall(req).execute().use { it.body!!.string() }
                val root = JSONObject(body)
                val choices = root.getJSONArray("choices")
                if (choices.length() == 0) throw IllegalStateException("No choices in API response")
                val choice = choices.getJSONObject(0)
                val usage = root.optJSONObject("usage")
                ChatResponse(
                    content = choice.getJSONObject("message").getString("content"),
                    model = root.optString("model", request.model),
                    promptTokens = usage?.optInt("prompt_tokens") ?: 0,
                    completionTokens = usage?.optInt("completion_tokens") ?: 0,
                )
            }
        }

    companion object {
        private val JSON = "application/json".toMediaType()

        private fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}

