package com.flow.pharos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flow.pharos.core.model.ClaimStatus
import com.flow.pharos.core.model.entity.ClaimEntity
import com.flow.pharos.core.storage.repository.ClaimRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConflictResolutionUiState(
    val canUndo: Boolean = false
)

@HiltViewModel
class ConflictResolutionViewModel @Inject constructor(
    private val claimRepository: ClaimRepository
) : ViewModel() {

    val conflictClaims: StateFlow<List<ClaimEntity>> = claimRepository.getConflictClaims()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(ConflictResolutionUiState())
    val uiState: StateFlow<ConflictResolutionUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<UndoAction>()

    private sealed class UndoAction {
        data class Approved(
            val approvedClaim: ClaimEntity,
            val supersededClaim: ClaimEntity?
        ) : UndoAction()

        data class Rejected(
            val rejectedClaim: ClaimEntity
        ) : UndoAction()
    }

    fun approveNewClaim(claim: ClaimEntity) {
        viewModelScope.launch {
            // Find the conflicting claim in the same cluster that is currently APPROVED
            val supersededClaim = if (claim.clusterId != null) {
                val clusterClaims = claimRepository.getClaimsByCluster(claim.clusterId!!)
                clusterClaims.firstOrNull { it.id != claim.id && it.status == ClaimStatus.APPROVED }
            } else null

            // Approve the new claim
            claimRepository.updateClaim(claim.copy(status = ClaimStatus.APPROVED))

            // Supersede the old claim if found
            if (supersededClaim != null) {
                claimRepository.updateClaim(
                    supersededClaim.copy(
                        status = ClaimStatus.SUPERSEDED,
                        supersededById = claim.id
                    )
                )
                // Link the new claim to what it supersedes
                claimRepository.updateClaim(
                    claim.copy(status = ClaimStatus.APPROVED, supersedes = supersededClaim.id)
                )
            }

            undoStack.add(UndoAction.Approved(claim, supersededClaim))
            _uiState.value = _uiState.value.copy(canUndo = true)
        }
    }

    fun rejectClaim(claim: ClaimEntity) {
        viewModelScope.launch {
            claimRepository.updateClaim(claim.copy(status = ClaimStatus.REJECTED))
            undoStack.add(UndoAction.Rejected(claim))
            _uiState.value = _uiState.value.copy(canUndo = true)
        }
    }

    fun undoLastSwipe() {
        if (undoStack.isEmpty()) return
        viewModelScope.launch {
            when (val action = undoStack.removeAt(undoStack.lastIndex)) {
                is UndoAction.Approved -> {
                    // Revert the approved claim back to CONFLICT
                    claimRepository.updateClaim(
                        action.approvedClaim.copy(status = ClaimStatus.CONFLICT, supersedes = null)
                    )
                    // Revert the superseded claim if any
                    if (action.supersededClaim != null) {
                        claimRepository.updateClaim(
                            action.supersededClaim.copy(
                                status = ClaimStatus.APPROVED,
                                supersededById = null
                            )
                        )
                    }
                }
                is UndoAction.Rejected -> {
                    // Revert the rejected claim back to CONFLICT
                    claimRepository.updateClaim(
                        action.rejectedClaim.copy(status = ClaimStatus.CONFLICT)
                    )
                }
            }
            _uiState.value = _uiState.value.copy(canUndo = undoStack.isNotEmpty())
        }
    }

    /**
     * Finds the existing approved claim that conflicts with the given claim.
     * Returns null if no conflicting claim is found in the same cluster.
     */
    suspend fun getConflictingClaim(claim: ClaimEntity): ClaimEntity? {
        if (claim.clusterId == null) return null
        val clusterClaims = claimRepository.getClaimsByCluster(claim.clusterId!!)
        return clusterClaims.firstOrNull { it.id != claim.id && it.status == ClaimStatus.APPROVED }
    }
}
