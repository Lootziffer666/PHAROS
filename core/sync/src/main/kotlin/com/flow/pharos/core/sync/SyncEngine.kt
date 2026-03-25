package com.flow.pharos.core.sync

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Orchestrates sync between two folders using SHA-256 manifest comparison.
 *
 * Typical workflow:
 * 1. [generateManifest] – scan the [syncFolder] and build a [SyncManifest]
 * 2. [writeManifest]    – persist the manifest as JSON inside the folder
 * 3. [compareWithRemote] – compare with a manifest from the other device
 * 4. [sync]             – copy missing / updated files from a remote folder
 */
class SyncEngine(
    private val syncFolder: File,
    private val deviceId: String
) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    companion object {
        const val MANIFEST_FILENAME = "pharos_manifest.json"
    }

    fun generateManifest(): SyncManifest {
        val entries = mutableListOf<ManifestEntry>()
        collectEntries(syncFolder, syncFolder, entries)
        return SyncManifest(
            deviceId = deviceId,
            entries = entries.sortedBy { it.relativePath }
        )
    }

    private fun collectEntries(
        root: File,
        current: File,
        entries: MutableList<ManifestEntry>
    ) {
        val files = current.listFiles() ?: return
        for (file in files) {
            if (file.name == MANIFEST_FILENAME) continue
            if (file.isDirectory) {
                collectEntries(root, file, entries)
            } else {
                val relativePath = file.toRelativeString(root)
                entries.add(
                    ManifestEntry(
                        relativePath = relativePath,
                        sha256 = FileHasher.sha256(file),
                        size = file.length(),
                        lastModified = file.lastModified()
                    )
                )
            }
        }
    }

    fun writeManifest(manifest: SyncManifest) {
        val manifestFile = File(syncFolder, MANIFEST_FILENAME)
        manifestFile.writeText(gson.toJson(manifest))
    }

    fun readManifest(): SyncManifest? {
        val manifestFile = File(syncFolder, MANIFEST_FILENAME)
        if (!manifestFile.exists()) return null
        return try {
            gson.fromJson(manifestFile.readText(), SyncManifest::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun compareWithRemote(remoteManifest: SyncManifest): SyncDiff {
        val localManifest = generateManifest()
        return ManifestComparator.compare(localManifest, remoteManifest)
    }

    /**
     * One-way sync: copies new and (if newer) modified files from
     * [remoteFolder] into this engine's [syncFolder].
     */
    fun sync(remoteFolder: File): SyncResult {
        return try {
            val remoteEngine = SyncEngine(remoteFolder, "remote")
            val localManifest = generateManifest()
            val remoteManifest = remoteEngine.generateManifest()
            val diff = ManifestComparator.compare(localManifest, remoteManifest)

            if (!diff.hasChanges) {
                return SyncResult.NoChanges(localManifest.entries.size)
            }

            var filesTransferred = 0

            for (entry in diff.added) {
                val src = File(remoteFolder, entry.relativePath)
                val dst = File(syncFolder, entry.relativePath)
                dst.parentFile?.mkdirs()
                src.copyTo(dst, overwrite = true)
                filesTransferred++
            }

            for ((localEntry, remoteEntry) in diff.modified) {
                if (remoteEntry.lastModified > localEntry.lastModified) {
                    val src = File(remoteFolder, remoteEntry.relativePath)
                    val dst = File(syncFolder, localEntry.relativePath)
                    src.copyTo(dst, overwrite = true)
                    filesTransferred++
                }
            }

            val updatedManifest = generateManifest()
            writeManifest(updatedManifest)

            SyncResult.Success(diff, filesTransferred)
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Sync failed", e)
        }
    }
}
