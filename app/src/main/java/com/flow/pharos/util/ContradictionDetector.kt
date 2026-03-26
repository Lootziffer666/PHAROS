package com.flow.pharos.util

import com.flow.pharos.core.llm.JsonParser
import com.flow.pharos.core.model.entity.AnalysisEntity

data class ContradictionInsight(
    val leftFileName: String,
    val rightFileName: String,
    val sharedTopic: String,
    val reason: String
)

object ContradictionDetector {

    private val negationMarkers = setOf(
        "not", "no", "never", "without", "cannot", "can't", "failed", "missing", "denied", "reject"
    )

    fun detectPotentialContradictions(
        analysesByFileName: Map<String, AnalysisEntity>
    ): List<ContradictionInsight> {
        val entries = analysesByFileName.entries.toList()
        if (entries.size < 2) return emptyList()
        val prepared = entries.map { (fileName, analysis) ->
            PreparedAnalysis(
                fileName = fileName,
                summary = analysis.summary.lowercase(),
                topics = JsonParser.fromJsonArray(analysis.topics)
                    .map { it.trim().lowercase() }
                    .filter { it.isNotBlank() }
                    .toSet()
            )
        }

        val results = mutableListOf<ContradictionInsight>()
        for (i in prepared.indices) {
            for (j in i + 1 until prepared.size) {
                val left = prepared[i]
                val right = prepared[j]
                val overlapTopics = left.topics.intersect(right.topics)
                    .filter { it.length >= 3 }
                    .sorted()
                if (overlapTopics.isEmpty()) continue

                overlapTopics.forEach { topic ->
                    val leftNegatesTopic = negatesTopic(left.summary, topic)
                    val rightNegatesTopic = negatesTopic(right.summary, topic)
                    if (leftNegatesTopic == rightNegatesTopic) return@forEach

                    val reason = if (leftNegatesTopic) {
                        "Left summary negates topic '$topic' while right summary affirms it."
                    } else {
                        "Right summary negates topic '$topic' while left summary affirms it."
                    }
                    results += ContradictionInsight(
                        leftFileName = left.fileName,
                        rightFileName = right.fileName,
                        sharedTopic = topic,
                        reason = reason
                    )
                    return@forEach
                }
            }
        }
        return results.distinctBy { "${it.leftFileName}|${it.rightFileName}|${it.sharedTopic}" }
    }

    private fun negatesTopic(summary: String, topic: String): Boolean {
        val windows = summary.split(Regex("[.!?]"))
            .filter { it.contains(topic) }
        return windows.any { sentence ->
            negationMarkers.any { marker ->
                "\\b${Regex.escape(marker)}\\b".toRegex().containsMatchIn(sentence)
            }
        }
    }

    private data class PreparedAnalysis(
        val fileName: String,
        val summary: String,
        val topics: Set<String>
    )
}
