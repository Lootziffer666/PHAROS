package com.flow.pharos.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SyncEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var localFolder: java.io.File
    private lateinit var remoteFolder: java.io.File

    @Before
    fun setUp() {
        localFolder = tempFolder.newFolder("local")
        remoteFolder = tempFolder.newFolder("remote")
    }

    @Test
    fun `generateManifest creates manifest from folder contents`() {
        localFolder.resolve("file1.txt").writeText("hello")
        localFolder.resolve("file2.txt").writeText("world")

        val engine = SyncEngine(localFolder, "test-device")
        val manifest = engine.generateManifest()

        assertEquals("test-device", manifest.deviceId)
        assertEquals(2, manifest.entries.size)
        assertTrue(manifest.entries.any { it.relativePath == "file1.txt" })
        assertTrue(manifest.entries.any { it.relativePath == "file2.txt" })
    }

    @Test
    fun `generateManifest handles subdirectories`() {
        localFolder.resolve("sub").mkdirs()
        localFolder.resolve("sub/nested.txt").writeText("nested content")

        val engine = SyncEngine(localFolder, "test-device")
        val manifest = engine.generateManifest()

        assertEquals(1, manifest.entries.size)
        assertTrue(manifest.entries[0].relativePath.contains("nested.txt"))
    }

    @Test
    fun `generateManifest excludes manifest file`() {
        localFolder.resolve("file1.txt").writeText("content")
        localFolder.resolve(SyncEngine.MANIFEST_FILENAME).writeText("{}")

        val engine = SyncEngine(localFolder, "test-device")
        val manifest = engine.generateManifest()

        assertEquals(1, manifest.entries.size)
        assertEquals("file1.txt", manifest.entries[0].relativePath)
    }

    @Test
    fun `writeManifest and readManifest roundtrip`() {
        val engine = SyncEngine(localFolder, "test-device")
        val original = SyncManifest(
            deviceId = "test-device",
            entries = listOf(ManifestEntry("file.txt", "abc123", 100, 1000L))
        )

        engine.writeManifest(original)
        val restored = engine.readManifest()

        assertNotNull(restored)
        assertEquals(original.deviceId, restored!!.deviceId)
        assertEquals(original.entries.size, restored.entries.size)
        assertEquals(original.entries[0].relativePath, restored.entries[0].relativePath)
        assertEquals(original.entries[0].sha256, restored.entries[0].sha256)
    }

    @Test
    fun `readManifest returns null when no manifest exists`() {
        val engine = SyncEngine(localFolder, "test-device")
        assertNull(engine.readManifest())
    }

    @Test
    fun `sync copies new files from remote to local`() {
        remoteFolder.resolve("new-file.txt").writeText("new content")

        val engine = SyncEngine(localFolder, "local-device")
        val result = engine.sync(remoteFolder)

        assertTrue(result is SyncResult.Success)
        val success = result as SyncResult.Success
        assertTrue(success.filesTransferred > 0)
        assertTrue(localFolder.resolve("new-file.txt").exists())
        assertEquals("new content", localFolder.resolve("new-file.txt").readText())
    }

    @Test
    fun `sync returns NoChanges when folders are identical`() {
        localFolder.resolve("file.txt").writeText("same")
        remoteFolder.resolve("file.txt").writeText("same")

        val engine = SyncEngine(localFolder, "local-device")
        val result = engine.sync(remoteFolder)

        assertTrue(result is SyncResult.NoChanges)
    }

    @Test
    fun `generateManifest produces correct hashes`() {
        localFolder.resolve("test.txt").writeText("hello")

        val engine = SyncEngine(localFolder, "test-device")
        val manifest = engine.generateManifest()

        val expectedHash = FileHasher.sha256("hello".toByteArray())
        assertEquals(expectedHash, manifest.entries[0].sha256)
    }

    @Test
    fun `sync handles empty folders`() {
        val engine = SyncEngine(localFolder, "local-device")
        val result = engine.sync(remoteFolder)

        assertTrue(result is SyncResult.NoChanges)
    }

    @Test
    fun `generateManifest entries are sorted by path`() {
        localFolder.resolve("charlie.txt").writeText("c")
        localFolder.resolve("alpha.txt").writeText("a")
        localFolder.resolve("bravo.txt").writeText("b")

        val engine = SyncEngine(localFolder, "test-device")
        val manifest = engine.generateManifest()

        assertEquals("alpha.txt", manifest.entries[0].relativePath)
        assertEquals("bravo.txt", manifest.entries[1].relativePath)
        assertEquals("charlie.txt", manifest.entries[2].relativePath)
    }
}
