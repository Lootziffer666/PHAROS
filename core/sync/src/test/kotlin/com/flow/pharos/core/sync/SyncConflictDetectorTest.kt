package com.flow.pharos.core.sync

import com.flow.pharos.core.truth.ProvenanceLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [SyncConflictDetector] — verifies conflict detection in sync diffs.
 *
 * These are parity-critical tests: both Android and Desktop share this exact
 * conflict detection logic through the core/sync JVM module. Any conflict
 * detected on one platform must be detected identically on the other.
 */
class SyncConflictDetectorTest {

    @Test
    fun `no conflicts when diff has no modifications`() {
        val diff = SyncDiff(
            added = listOf(ManifestEntry("new.txt", "abc", 100, 1000L)),
            modified = emptyList(),
            deleted = listOf(ManifestEntry("old.txt", "def", 200, 2000L)),
            unchanged = listOf(ManifestEntry("same.txt", "ghi", 300, 3000L))
        )
        assertFalse(SyncConflictDetector.hasConflicts(diff))
        assertEquals(0, SyncConflictDetector.detectConflicts(diff).size)
    }

    @Test
    fun `detects conflict when file modified on both sides`() {
        val localEntry = ManifestEntry("doc.txt", "local-hash", 100, 1000L)
        val remoteEntry = ManifestEntry("doc.txt", "remote-hash", 150, 2000L)
        val diff = SyncDiff(
            added = emptyList(),
            modified = listOf(localEntry to remoteEntry),
            deleted = emptyList(),
            unchanged = emptyList()
        )

        assertTrue(SyncConflictDetector.hasConflicts(diff))
        val conflicts = SyncConflictDetector.detectConflicts(diff)
        assertEquals(1, conflicts.size)
        assertEquals("doc.txt", conflicts[0].relativePath)
    }

    @Test
    fun `conflict record has correct claim IDs`() {
        val localEntry = ManifestEntry("doc.txt", "local-hash", 100, 1000L)
        val remoteEntry = ManifestEntry("doc.txt", "remote-hash", 150, 2000L)
        val diff = SyncDiff(
            added = emptyList(),
            modified = listOf(localEntry to remoteEntry),
            deleted = emptyList(),
            unchanged = emptyList()
        )

        val conflict = SyncConflictDetector.detectConflicts(diff)[0]
        assertEquals(2, conflict.conflictRecord.claimIds.size)
        assertTrue(conflict.conflictRecord.claimIds[0].contains("local"))
        assertTrue(conflict.conflictRecord.claimIds[1].contains("remote"))
    }

    @Test
    fun `conflict record is open by default`() {
        val localEntry = ManifestEntry("doc.txt", "local-hash", 100, 1000L)
        val remoteEntry = ManifestEntry("doc.txt", "remote-hash", 150, 2000L)
        val diff = SyncDiff(
            added = emptyList(),
            modified = listOf(localEntry to remoteEntry),
            deleted = emptyList(),
            unchanged = emptyList()
        )

        val conflict = SyncConflictDetector.detectConflicts(diff)[0]
        assertTrue(conflict.conflictRecord.isOpen)
    }

    @Test
    fun `trust metadata has SOURCE provenance for both sides`() {
        val localEntry = ManifestEntry("doc.txt", "local-hash", 100, 1000L)
        val remoteEntry = ManifestEntry("doc.txt", "remote-hash", 150, 2000L)
        val diff = SyncDiff(
            added = emptyList(),
            modified = listOf(localEntry to remoteEntry),
            deleted = emptyList(),
            unchanged = emptyList()
        )

        val conflict = SyncConflictDetector.detectConflicts(diff)[0]
        assertEquals(ProvenanceLevel.SOURCE, conflict.localTrust.provenance)
        assertEquals(ProvenanceLevel.SOURCE, conflict.remoteTrust.provenance)
    }

    @Test
    fun `remote is newer when remote timestamp is later`() {
        val localEntry = ManifestEntry("doc.txt", "local-hash", 100, 1000L)
        val remoteEntry = ManifestEntry("doc.txt", "remote-hash", 150, 2000L)
        val diff = SyncDiff(
            added = emptyList(),
            modified = listOf(localEntry to remoteEntry),
            deleted = emptyList(),
            unchanged = emptyList()
        )

        val conflict = SyncConflictDetector.detectConflicts(diff)[0]
        assertTrue(conflict.remoteIsNewer)
        assertFalse(conflict.localIsNewer)
    }

    @Test
    fun `local is newer when local timestamp is later`() {
        val localEntry = ManifestEntry("doc.txt", "local-hash", 100, 3000L)
        val remoteEntry = ManifestEntry("doc.txt", "remote-hash", 150, 2000L)
        val diff = SyncDiff(
            added = emptyList(),
            modified = listOf(localEntry to remoteEntry),
            deleted = emptyList(),
            unchanged = emptyList()
        )

        val conflict = SyncConflictDetector.detectConflicts(diff)[0]
        assertTrue(conflict.localIsNewer)
        assertFalse(conflict.remoteIsNewer)
    }

    @Test
    fun `detects multiple conflicts for multiple modified files`() {
        val diff = SyncDiff(
            added = emptyList(),
            modified = listOf(
                ManifestEntry("a.txt", "la", 100, 1000L) to ManifestEntry("a.txt", "ra", 100, 2000L),
                ManifestEntry("b.txt", "lb", 200, 1000L) to ManifestEntry("b.txt", "rb", 200, 2000L),
                ManifestEntry("c.txt", "lc", 300, 1000L) to ManifestEntry("c.txt", "rc", 300, 2000L)
            ),
            deleted = emptyList(),
            unchanged = emptyList()
        )

        val conflicts = SyncConflictDetector.detectConflicts(diff)
        assertEquals(3, conflicts.size)
        assertEquals("a.txt", conflicts[0].relativePath)
        assertEquals("b.txt", conflicts[1].relativePath)
        assertEquals("c.txt", conflicts[2].relativePath)
    }

    @Test
    fun `conflict summary describes the conflict`() {
        val localEntry = ManifestEntry("doc.txt", "local-hash", 100, 1000L)
        val remoteEntry = ManifestEntry("doc.txt", "remote-hash", 150, 2000L)
        val diff = SyncDiff(
            added = emptyList(),
            modified = listOf(localEntry to remoteEntry),
            deleted = emptyList(),
            unchanged = emptyList()
        )

        val conflict = SyncConflictDetector.detectConflicts(diff)[0]
        assertTrue(conflict.conflictRecord.summary.contains("doc.txt"))
        assertTrue(conflict.conflictRecord.summary.contains("both devices"))
    }
}
