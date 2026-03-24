package com.flow.pharos.core.llm

import com.flow.pharos.core.model.AnalysisResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

object JsonParser {
    private val gson = Gson()

    fun parseAnalysisResponse(raw: String): AnalysisResponse? {
        val cleaned = cleanJsonResponse(raw)
        return try {
            gson.fromJson(cleaned, AnalysisResponse::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    fun cleanJsonResponse(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```")) {
            val firstNewline = cleaned.indexOf('\n')
            if (firstNewline > 0) {
                cleaned = cleaned.substring(firstNewline + 1)
            }
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length - 3)
        }
        return cleaned.trim()
    }

    fun toJsonArray(list: List<String>): String = gson.toJson(list)

    fun fromJsonArray(json: String): List<String> {
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
