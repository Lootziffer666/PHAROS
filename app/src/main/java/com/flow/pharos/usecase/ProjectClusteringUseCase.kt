package com.flow.pharos.usecase

import com.flow.pharos.core.llm.JsonParser
import com.flow.pharos.core.model.entity.ProjectEntity
import com.flow.pharos.core.storage.repository.AnalysisRepository
import com.flow.pharos.core.storage.repository.FileRepository
import com.flow.pharos.core.storage.repository.ProjectRepository
import java.util.UUID

data class ClusteringResult(val projectsCreated: Int, val projectsUpdated: Int, val filesAssigned: Int)

class ProjectClusteringUseCase(
    private val analysisRepository: AnalysisRepository,
    private val projectRepository: ProjectRepository,
    private val fileRepository: FileRepository
) {
    suspend fun clusterProjects(): ClusteringResult {
        val allAnalyses = analysisRepository.getAllAnalyses()
        if (allAnalyses.isEmpty()) return ClusteringResult(0, 0, 0)

        val suggestionCounts = mutableMapOf<String, MutableList<String>>()
        val topicCounts = mutableMapOf<String, MutableList<String>>()

        allAnalyses.forEach { analysis ->
            JsonParser.fromJsonArray(analysis.projectSuggestions).forEach { suggestion ->
                val normalized = normalizeProjectName(suggestion)
                if (normalized.isNotBlank()) suggestionCounts.getOrPut(normalized) { mutableListOf() }.add(analysis.fileId)
            }
            JsonParser.fromJsonArray(analysis.topics).forEach { topic ->
                val normalized = normalizeProjectName(topic)
                if (normalized.isNotBlank()) topicCounts.getOrPut(normalized) { mutableListOf() }.add(analysis.fileId)
            }
        }

        val projectCandidates = mutableMapOf<String, MutableSet<String>>()
        suggestionCounts.forEach { (name, fileIds) -> projectCandidates.getOrPut(name) { mutableSetOf() }.addAll(fileIds) }
        topicCounts.filter { it.value.size >= 2 }.forEach { (name, fileIds) -> projectCandidates.getOrPut(name) { mutableSetOf() }.addAll(fileIds) }

        val topProjects = projectCandidates.entries.sortedByDescending { it.value.size }.take(MAX_PROJECTS)
        projectRepository.clearAllCrossRefs()
        var created = 0; var updated = 0; var filesAssigned = 0

        topProjects.forEach { (projectName, fileIds) ->
            val existing = projectRepository.getProjectByName(projectName)
            val projectId: String
            if (existing != null) {
                projectId = existing.id
                projectRepository.updateProject(existing.copy(updatedAt = System.currentTimeMillis()))
                updated++
            } else {
                projectId = UUID.randomUUID().toString()
                projectRepository.insertProject(
                    ProjectEntity(
                        id = projectId, name = projectName,
                        description = "Project \"$projectName\" - derived from document analysis (${fileIds.size} files)",
                        createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
                    )
                )
                created++
            }
            projectRepository.clearProjectFiles(projectId)
            fileIds.forEach { fileId -> projectRepository.addFileToProject(projectId, fileId); filesAssigned++ }
        }
        return ClusteringResult(created, updated, filesAssigned)
    }

    companion object {
        private const val MAX_PROJECTS = 20
        fun normalizeProjectName(name: String): String = name.trim().lowercase().replaceFirstChar { it.uppercaseChar() }
    }
}
