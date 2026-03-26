package com.flow.pharos

import com.flow.pharos.core.model.entity.AnalysisEntity
import com.flow.pharos.core.llm.JsonParser
import com.flow.pharos.util.ContradictionDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContradictionDetectorTest {

    @Test
    fun `detects potential contradiction when one summary negates shared topic`() {
        val analyses = mapOf(
            "spec-a.md" to analysis(
                summary = "Server backup is enabled for all databases.",
                topics = listOf("backup", "server")
            ),
            "spec-b.md" to analysis(
                summary = "Server backup is not enabled on production.",
                topics = listOf("backup", "server")
            )
        )

        val results = ContradictionDetector.detectPotentialContradictions(analyses)

        assertEquals(1, results.size)
        assertTrue(results[0].sharedTopic in setOf("server", "backup"))
    }

    @Test
    fun `does not report contradiction when both summaries have same polarity`() {
        val analyses = mapOf(
            "a.md" to analysis(
                summary = "Backups are enabled and verified daily.",
                topics = listOf("backup")
            ),
            "b.md" to analysis(
                summary = "Backups are enabled for the reporting cluster.",
                topics = listOf("backup")
            )
        )

        val results = ContradictionDetector.detectPotentialContradictions(analyses)

        assertTrue(results.isEmpty())
    }

    @Test
    fun `does not report contradiction when topics do not overlap`() {
        val analyses = mapOf(
            "a.md" to analysis(
                summary = "Invoice processing is not automated.",
                topics = listOf("invoice")
            ),
            "b.md" to analysis(
                summary = "Backups are automated every night.",
                topics = listOf("backup")
            )
        )

        val results = ContradictionDetector.detectPotentialContradictions(analyses)
        assertTrue(results.isEmpty())
    }

    private fun analysis(summary: String, topics: List<String>): AnalysisEntity {
        return AnalysisEntity(
            id = "id-$summary",
            fileId = "file-$summary",
            summary = summary,
            topics = JsonParser.toJsonArray(topics),
            projectSuggestions = "[]",
            actionItems = "[]",
            confidence = 0.75
        )
    }
}
