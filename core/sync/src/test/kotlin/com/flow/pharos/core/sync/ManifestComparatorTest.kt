package com.flow.pharos.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestComparatorTest {

    @Test
    fun `compare identical manifests returns no changes`() {
        val entries = listOf(
            ManifestEntry("file1.txt", "abc123", 100, 1000L),
            ManifestEntry("file2.txt", "def456", 200, 2000L)
        )
        val local = SyncManifest(deviceId = "device-a", entries = entries)
        val remote = SyncManifest(deviceId = "device-b", entries = entries)

        val diff = ManifestComparator.compare(local, remote)

        assertFalse(diff.hasChanges)
        assertEquals(0, diff.added.size)
        assertEquals(0, diff.modified.size)
        assertEquals(0, diff.deleted.size)
        assertEquals(2, diff.unchanged.size)
    }

    @Test
    fun `compare detects added files`() {
        val local = SyncManifest(deviceId = "device-a", entries = listOf(
            ManifestEntry("file1.txt", "abc123", 100, 1000L)
        ))
        val remote = SyncManifest(deviceId = "device-b", entries = listOf(
            ManifestEntry("file1.txt", "abc123", 100, 1000L),
            ManifestEntry("file2.txt", "def456", 200, 2000L)
        ))

        val diff = ManifestComparator.compare(local, remote)

        assertTrue(diff.hasChanges)
        assertEquals(1, diff.added.size)
        assertEquals("file2.txt", diff.added[0].relativePath)
    }

    @Test
    fun `compare detects modified files`() {
        val local = SyncManifest(deviceId = "device-a", entries = listOf(
            ManifestEntry("file1.txt", "abc123", 100, 1000L)
        ))
        val remote = SyncManifest(deviceId = "device-b", entries = listOf(
            ManifestEntry("file1.txt", "xyz789", 150, 3000L)
        ))

        val diff = ManifestComparator.compare(local, remote)

        assertTrue(diff.hasChanges)
        assertEquals(1, diff.modified.size)
        assertEquals("abc123", diff.modified[0].first.sha256)
        assertEquals("xyz789", diff.modified[0].second.sha256)
    }

    @Test
    fun `compare detects deleted files`() {
        val local = SyncManifest(deviceId = "device-a", entries = listOf(
            ManifestEntry("file1.txt", "abc123", 100, 1000L),
            ManifestEntry("file2.txt", "def456", 200, 2000L)
        ))
        val remote = SyncManifest(deviceId = "device-b", entries = listOf(
            ManifestEntry("file1.txt", "abc123", 100, 1000L)
        ))

        val diff = ManifestComparator.compare(local, remote)

        assertTrue(diff.hasChanges)
        assertEquals(1, diff.deleted.size)
        assertEquals("file2.txt", diff.deleted[0].relativePath)
    }

    @Test
    fun `compare with empty local manifest`() {
        val local = SyncManifest(deviceId = "device-a", entries = emptyList())
        val remote = SyncManifest(deviceId = "device-b", entries = listOf(
            ManifestEntry("file1.txt", "abc123", 100, 1000L)
        ))

        val diff = ManifestComparator.compare(local, remote)

        assertTrue(diff.hasChanges)
        assertEquals(1, diff.added.size)
        assertEquals(0, diff.deleted.size)
    }

    @Test
    fun `compare with empty remote manifest`() {
        val local = SyncManifest(deviceId = "device-a", entries = listOf(
            ManifestEntry("file1.txt", "abc123", 100, 1000L)
        ))
        val remote = SyncManifest(deviceId = "device-b", entries = emptyList())

        val diff = ManifestComparator.compare(local, remote)

        assertTrue(diff.hasChanges)
        assertEquals(0, diff.added.size)
        assertEquals(1, diff.deleted.size)
    }

    @Test
    fun `compare both manifests empty returns no changes`() {
        val local = SyncManifest(deviceId = "device-a", entries = emptyList())
        val remote = SyncManifest(deviceId = "device-b", entries = emptyList())

        val diff = ManifestComparator.compare(local, remote)

        assertFalse(diff.hasChanges)
        assertEquals(0, diff.totalChanges)
    }

    @Test
    fun `totalChanges sums all change types`() {
        val local = SyncManifest(deviceId = "device-a", entries = listOf(
            ManifestEntry("keep.txt", "aaa", 100, 1000L),
            ManifestEntry("modified.txt", "old", 100, 1000L),
            ManifestEntry("deleted.txt", "ddd", 100, 1000L)
        ))
        val remote = SyncManifest(deviceId = "device-b", entries = listOf(
            ManifestEntry("keep.txt", "aaa", 100, 1000L),
            ManifestEntry("modified.txt", "new", 150, 3000L),
            ManifestEntry("added.txt", "eee", 100, 1000L)
        ))

        val diff = ManifestComparator.compare(local, remote)

        assertEquals(3, diff.totalChanges)
        assertEquals(1, diff.added.size)
        assertEquals(1, diff.modified.size)
        assertEquals(1, diff.deleted.size)
        assertEquals(1, diff.unchanged.size)
    }
}
