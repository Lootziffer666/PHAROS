package com.flow.pharos.usecase

import android.content.Context
import android.net.Uri
import com.flow.pharos.core.llm.AiApiProvider
import com.flow.pharos.core.llm.JsonParser
import com.flow.pharos.core.model.FileStatus
import com.flow.pharos.core.model.entity.AnalysisEntity
import com.flow.pharos.core.model.entity.FileEntity
import com.flow.pharos.core.storage.repository.AnalysisRepository
import com.flow.pharos.core.storage.repository.FileRepository
import com.flow.pharos.core.storage.repository.SettingsRepository
import com.flow.pharos.util.PdfTextExtractor
import com.flow.pharos.util.TextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

data class AnalysisProgress(val current: Int, val total: Int, val currentFileName: String = "", val succeeded: Int = 0, val failed: Int = 0)
data class AnalysisResult(val totalProcessed: Int, val succeeded: Int, val failed: Int, val skipped: Int)
data class AnalysisPreview(val fileCount: Int, val estimatedTokens: Long, val files: List<FileEntity>)

class AnalysisUseCase(
    private val fileRepository: FileRepository,
    private val analysisRepository: AnalysisRepository,
    private val settingsRepository: SettingsRepository,
    private val aiApiProvider: AiApiProvider,
    private val textExtractor: TextExtractor,
    private val pdfTextExtractor: PdfTextExtractor,
    private val context: Context
) {
    suspend fun getAnalysisPreview(analyzeAll: Boolean = false): AnalysisPreview {
        val files = if (analyzeAll) fileRepository.getAllFilesList().filter { it.status != FileStatus.UNSUPPORTED }
        else fileRepository.getFilesByStatuses(listOf(FileStatus.NEVER, FileStatus.STALE))
        var totalChars = 0L
        files.forEach { totalChars += estimateChars(it) }
        return AnalysisPreview(files.size, totalChars / 4, files)
    }

    suspend fun analyzeFiles(
        analyzeAll: Boolean = false,
        onProgress: suspend (AnalysisProgress) -> Unit
    ): AnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = settingsRepository.getApiKey()
            ?: throw IllegalStateException("No API key configured")
        val files = if (analyzeAll) {
            fileRepository.getAllFilesList().filter { it.status != FileStatus.UNSUPPORTED }
        } else {
            fileRepository.getFilesByStatuses(listOf(FileStatus.NEVER, FileStatus.STALE))
        }
        var succeeded = 0; var failed = 0; var skipped = 0; var retryDelayMs = 300L

        files.forEachIndexed { index, file ->
            ensureActive()
            onProgress(AnalysisProgress(index + 1, files.size, file.name, succeeded, failed))
            try {
                val text = extractText(file)
                if (text.isNullOrBlank() || text.length < 10) {
                    fileRepository.updateFileFailed(file.id, FileStatus.UNSUPPORTED, "Text extraction failed or content too short")
                    skipped++; return@forEachIndexed
                }
                delay(retryDelayMs)
                val rawResponse = retryWithBackoff {
                    aiApiProvider.analyzeDocument(apiKey, file.name, file.mimeType, text)
                }
                val parsed = JsonParser.parseAnalysisResponse(rawResponse)
                if (parsed != null) {
                    analysisRepository.insertAnalysis(
                        AnalysisEntity(
                            id = UUID.randomUUID().toString(), fileId = file.id,
                            summary = parsed.summary, topics = JsonParser.toJsonArray(parsed.topics),
                            projectSuggestions = JsonParser.toJsonArray(parsed.projectSuggestions),
                            actionItems = JsonParser.toJsonArray(parsed.actionItems),
                            confidence = parsed.confidence, rawResponse = rawResponse,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    fileRepository.updateFileAnalyzed(file.id, FileStatus.UP_TO_DATE, System.currentTimeMillis())
                    succeeded++; retryDelayMs = 300L
                } else {
                    analysisRepository.insertAnalysis(
                        AnalysisEntity(
                            id = UUID.randomUUID().toString(), fileId = file.id,
                            summary = "Parse error - see raw response", topics = "[]",
                            projectSuggestions = "[]", actionItems = "[]",
                            confidence = 0.0, rawResponse = rawResponse,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    fileRepository.updateFileFailed(file.id, FileStatus.FAILED, "JSON parse error")
                    failed++
                }
            } catch (e: IOException) {
                fileRepository.updateFileFailed(file.id, FileStatus.FAILED, "API error: ${e.message}")
                failed++
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                fileRepository.updateFileFailed(file.id, FileStatus.FAILED, "Error: ${e.message}")
                failed++
            }
        }
        AnalysisResult(files.size, succeeded, failed, skipped)
    }

    suspend fun analyzeSingleFile(fileId: String): Boolean = withContext(Dispatchers.IO) {
        val apiKey = settingsRepository.getApiKey()
            ?: throw IllegalStateException("No API key configured")
        val file = fileRepository.getFileById(fileId)
            ?: throw IllegalStateException("File not found")
        val text = extractText(file)
        if (text.isNullOrBlank() || text.length < 10) {
            fileRepository.updateFileFailed(file.id, FileStatus.UNSUPPORTED, "Text extraction failed or content too short")
            return@withContext false
        }
        val rawResponse = aiApiProvider.analyzeDocument(apiKey, file.name, file.mimeType, text)
        val parsed = JsonParser.parseAnalysisResponse(rawResponse)
        if (parsed != null) {
            analysisRepository.insertAnalysis(
                AnalysisEntity(
                    id = UUID.randomUUID().toString(), fileId = file.id,
                    summary = parsed.summary, topics = JsonParser.toJsonArray(parsed.topics),
                    projectSuggestions = JsonParser.toJsonArray(parsed.projectSuggestions),
                    actionItems = JsonParser.toJsonArray(parsed.actionItems),
                    confidence = parsed.confidence, rawResponse = rawResponse,
                    createdAt = System.currentTimeMillis()
                )
            )
            fileRepository.updateFileAnalyzed(file.id, FileStatus.UP_TO_DATE, System.currentTimeMillis())
            true
        } else {
            fileRepository.updateFileFailed(file.id, FileStatus.FAILED, "JSON parse error")
            false
        }
    }

    private fun extractText(file: FileEntity): String? {
        val uri = Uri.parse(file.documentUri)
        return when {
            file.mimeType.startsWith("text/") || file.name.endsWith(".txt") || file.name.endsWith(".md") ->
                textExtractor.extractText(uri)
            file.mimeType == "application/pdf" || file.name.endsWith(".pdf") ->
                context.contentResolver.openInputStream(uri)?.use { pdfTextExtractor.extractText(it) }
            else -> null
        }
    }

    private fun estimateChars(file: FileEntity): Long = when {
        file.mimeType.startsWith("text/") -> file.size
        file.mimeType == "application/pdf" -> (file.size * 0.6).toLong()
        else -> file.size
    }.coerceAtMost(TextExtractor.MAX_CHARS.toLong())

    private suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        repeat(maxRetries - 1) {
            try {
                return block()
            } catch (e: IOException) {
                val msg = e.message ?: ""
                if (msg.contains("429") || msg.matches(Regex(".*\\b5\\d{2}\\b.*"))) {
                    delay(currentDelay); currentDelay *= 2
                } else throw e
            }
        }
        return block()
    }
}
