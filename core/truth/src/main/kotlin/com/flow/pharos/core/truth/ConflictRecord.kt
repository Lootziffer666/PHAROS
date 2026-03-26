package com.flow.pharos.core.truth

/**
 * Records a conflict between two or more pieces of information.
 *
 * Pharos must surface conflicts rather than prematurely resolving them.
 * Both Android and Desktop must present these identically.
 *
 * @property id        Unique identifier for this conflict.
 * @property claimIds  The IDs of the conflicting claims.
 * @property summary   Human-readable description of the conflict.
 * @property resolution Current resolution state.
 * @property detectedAt Timestamp when the conflict was first detected.
 */
data class ConflictRecord(
    val id: String,
    val claimIds: List<String>,
    val summary: String,
    val resolution: Resolution = Resolution.Unresolved,
    val detectedAt: Long = System.currentTimeMillis()
) {
    init {
        require(claimIds.size >= 2) { "A conflict requires at least two claims" }
    }

    /** Returns true if this conflict has not been resolved. */
    val isOpen: Boolean
        get() = resolution is Resolution.Unresolved

    /**
     * Resolution state of a conflict.
     */
    sealed class Resolution {
        /** Conflict has not been resolved. */
        data object Unresolved : Resolution()

        /** User explicitly chose one claim over the others. */
        data class UserResolved(
            val chosenClaimId: String,
            val reason: String,
            val resolvedAt: Long = System.currentTimeMillis()
        ) : Resolution()

        /** Conflict was resolved by newer information superseding older claims. */
        data class SupersededBySource(
            val supersedingSourceId: String,
            val resolvedAt: Long = System.currentTimeMillis()
        ) : Resolution()
    }
}
