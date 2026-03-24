package com.flow.pharos.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flow.pharos.core.model.entity.AnalysisEntity
import com.flow.pharos.core.model.entity.FileEntity
import com.flow.pharos.core.model.entity.ProjectEntity
import com.flow.pharos.core.storage.repository.AnalysisRepository
import com.flow.pharos.core.storage.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {
    val projects: StateFlow<List<ProjectEntity>> = projectRepository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _projectFiles = MutableStateFlow<List<FileEntity>>(emptyList())
    val projectFiles: StateFlow<List<FileEntity>> = _projectFiles.asStateFlow()

    private val _fileAnalyses = MutableStateFlow<Map<String, AnalysisEntity>>(emptyMap())
    val fileAnalyses: StateFlow<Map<String, AnalysisEntity>> = _fileAnalyses.asStateFlow()

    fun loadProjectFiles(projectId: String) {
        viewModelScope.launch {
            try {
                val files = projectRepository.getFilesForProjectList(projectId)
                _projectFiles.value = files
                val analyses = mutableMapOf<String, AnalysisEntity>()
                files.forEach { file -> analysisRepository.getLatestAnalysisForFile(file.id)?.let { analyses[file.id] = it } }
                _fileAnalyses.value = analyses
            } catch (e: Exception) {
                Log.e("ProjectsViewModel", "Failed to load project files", e)
            }
        }
    }
}
