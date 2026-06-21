package com.flow.pharos.usecase

import android.content.Context
import android.net.Uri
import com.flow.pharos.core.model.entity.FileEntity
import com.flow.pharos.core.storage.repository.AnalysisRepository
import com.flow.pharos.core.storage.repository.ClaimRepository
import com.flow.pharos.core.storage.repository.FileRepository
import com.flow.pharos.core.storage.repository.SettingsRepository
import com.flow.pharos.util.PdfTextExtractor
import com.flow.pharos.util.TextExtractor

data class PipelineProgress(
    val stage: String,
    val detail: String = ""
)

data class PipelineResult(
    val claimsExtracted: Int,
    val autoMerged: Int,
    val conflicts: Int,
    val errors: Int
)

class SsotPipelineUseCase(
    private val fileRepository: FileRepository,
    private val analysisRepository: AnalysisRepository,
    private val claimRepository: ClaimRepository,
    private val claimExtractionUseCase: ClaimExtractionUseCase,
    private val conflictDetectionUseCase: ConflictDetectionUseCase,
    private val settingsRepository: SettingsRepository,
    private val textExtractor: TextExtractor,
    private val pdfTextExtractor: PdfTextExtractor,
    private val context: Context
) {
    companion object {
        private const val DEFAULT_MODEL = "gemma3"
    }

    suspend fun runPipeline(
        fileId: String,
        onProgress: (suspend (PipelineProgress) -> Unit)? = null
    ): Result<PipelineResult> {
        return try {
            onProgress?.invoke(PipelineProgress("INIT", "Loading file information"))

            val file = fileRepository.getFileById(fileId)
                ?: return Result.failure(IllegalStateException("File not found: $fileId"))

            onProgress?.invoke(PipelineProgress("ANALYSIS_CHECK", "Checking analysis status"))

            val analysis = analysisRepository.getLatestAnalysisForFile(fileId)
            if (analysis == null) {
                return Result.failure(
                    IllegalStateException("No analysis found for file: ${file.name}. Run analysis first.")
                )
            }

            onProgress?.invoke(PipelineProgress("TEXT_EXTRACTION", "Extracting text from ${file.name}"))

            val text = extractText(file)
            if (text.isNullOrBlank()) {
                return Result.failure(
                    IllegalStateException("Could not extract text from file: ${file.name}")
                )
            }

            val model = resolveModel()

            onProgress?.invoke(PipelineProgress("CLAIM_EXTRACTION", "Extracting atomic claims via LLM"))

            val extractionResult = claimExtractionUseCase.extractClaims(file, text, model)
            val claims = extractionResult.getOrElse { e ->
                return Result.failure(
                    IllegalStateException("Claim extraction failed: ${e.message}")
                )
            }

            if (claims.isEmpty()) {
                return Result.success(PipelineResult(0, 0, 0, 0))
            }

            onProgress?.invoke(
                PipelineProgress("PERSISTING", "Saving ${claims.size} claims to database")
            )

            claimRepository.insertAll(claims)

            onProgress?.invoke(
                PipelineProgress("CONFLICT_DETECTION", "Checking for conflicts with existing SSOT")
            )

            val detectionResult = conflictDetectionUseCase.detectConflicts(claims, model)

            onProgress?.invoke(
                PipelineProgress("COMPLETE", "Pipeline finished: ${claims.size} claims extracted")
            )

            Result.success(
                PipelineResult(
                    claimsExtracted = claims.size,
                    autoMerged = detectionResult.autoMerged,
                    conflicts = detectionResult.conflicts,
                    errors = detectionResult.errors
                )
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    private fun resolveModel(): String {
        // Read the user-configured model from settings; fall back to default if not set
        return settingsRepository.getLlmModel() ?: DEFAULT_MODEL
    }

    private fun extractText(file: FileEntity): String? {
        val uri = Uri.parse(file.documentUri)
        return when {
            file.mimeType.startsWith("text/") ||
                file.name.endsWith(".txt") ||
                file.name.endsWith(".md") ->
                textExtractor.extractText(uri)
            file.mimeType == "application/pdf" || file.name.endsWith(".pdf") ->
                context.contentResolver.openInputStream(uri)?.use {
                    pdfTextExtractor.extractText(it)
                }
            else -> null
        }
    }
}
