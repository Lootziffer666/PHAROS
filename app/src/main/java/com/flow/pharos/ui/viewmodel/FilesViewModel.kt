package com.flow.pharos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flow.pharos.core.model.entity.AnalysisEntity
import com.flow.pharos.core.model.entity.FileEntity
import com.flow.pharos.core.storage.repository.AnalysisRepository
import com.flow.pharos.core.storage.repository.FileRepository
import com.flow.pharos.usecase.AnalysisUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val analysisRepository: AnalysisRepository,
    private val analysisUseCase: AnalysisUseCase
) : ViewModel() {
    val files: StateFlow<List<FileEntity>> = fileRepository.getAllFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFileAnalysis = MutableStateFlow<AnalysisEntity?>(null)
    val selectedFileAnalysis: StateFlow<AnalysisEntity?> = _selectedFileAnalysis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    fun loadFileAnalysis(fileId: String) {
        viewModelScope.launch { _selectedFileAnalysis.value = analysisRepository.getLatestAnalysisForFile(fileId) }
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun analyzeSingleFile(fileId: String) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _errorMessage.value = null
            try {
                analysisUseCase.analyzeSingleFile(fileId)
                loadFileAnalysis(fileId)
            } catch (e: Exception) {
                _errorMessage.value = "Analysis failed: ${e.message}"
            }
            _isAnalyzing.value = false
        }
    }
}
