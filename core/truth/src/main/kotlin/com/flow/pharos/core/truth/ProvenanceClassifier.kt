package com.flow.pharos.core.truth

/**
 * Classifies content into provenance levels based on its origin and characteristics.
 *
 * This classifier provides the bridge between raw data (like AI analysis results)
 * and the Pharos truth model. It ensures that AI-generated content is never
 * presented as source fact.
 *
 * Both Android and Desktop use this shared logic to classify content identically.
 */
object ProvenanceClassifier {

    /**
     * Classifies an AI analysis result based on its confidence score.
     *
     * AI-generated analysis is at best a DERIVATION (high confidence
     * with supporting evidence) and typically a HYPOTHESIS (speculative).
     * It is never SOURCE or EXTRACTION since it involves inference.
     *
     * @param confidence The AI's self-reported confidence (0.0 to 1.0).
     * @param hasSourceRefs Whether the analysis references specific source material.
     * @return The appropriate provenance level for this analysis result.
     */
    fun classifyAnalysis(confidence: Double, hasSourceRefs: Boolean): ProvenanceLevel {
        return when {
            confidence >= 0.8 && hasSourceRefs -> ProvenanceLevel.DERIVATION
            else -> ProvenanceLevel.HYPOTHESIS
        }
    }

    /**
     * Classifies a file hash comparison result.
     *
     * File hashes are deterministic extractions from source material.
     * The hash itself is EXTRACTION level; the source file is SOURCE level.
     */
    fun classifyFileHash(): ProvenanceLevel = ProvenanceLevel.EXTRACTION

    /**
     * Classifies a raw ingested file.
     *
     * Original unmodified files are source material.
     */
    fun classifyRawFile(): ProvenanceLevel = ProvenanceLevel.SOURCE

    /**
     * Classifies a topic clustering result.
     *
     * Topic clustering combines multiple extractions to derive structure.
     * This is always DERIVATION since it involves algorithmic inference.
     */
    fun classifyTopicClustering(): ProvenanceLevel = ProvenanceLevel.DERIVATION

    /**
     * Classifies an AI-generated summary.
     *
     * AI summaries are always HYPOTHESIS — they involve model-generated
     * interpretation that may or may not be accurate. Even high-confidence
     * summaries must be labeled as hypothesis per Pharos trust principles.
     */
    fun classifyAiSummary(): ProvenanceLevel = ProvenanceLevel.HYPOTHESIS

    /**
     * Classifies a project suggestion from AI analysis.
     *
     * Project suggestions are speculative by nature and are always HYPOTHESIS.
     */
    fun classifyProjectSuggestion(): ProvenanceLevel = ProvenanceLevel.HYPOTHESIS

    /**
     * Returns a human-readable label for use in UI display.
     * Both platforms must use this shared labeling for consistency.
     */
    fun labelFor(level: ProvenanceLevel): String = when (level) {
        ProvenanceLevel.SOURCE -> "Source material"
        ProvenanceLevel.EXTRACTION -> "Extracted from source"
        ProvenanceLevel.DERIVATION -> "Derived from analysis"
        ProvenanceLevel.HYPOTHESIS -> "AI-generated (unverified)"
    }
}
