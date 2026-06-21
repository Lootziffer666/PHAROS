package com.flow.pharos.usecase

import com.flow.pharos.core.llm.ChatMessage
import com.flow.pharos.core.llm.ChatRequest
import com.flow.pharos.core.llm.JsonParser
import com.flow.pharos.core.llm.LlmGateway
import com.flow.pharos.core.model.ClaimStatus
import com.flow.pharos.core.model.entity.ClaimEntity
import com.flow.pharos.core.storage.repository.ClaimRepository
import com.google.gson.Gson

data class ConflictDetectionResult(
    val autoMerged: Int,
    val conflicts: Int,
    val errors: Int
)

class ConflictDetectionUseCase(
    private val claimRepository: ClaimRepository,
    private val llmGateway: LlmGateway
) {
    private val gson = Gson()

    // NOTE: Known limitation (issue #2) - If the pipeline runs concurrently for two files,
    // one pass may not see the other's newly-approved claims during conflict checking, since
    // approvedClaims is fetched once at the start. A future enhancement could serialize
    // pipeline runs or re-read approved claims per iteration.
    suspend fun detectConflicts(pendingClaims: List<ClaimEntity>, model: String): ConflictDetectionResult {
        var autoMerged = 0
        var conflicts = 0
        var errors = 0

        val approvedClaims = claimRepository.getAllClaimsList()
            .filter { it.status == ClaimStatus.APPROVED }

        // NOTE: Known limitation (issue #6) - Conflict detection only compares within the same
        // clusterId. Two claims that genuinely contradict but receive different cluster labels
        // from the LLM will not be flagged. The cluster label is a best-effort optimization that
        // trades recall for fewer LLM calls.
        val approvedByCluster = approvedClaims.groupBy { it.clusterId ?: "__unclustered__" }

        for (claim in pendingClaims) {
            val clusterKey = claim.clusterId ?: "__unclustered__"
            val clusterClaims = approvedByCluster[clusterKey].orEmpty()

            if (clusterClaims.isEmpty()) {
                claimRepository.updateClaimStatus(claim.id, ClaimStatus.APPROVED)
                autoMerged++
                continue
            }

            try {
                val conflictRationale = checkForContradictions(claim, clusterClaims, model)
                if (conflictRationale != null) {
                    claimRepository.updateClaim(
                        claim.copy(
                            status = ClaimStatus.CONFLICT,
                            aiRationale = conflictRationale
                        )
                    )
                    conflicts++
                } else {
                    claimRepository.updateClaimStatus(claim.id, ClaimStatus.APPROVED)
                    autoMerged++
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                errors++
            }
        }

        return ConflictDetectionResult(autoMerged, conflicts, errors)
    }

    private suspend fun checkForContradictions(
        newClaim: ClaimEntity,
        existingClaims: List<ClaimEntity>,
        model: String
    ): String? {
        val existingClaimsText = existingClaims.joinToString("\n") { "- ${it.content}" }

        val systemPrompt = """You are a contradiction detection assistant. You determine if a new claim contradicts any existing approved claims.

Rules:
- A contradiction means the new claim and an existing claim cannot both be true simultaneously.
- Minor differences in wording or scope are NOT contradictions.
- Only flag direct logical contradictions.
- Respond with ONLY a JSON object: {"contradicts": true/false, "rationale": "explanation"}
- If contradicts is true, explain which existing claim it contradicts and why."""

        val userPrompt = """New claim: "${newClaim.content}"

Existing approved claims:
$existingClaimsText

Does the new claim contradict any existing claim?"""

        val request = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            ),
            temperature = 0.1,
            maxTokens = 512
        )

        val result = llmGateway.chat(request)
        return result.getOrNull()?.let { response ->
            parseContradictionResponse(response.content)
        }
    }

    private fun parseContradictionResponse(raw: String): String? {
        val cleaned = JsonParser.cleanJsonResponse(raw)
        return try {
            val map = gson.fromJson(cleaned, Map::class.java)
            val contradicts = map["contradicts"]
            val isContradiction = when (contradicts) {
                is Boolean -> contradicts
                is String -> contradicts.equals("true", ignoreCase = true)
                else -> false
            }
            if (isContradiction) {
                (map["rationale"] as? String) ?: "Contradiction detected"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
