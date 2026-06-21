package com.flow.pharos.core.model.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flow.pharos.core.model.ClaimStatus

@Entity(
    tableName = "claims",
    foreignKeys = [
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceFileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sourceFileId"),
        Index("status"),
        Index("clusterId")
    ]
)
data class ClaimEntity(
    @PrimaryKey
    val id: String,
    val content: String,
    val sourceFileId: String,
    val sourceFileName: String,
    val sourceTimestamp: Long,
    val extractedAt: Long,
    val status: ClaimStatus = ClaimStatus.PENDING,
    val confidence: Double,
    val clusterId: String? = null,
    val supersededById: String? = null,
    val supersedes: String? = null,
    val aiRationale: String? = null
)
