package com.flow.pharos.core.storage.repository

import com.flow.pharos.core.model.ClaimStatus
import com.flow.pharos.core.model.entity.ClaimEntity
import com.flow.pharos.core.storage.db.dao.ClaimDao
import kotlinx.coroutines.flow.Flow

class ClaimRepository(private val claimDao: ClaimDao) {
    fun getClaimsByStatus(status: ClaimStatus): Flow<List<ClaimEntity>> = claimDao.getClaimsByStatus(status)
    fun getClaimsByFileId(fileId: String): Flow<List<ClaimEntity>> = claimDao.getClaimsByFileId(fileId)
    fun getAllClaims(): Flow<List<ClaimEntity>> = claimDao.getAllClaims()
    suspend fun getAllClaimsList(): List<ClaimEntity> = claimDao.getAllClaimsList()
    suspend fun getClaimById(id: String): ClaimEntity? = claimDao.getClaimById(id)
    fun getApprovedClaims(): Flow<List<ClaimEntity>> = claimDao.getApprovedClaims()
    fun getConflictClaims(): Flow<List<ClaimEntity>> = claimDao.getConflictClaims()
    suspend fun insertClaim(claim: ClaimEntity) = claimDao.insertClaim(claim)
    suspend fun insertAll(claims: List<ClaimEntity>) = claimDao.insertAll(claims)
    suspend fun updateClaim(claim: ClaimEntity) = claimDao.updateClaim(claim)
    suspend fun updateClaimStatus(id: String, status: ClaimStatus) = claimDao.updateClaimStatus(id, status)
    suspend fun countByStatus(status: ClaimStatus): Int = claimDao.countByStatus(status)
    suspend fun getClaimsByCluster(clusterId: String): List<ClaimEntity> = claimDao.getClaimsByCluster(clusterId)
    fun getClaimsForTimeline(): Flow<List<ClaimEntity>> = claimDao.getClaimsForTimeline()

    /**
     * Atomically approves a new claim and supersedes the old claim with bidirectional linking.
     */
    suspend fun approveAndSupersede(newClaim: ClaimEntity, oldClaim: ClaimEntity?) =
        claimDao.approveAndSupersede(newClaim, oldClaim)

    /**
     * Atomically reverts a previous approval, restoring original claim states.
     */
    suspend fun revertApproval(approvedClaim: ClaimEntity, supersededClaim: ClaimEntity?) =
        claimDao.revertApproval(approvedClaim, supersededClaim)
}
