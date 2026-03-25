package com.flow.pharos.core.sync

/**
 * Compares two [SyncManifest]s and returns a [SyncDiff] describing the
 * differences. The diff is computed from the perspective of the local
 * manifest – i.e. "added" means the file exists on the remote but not
 * locally.
 */
object ManifestComparator {

    fun compare(local: SyncManifest, remote: SyncManifest): SyncDiff {
        val localMap = local.entries.associateBy { it.relativePath }
        val remoteMap = remote.entries.associateBy { it.relativePath }

        val added = mutableListOf<ManifestEntry>()
        val modified = mutableListOf<Pair<ManifestEntry, ManifestEntry>>()
        val deleted = mutableListOf<ManifestEntry>()
        val unchanged = mutableListOf<ManifestEntry>()

        for ((path, remoteEntry) in remoteMap) {
            val localEntry = localMap[path]
            if (localEntry == null) {
                added.add(remoteEntry)
            } else if (localEntry.sha256 != remoteEntry.sha256) {
                modified.add(localEntry to remoteEntry)
            } else {
                unchanged.add(localEntry)
            }
        }

        for ((path, localEntry) in localMap) {
            if (path !in remoteMap) {
                deleted.add(localEntry)
            }
        }

        return SyncDiff(added, modified, deleted, unchanged)
    }
}
