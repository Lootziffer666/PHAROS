package com.flow.pharos.core.truth

/**
 * Metadata describing the freshness and trustworthiness context of a claim.
 *
 * Pharos uses this to show users what they can safely rely on and what
 * may be outdated or uncertain.
 *
 * @property sourceId      ID of the originating source (file, chat, repo, etc.).
 * @property capturedAt    Timestamp when the information was first captured.
 * @property lastVerifiedAt Timestamp of the last verification check, or null if never verified.
 * @property provenance    Semantic layer of this information.
 * @property verification  Current verification state.
 * @property deviceId      Which device produced or last modified this metadata.
 */
data class TrustMetadata(
    val sourceId: String,
    val capturedAt: Long,
    val lastVerifiedAt: Long? = null,
    val provenance: ProvenanceLevel,
    val verification: VerificationState = VerificationState.Unverified,
    val deviceId: String? = null
) {
    /**
     * Returns the age in milliseconds since the information was captured.
     */
    fun ageMillis(now: Long = System.currentTimeMillis()): Long = now - capturedAt

    /**
     * Returns true if the information has not been verified within the given
     * [thresholdMillis] (default: 24 hours).
     */
    fun isStale(
        now: Long = System.currentTimeMillis(),
        thresholdMillis: Long = 24 * 60 * 60 * 1000L
    ): Boolean {
        val lastCheck = lastVerifiedAt ?: capturedAt
        return (now - lastCheck) > thresholdMillis
    }

    /**
     * Returns true if a user can safely rely on this information for
     * project-state recovery without additional verification.
     */
    val isSafeToResume: Boolean
        get() = provenance.isGrounded && verification.isTrustworthy
}
