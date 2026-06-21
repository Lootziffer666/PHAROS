package com.flow.pharos.core.model

import com.google.gson.annotations.SerializedName

data class ExtractedClaim(
    @SerializedName("content")
    val content: String = "",
    @SerializedName("confidence")
    val confidence: Double = 0.0,
    @SerializedName("cluster")
    val cluster: String? = null
)

data class ClaimExtractionResponse(
    @SerializedName("claims")
    val claims: List<ExtractedClaim> = emptyList()
)
