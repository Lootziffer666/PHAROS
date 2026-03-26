package com.flow.pharos.core.sync

import com.flow.pharos.core.truth.ConflictRecord
import com.flow.pharos.core.truth.ProvenanceLevel
import com.flow.pharos.core.truth.TrustMetadata
import com.flow.pharos.core.truth.VerificationState

/**
 * Detects sync conflicts where both local and remote have diverged
 * from a common ancestor. Pharos must surface these conflicts rather
 * than silently picking a winner.
 *
 * A conflict occurs when a file exists on both sides with different
 * content (different SHA-256 hashes) — this means both devices modified
 * the file independently. Unlike a simple "modified" entry which
 * the SyncEngine can resolve via last-write-wins, a true conflict
 * requires user attention.
 */
object SyncConflictDetector {

    /**
     * Analyzes a [SyncDiff] and identifies entries that represent true
     * conflicts (both sides modified the same file differently).
     *
     * Every modified entry where both local and remote have different
     * hashes is a potential conflict that must be surfaced.
     */
    fun detectConflicts(diff: SyncDiff): List<SyncConflict> {
        return diff.modified.map { (local, remote) ->
            SyncConflict(
                relativePath = local.relativePath,
                localEntry = local,
                remoteEntry = remote,
                conflictRecord = ConflictRecord(
                    id = "sync-conflict:${local.relativePath}",
                    claimIds = listOf(
                        "local:${local.relativePath}:${local.sha256}",
                        "remote:${remote.relativePath}:${remote.sha256}"
                    ),
                    summary = "File '${local.relativePath}' modified on both devices with different content"
                ),
                localTrust = TrustMetadata(
                    sourceId = "local:${local.relativePath}",
                    capturedAt = local.lastModified,
                    provenance = ProvenanceLevel.SOURCE,
                    verification = VerificationState.Unverified
                ),
                remoteTrust = TrustMetadata(
                    sourceId = "remote:${remote.relativePath}",
                    capturedAt = remote.lastModified,
                    provenance = ProvenanceLevel.SOURCE,
                    verification = VerificationState.Unverified
                )
            )
        }
    }

    /**
     * Returns true if the diff contains entries that represent true
     * conflicts requiring user attention.
     */
    fun hasConflicts(diff: SyncDiff): Boolean = diff.modified.isNotEmpty()
}

/**
 * Represents a sync conflict for a single file where local and remote
 * versions have diverged. Both versions carry provenance metadata so
 * the user can make an informed decision.
 */
data class SyncConflict(
    val relativePath: String,
    val localEntry: ManifestEntry,
    val remoteEntry: ManifestEntry,
    val conflictRecord: ConflictRecord,
    val localTrust: TrustMetadata,
    val remoteTrust: TrustMetadata
) {
    /** True if the remote version is newer based on lastModified timestamps. */
    val remoteIsNewer: Boolean
        get() = remoteEntry.lastModified > localEntry.lastModified

    /** True if the local version is newer based on lastModified timestamps. */
    val localIsNewer: Boolean
        get() = localEntry.lastModified > remoteEntry.lastModified
}
