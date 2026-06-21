package com.flow.pharos.core.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flow.pharos.core.model.ClaimStatus
import com.flow.pharos.core.model.entity.ClaimEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClaimDao {
    @Query("SELECT * FROM claims WHERE status = :status")
    fun getClaimsByStatus(status: ClaimStatus): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims WHERE sourceFileId = :fileId")
    fun getClaimsByFileId(fileId: String): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims")
    fun getAllClaims(): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims")
    suspend fun getAllClaimsList(): List<ClaimEntity>

    @Query("SELECT * FROM claims WHERE id = :id")
    suspend fun getClaimById(id: String): ClaimEntity?

    @Query("SELECT * FROM claims WHERE status = 'APPROVED'")
    fun getApprovedClaims(): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims WHERE status = 'CONFLICT'")
    fun getConflictClaims(): Flow<List<ClaimEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaim(claim: ClaimEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(claims: List<ClaimEntity>)

    @Update
    suspend fun updateClaim(claim: ClaimEntity)

    @Query("UPDATE claims SET status = :status WHERE id = :id")
    suspend fun updateClaimStatus(id: String, status: ClaimStatus)

    @Query("SELECT COUNT(*) FROM claims WHERE status = :status")
    suspend fun countByStatus(status: ClaimStatus): Int

    @Query("SELECT * FROM claims WHERE clusterId = :clusterId")
    suspend fun getClaimsByCluster(clusterId: String): List<ClaimEntity>

    @Query("SELECT * FROM claims ORDER BY extractedAt DESC")
    fun getClaimsForTimeline(): Flow<List<ClaimEntity>>
}
