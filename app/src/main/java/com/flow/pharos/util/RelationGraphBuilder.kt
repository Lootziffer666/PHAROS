package com.flow.pharos.util

import com.flow.pharos.core.llm.JsonParser
import com.flow.pharos.core.model.entity.AnalysisEntity

data class ProjectRelationGraph(
    val fileNodes: List<String>,
    val topicNodes: List<String>,
    val edges: List<Pair<String, String>>
)

object RelationGraphBuilder {

    fun build(
        analysesByFileName: Map<String, AnalysisEntity>,
        maxTopics: Int = 12
    ): ProjectRelationGraph {
        val topicFrequency = linkedMapOf<String, Int>()
        val fileToTopics = linkedMapOf<String, List<String>>()

        analysesByFileName.forEach { (fileName, analysis) ->
            val topics = JsonParser.fromJsonArray(analysis.topics)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            fileToTopics[fileName] = topics
            topics.forEach { topic ->
                topicFrequency[topic] = (topicFrequency[topic] ?: 0) + 1
            }
        }

        val selectedTopics = topicFrequency.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(maxTopics)
            .map { it.key }
            .toList()
        val selectedTopicSet = selectedTopics.toSet()

        val edges = buildList {
            fileToTopics.forEach { (file, topics) ->
                topics.filter { it in selectedTopicSet }.sorted().forEach { topic ->
                    add(file to topic)
                }
            }
        }.sortedWith(compareBy<Pair<String, String>> { it.first }.thenBy { it.second })

        return ProjectRelationGraph(
            fileNodes = analysesByFileName.keys.sorted(),
            topicNodes = selectedTopics,
            edges = edges
        )
    }
}
