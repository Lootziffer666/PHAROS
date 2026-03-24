package com.flow.pharos.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flow.pharos.core.model.entity.FolderEntity
import com.flow.pharos.core.storage.repository.FolderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FolderViewModel @Inject constructor(
    private val folderRepository: FolderRepository
) : ViewModel() {
    val folders: StateFlow<List<FolderEntity>> = folderRepository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFolder(treeUri: String, displayName: String) {
        viewModelScope.launch {
            try {
                folderRepository.insertFolder(FolderEntity(id = UUID.randomUUID().toString(), treeUri = treeUri, displayName = displayName))
            } catch (e: Exception) {
                Log.e("FolderViewModel", "Failed to add folder", e)
            }
        }
    }

    fun removeFolder(folderId: String) {
        viewModelScope.launch {
            try { folderRepository.deleteFolder(folderId) } catch (e: Exception) {
                Log.e("FolderViewModel", "Failed to remove folder", e)
            }
        }
    }
}
