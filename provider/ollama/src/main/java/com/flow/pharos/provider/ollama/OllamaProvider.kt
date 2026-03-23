package com.flow.pharos.provider.ollama

import com.flow.pharos.core.llm.ChatRequest
import com.flow.pharos.core.llm.ChatResponse
import com.flow.pharos.core.llm.LlmGateway
import com.flow.pharos.core.model.FreeModelCatalog
import com.flow.pharos.core.model.LocalModelPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OllamaProvider(
    private val baseUrl: String,
    private val httpClient: OkHttpClient = defaultClient(),
) : LlmGateway {

    /** All model presets known to Pharos that are suitable for a 3060 12 GB. */
    val catalog: List<LocalModelPreset> = FreeModelCatalog.presets

    override suspend fun ping(): Result<String> {
        if (baseUrl.isBlank()) return Result.failure(IllegalStateException("Missing Ollama base URL"))
        return models().map { "Ollama reachable at $baseUrl — ${it.size} model(s) loaded" }
    }

    override suspend fun models(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/tags")
                .build()
            val body = httpClient.newCall(request).execute().use { it.body!!.string() }
            val arr = JSONObject(body).getJSONArray("models")
            (0 until arr.length()).map { arr.getJSONObject(it).getString("name") }
        }
    }

    override suspend fun chat(request: ChatRequest): Result<ChatResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject().apply {
                    put("model", request.model)
                    put("stream", false)
                    put("messages", JSONArray().also { arr ->
                        request.messages.forEach { msg ->
                            arr.put(JSONObject().apply {
                                put("role", msg.role)
                                put("content", msg.content)
                            })
                        }
                    })
                    put("options", JSONObject().apply {
                        put("temperature", request.temperature)
                        put("num_predict", request.maxTokens)
                    })
                }
                val req = Request.Builder()
                    .url("${baseUrl.trimEnd('/')}/api/chat")
                    .post(payload.toString().toRequestBody(JSON))
                    .build()
                val body = httpClient.newCall(req).execute().use { it.body!!.string() }
                val root = JSONObject(body)
                ChatResponse(
                    content = root.getJSONObject("message").getString("content"),
                    model = root.optString("model", request.model),
                    promptTokens = root.optInt("prompt_eval_count", 0),
                    completionTokens = root.optInt("eval_count", 0),
                )
            }
        }

    /** Build the JSON body for an Ollama model-pull request. */
    fun buildPullBody(modelName: String): String =
        JSONObject().apply { put("model", modelName) }.toString()

    companion object {
        private val JSON = "application/json".toMediaType()

        private fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // pulls can be slow
            .build()
    }
}

