package com.flow.pharos.usecase

import com.flow.pharos.core.llm.ChatMessage
import com.flow.pharos.core.llm.ChatRequest
import com.flow.pharos.core.llm.JsonParser
import com.flow.pharos.core.llm.LlmGateway
import com.flow.pharos.core.model.ClaimExtractionResponse
import com.flow.pharos.core.model.ClaimStatus
import com.flow.pharos.core.model.entity.ClaimEntity
import com.flow.pharos.core.model.entity.FileEntity
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.util.UUID

class ClaimExtractionUseCase(
    private val llmGateway: LlmGateway
) {
    private val gson = Gson()

    suspend fun extractClaims(file: FileEntity, textContent: String, model: String): Result<List<ClaimEntity>> {
        val systemPrompt = """You are a precise fact-extraction assistant. Your task is to extract discrete, atomic factual assertions from the provided document text.

Rules:
- Each claim must be a single, self-contained factual statement.
- Do not combine multiple facts into one claim.
- Each claim should be verifiable independently.
- Assign a confidence score (0.0 to 1.0) based on how clearly the fact is stated.
- Optionally assign a cluster/topic label if the claim belongs to a clear category.
- Return ONLY valid JSON, no explanations or markdown.

Output format:
{"claims": [{"content": "...", "confidence": 0.95, "cluster": "topic"}, ...]}"""

        val userPrompt = """Extract all atomic factual claims from the following document.

Document name: ${file.name}

Document content:
$textContent"""

        val request = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            ),
            temperature = 0.2,
            maxTokens = 4096
        )

        val chatResult = llmGateway.chat(request)
        return chatResult.mapCatching { response ->
            val parsed = parseClaimExtractionResponse(response.content)
                ?: throw IllegalStateException("Failed to parse claim extraction response")

            val now = System.currentTimeMillis()
            parsed.claims.map { extracted ->
                ClaimEntity(
                    id = UUID.randomUUID().toString(),
                    content = extracted.content,
                    sourceFileId = file.id,
                    sourceFileName = file.name,
                    sourceTimestamp = file.lastModified,
                    extractedAt = now,
                    status = ClaimStatus.PENDING,
                    confidence = extracted.confidence,
                    clusterId = extracted.cluster
                )
            }
        }
    }

    fun parseClaimExtractionResponse(raw: String): ClaimExtractionResponse? {
        val cleaned = JsonParser.cleanJsonResponse(raw)
        return try {
            gson.fromJson(cleaned, ClaimExtractionResponse::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }
}
