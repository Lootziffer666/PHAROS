package com.flow.pharos.core.truth

/**
 * Semantic layer indicating how a piece of information was obtained.
 *
 * Ordered from most trustworthy (direct source material) to least
 * trustworthy (hypothesis / speculation). The ordinal can be used for
 * comparisons: lower ordinal = stronger provenance.
 *
 * These layers are central to the Pharos lighthouse model:
 * - Pharos must never present synthesis as fact.
 * - Every important statement must preserve provenance.
 * - Uncertainty must be explicit.
 */
enum class ProvenanceLevel {
    /** Raw, unprocessed source material (e.g. original file, chat log, repo output). */
    SOURCE,

    /** Information extracted from a source via deterministic means (e.g. parsing, OCR, regex). */
    EXTRACTION,

    /** Information derived by combining multiple extractions (e.g. topic clustering, cross-referencing). */
    DERIVATION,

    /** Speculative or AI-generated claim that may or may not be accurate. */
    HYPOTHESIS;

    /** True when this level represents user-verifiable source material. */
    val isGrounded: Boolean
        get() = this == SOURCE || this == EXTRACTION

    /** True when this level involves inference or speculation. */
    val isInferred: Boolean
        get() = this == DERIVATION || this == HYPOTHESIS
}
