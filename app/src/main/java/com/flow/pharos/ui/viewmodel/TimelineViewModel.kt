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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val claimRepository: ClaimRepository
) : ViewModel() {

    private val allClaimsFlow = claimRepository.getClaimsForTimeline()

    private val _statusFilter = MutableStateFlow<ClaimStatus?>(null)
    val statusFilter: StateFlow<ClaimStatus?> = _statusFilter.asStateFlow()

    val filteredClaims: StateFlow<List<ClaimEntity>> = combine(
        allClaimsFlow,
        _statusFilter
    ) { claims, filter ->
        if (filter == null) claims
        else claims.filter { it.status == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val claimStatistics: StateFlow<Map<ClaimStatus, Int>> = allClaimsFlow
        .combine(_statusFilter) { claims, _ ->
            claims.groupBy { it.status }.mapValues { it.value.size }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setStatusFilter(status: ClaimStatus?) {
        _statusFilter.value = status
    }
}
