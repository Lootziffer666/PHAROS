package com.flow.pharos.provider.perplexity

import com.flow.pharos.core.llm.AiApiProvider
import com.flow.pharos.core.llm.ChatRequest
import com.flow.pharos.core.llm.ChatResponse
import com.flow.pharos.core.llm.LlmGateway
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class PerplexityProvider(
    private val apiKey: String,
    private val httpClient: OkHttpClient = defaultClient(),
) : LlmGateway, AiApiProvider {

    override val name: String = "Perplexity"
    private val gson = Gson()

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

    override suspend fun testApiKey(apiKey: String): String {
        val body = buildAnalysisRequestBody("You are a test assistant.", "Reply with exactly: {\"status\": \"ok\"}")
        val req = Request.Builder()
            .url("https://api.perplexity.ai/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        return withContext(Dispatchers.IO) {
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string() ?: ""
                    throw IOException("API test failed (HTTP ${resp.code}): $err")
                }
                val respBody = resp.body?.string() ?: throw IOException("Empty response")
                extractContent(respBody)
            }
        }
    }

    override suspend fun analyzeDocument(
        apiKey: String, fileName: String, mimeType: String, textContent: String
    ): String {
        val systemPrompt = "You are a document analysis assistant. Analyze the provided document and return ONLY a valid JSON object (no markdown, no code fences) with this exact structure:\n{\"topics\":[\"topic1\"],\"project_suggestions\":[\"project1\"],\"summary\":\"summary\",\"action_items\":[\"action1\"],\"confidence\":0.85}\nRules: topics: 2-6 short keywords; project_suggestions: 1-3 project names; summary: 2-4 sentences; action_items: 0-5 items; confidence: 0.0-1.0. Return ONLY the JSON."
        val userPrompt = "Analyze this document:\n\nFilename: $fileName\nType: $mimeType\n\nContent:\n$textContent"
        val body = buildAnalysisRequestBody(systemPrompt, userPrompt)
        val req = Request.Builder()
            .url("https://api.perplexity.ai/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        return withContext(Dispatchers.IO) {
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string() ?: ""
                    throw IOException("API call failed (HTTP ${resp.code}): $err")
                }
                val respBody = resp.body?.string() ?: throw IOException("Empty response")
                extractContent(respBody)
            }
        }
    }

    private fun buildAnalysisRequestBody(systemPrompt: String, userPrompt: String): String {
        val body = JsonObject().apply {
            addProperty("model", "sonar")
            add("messages", gson.toJsonTree(listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userPrompt)
            )))
            addProperty("max_tokens", 1024)
            addProperty("temperature", 0.1)
        }
        return gson.toJson(body)
    }

    private fun extractContent(responseBody: String): String {
        return try {
            val json = gson.fromJson(responseBody, JsonObject::class.java)
            val choices = json.getAsJsonArray("choices")
            if (choices != null && choices.isNotEmpty()) {
                choices[0].asJsonObject.getAsJsonObject("message").get("content").asString
            } else responseBody
        } catch (e: Exception) { responseBody }
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

