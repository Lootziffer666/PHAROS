package com.flow.pharos.core.sync

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Utility for computing SHA-256 hashes of files, input streams, and byte
 * arrays.  Both Android (via ContentResolver) and desktop (via java.io.File)
 * can use these helpers to produce comparable hashes.
 */
object FileHasher {

    private const val BUFFER_SIZE = 8192

    fun sha256(file: File): String = sha256(file.inputStream())

    fun sha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        inputStream.use { stream ->
            var bytesRead = stream.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = stream.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }
}
