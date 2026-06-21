package com.flow.pharos.core.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.flow.pharos.core.model.ClaimStatus
import com.flow.pharos.core.model.entity.ClaimEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ClaimDao {
    @Query("SELECT * FROM claims WHERE status = :status")
    abstract fun getClaimsByStatus(status: ClaimStatus): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims WHERE sourceFileId = :fileId")
    abstract fun getClaimsByFileId(fileId: String): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims")
    abstract fun getAllClaims(): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims")
    abstract suspend fun getAllClaimsList(): List<ClaimEntity>

    @Query("SELECT * FROM claims WHERE id = :id")
    abstract suspend fun getClaimById(id: String): ClaimEntity?

    @Query("SELECT * FROM claims WHERE status = 'APPROVED'")
    abstract fun getApprovedClaims(): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims WHERE status = 'CONFLICT'")
    abstract fun getConflictClaims(): Flow<List<ClaimEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertClaim(claim: ClaimEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(claims: List<ClaimEntity>)

    @Update
    abstract suspend fun updateClaim(claim: ClaimEntity)

    @Query("UPDATE claims SET status = :status WHERE id = :id")
    abstract suspend fun updateClaimStatus(id: String, status: ClaimStatus)

    @Query("SELECT COUNT(*) FROM claims WHERE status = :status")
    abstract suspend fun countByStatus(status: ClaimStatus): Int

    @Query("SELECT * FROM claims WHERE clusterId = :clusterId")
    abstract suspend fun getClaimsByCluster(clusterId: String): List<ClaimEntity>

    @Query("SELECT * FROM claims ORDER BY extractedAt DESC")
    abstract fun getClaimsForTimeline(): Flow<List<ClaimEntity>>

    /**
     * Atomically approves a new claim, supersedes the old claim, and links them.
     * This prevents inconsistent state if the process dies mid-operation.
     */
    @Transaction
    open suspend fun approveAndSupersede(
        newClaim: ClaimEntity,
        oldClaim: ClaimEntity?
    ) {
        // Approve the new claim (with supersedes link if applicable)
        val approvedNew = if (oldClaim != null) {
            newClaim.copy(status = ClaimStatus.APPROVED, supersedes = oldClaim.id)
        } else {
            newClaim.copy(status = ClaimStatus.APPROVED)
        }
        updateClaim(approvedNew)

        // Supersede the old claim if present
        if (oldClaim != null) {
            updateClaim(
                oldClaim.copy(
                    status = ClaimStatus.SUPERSEDED,
                    supersededById = newClaim.id
                )
            )
        }
    }

    /**
     * Atomically reverts an approval: sets the new claim back to CONFLICT and
     * restores the old claim to APPROVED.
     */
    @Transaction
    open suspend fun revertApproval(
        approvedClaim: ClaimEntity,
        supersededClaim: ClaimEntity?
    ) {
        updateClaim(approvedClaim.copy(status = ClaimStatus.CONFLICT, supersedes = null))
        if (supersededClaim != null) {
            updateClaim(supersededClaim.copy(status = ClaimStatus.APPROVED, supersededById = null))
        }
    }
}
