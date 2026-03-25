package com.flow.pharos.core.sync

/**
 * A single entry in the sync manifest representing one tracked file.
 */
data class ManifestEntry(
    val relativePath: String,
    val sha256: String,
    val size: Long,
    val lastModified: Long
)

/**
 * JSON-serializable manifest listing all tracked files with their SHA-256 hashes.
 * Both Android and Windows generate and compare manifests in this format.
 */
data class SyncManifest(
    val version: Int = 1,
    val deviceId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val entries: List<ManifestEntry> = emptyList()
)
