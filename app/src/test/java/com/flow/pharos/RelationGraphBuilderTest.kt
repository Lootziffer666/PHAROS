package com.flow.pharos

import com.flow.pharos.core.llm.JsonParser
import com.flow.pharos.core.model.entity.AnalysisEntity
import com.flow.pharos.util.RelationGraphBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationGraphBuilderTest {

    @Test
    fun `build creates edges between files and their topics`() {
        val analyses = mapOf(
            "file-a.md" to analysis(topics = listOf("kotlin", "android")),
            "file-b.md" to analysis(topics = listOf("android", "sync"))
        )

        val graph = RelationGraphBuilder.build(analyses)

        assertEquals(2, graph.fileNodes.size)
        assertTrue("android" in graph.topicNodes)
        assertTrue(graph.edges.contains("file-a.md" to "android"))
        assertTrue(graph.edges.contains("file-b.md" to "sync"))
    }

    @Test
    fun `build respects max topic cap`() {
        val analyses = mapOf(
            "f.md" to analysis(topics = listOf("a", "b", "c", "d"))
        )

        val graph = RelationGraphBuilder.build(analyses, maxTopics = 2)

        assertEquals(2, graph.topicNodes.size)
        assertEquals(2, graph.edges.size)
    }

    private fun analysis(topics: List<String>): AnalysisEntity {
        return AnalysisEntity(
            id = "id-${topics.joinToString()}",
            fileId = "file-${topics.joinToString()}",
            summary = "summary",
            topics = JsonParser.toJsonArray(topics),
            projectSuggestions = "[]",
            actionItems = "[]",
            confidence = 0.9
        )
    }
}
