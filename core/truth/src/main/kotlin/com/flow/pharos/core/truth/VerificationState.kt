package com.flow.pharos.core.truth

/**
 * Tracks the verification status of a claim or piece of information.
 *
 * Pharos must surface uncertainty, not hide it. This sealed class ensures
 * every claim carries an explicit verification state that both Android
 * and Desktop can reason about identically.
 */
sealed class VerificationState {

    /** Not yet checked against any other source. */
    data object Unverified : VerificationState()

    /** Confirmed by at least one independent source. */
    data class Confirmed(
        val supportingSourceIds: List<String>,
        val confirmedAt: Long = System.currentTimeMillis()
    ) : VerificationState()

    /** Contradicted by at least one source — conflict must be surfaced. */
    data class Disputed(
        val conflictId: String,
        val disputedAt: Long = System.currentTimeMillis()
    ) : VerificationState()

    /** Was valid but is now superseded by newer information. */
    data class Outdated(
        val supersededBy: String,
        val outdatedSince: Long = System.currentTimeMillis()
    ) : VerificationState()

    /** Returns true if this claim can be safely relied on. */
    val isTrustworthy: Boolean
        get() = this is Confirmed

    /** Returns true if this claim has known problems. */
    val hasIssues: Boolean
        get() = this is Disputed || this is Outdated
}
