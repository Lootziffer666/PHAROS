package com.flow.pharos.core.truth

/**
 * A trust assessment that wraps a value with its full provenance context.
 *
 * This is the primary wrapper used by Pharos to present information to users.
 * It ensures that every piece of data carries explicit trust semantics so
 * the UI can render appropriate trust indicators on both Android and Desktop.
 *
 * The key user outcome: "Bring me back into the real project state in under
 * 2 minutes without lying to me" — on either platform.
 *
 * @param T The type of the assessed value.
 * @property value       The actual data being assessed.
 * @property provenance  The provenance record for this data.
 * @property conflicts   Any known conflicts involving this data.
 */
data class TrustAssessment<T>(
    val value: T,
    val provenance: ProvenanceRecord,
    val conflicts: List<ConflictRecord> = emptyList()
) {
    /** True if this assessment has no known issues and comes from grounded sources. */
    val isReliable: Boolean
        get() = provenance.isSourceBacked
            && conflicts.none { it.isOpen }
            && !provenance.trust.verification.hasIssues

    /** True if there are unresolved conflicts affecting this assessment. */
    val hasOpenConflicts: Boolean
        get() = conflicts.any { it.isOpen }

    /** True if this value is inferred and should be presented with uncertainty markers. */
    val isUncertain: Boolean
        get() = provenance.trust.provenance.isInferred

    /**
     * Returns a human-readable trust label suitable for UI display.
     *
     * Both Android and Desktop should use this to present consistent
     * trust information to the user.
     */
    val trustLabel: String
        get() = when {
            hasOpenConflicts -> "conflicted"
            provenance.trust.verification.hasIssues -> "issues"
            provenance.trust.provenance == ProvenanceLevel.SOURCE -> "source"
            provenance.trust.provenance == ProvenanceLevel.EXTRACTION -> "extracted"
            provenance.trust.provenance == ProvenanceLevel.DERIVATION -> "derived"
            provenance.trust.provenance == ProvenanceLevel.HYPOTHESIS -> "hypothesis"
            else -> "unknown"
        }
}
