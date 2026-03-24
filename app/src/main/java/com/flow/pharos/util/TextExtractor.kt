package com.flow.pharos.util

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest

class TextExtractor(private val context: Context) {
    fun extractText(uri: Uri, maxChars: Int = MAX_CHARS): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (sb.length >= maxChars) {
                        sb.append("\n[... truncated at $maxChars characters ...]")
                        break
                    }
                    if (sb.isNotEmpty()) sb.append('\n')
                    sb.append(line)
                }
                sb.toString().ifEmpty { null }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun computeHash(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val MAX_CHARS = 30_000
    }
}
