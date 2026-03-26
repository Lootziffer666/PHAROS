package com.flow.pharos.core.truth

/**
 * A provenance record that links a specific claim or piece of content
 * back to its source evidence chain.
 *
 * This is the core data structure for Pharos' provenance-first model.
 * Every important statement in the system should have a ProvenanceRecord
 * so users can trace back to the original source.
 *
 * @property claimId     Unique identifier for this claim.
 * @property content     The textual content of the claim.
 * @property sourceRefs  References to the source material (file IDs, URIs, etc.).
 * @property trust       Trust metadata including provenance level and verification state.
 * @property parentClaimId If this claim was derived from another claim, the parent ID.
 */
data class ProvenanceRecord(
    val claimId: String,
    val content: String,
    val sourceRefs: List<String>,
    val trust: TrustMetadata,
    val parentClaimId: String? = null
) {
    /** True if this claim is directly supported by source material. */
    val isSourceBacked: Boolean
        get() = sourceRefs.isNotEmpty() && trust.provenance.isGrounded

    /** True if this claim is inferred and may need user verification. */
    val needsUserVerification: Boolean
        get() = trust.provenance.isInferred && !trust.verification.isTrustworthy
}
