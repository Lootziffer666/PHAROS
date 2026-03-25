package com.flow.pharos.core.sync

/**
 * Result of comparing two manifests via [ManifestComparator].
 */
data class SyncDiff(
    val added: List<ManifestEntry>,
    val modified: List<Pair<ManifestEntry, ManifestEntry>>,
    val deleted: List<ManifestEntry>,
    val unchanged: List<ManifestEntry>
) {
    val hasChanges: Boolean
        get() = added.isNotEmpty() || modified.isNotEmpty() || deleted.isNotEmpty()

    val totalChanges: Int
        get() = added.size + modified.size + deleted.size
}

/**
 * Outcome of a [SyncEngine.sync] operation.
 */
sealed class SyncResult {
    data class Success(val diff: SyncDiff, val filesTransferred: Int) : SyncResult()
    data class NoChanges(val totalFiles: Int) : SyncResult()
    data class Error(val message: String, val cause: Throwable? = null) : SyncResult()
}
