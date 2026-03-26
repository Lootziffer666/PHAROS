package com.flow.pharos.core.truth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ProvenanceRecord] semantics.
 *
 * These verify that claims are correctly classified as source-backed
 * or needing user verification.
 */
class ProvenanceRecordTest {

    @Test
    fun `source-backed record with source refs is marked as source-backed`() {
        val record = ProvenanceRecord(
            claimId = "claim-1",
            content = "Project deadline is March 30",
            sourceRefs = listOf("chat-log-42"),
            trust = TrustMetadata(
                sourceId = "chat-log-42",
                capturedAt = 1000L,
                provenance = ProvenanceLevel.SOURCE
            )
        )
        assertTrue(record.isSourceBacked)
    }

    @Test
    fun `hypothesis without sources is not source-backed`() {
        val record = ProvenanceRecord(
            claimId = "claim-2",
            content = "The project is probably delayed",
            sourceRefs = emptyList(),
            trust = TrustMetadata(
                sourceId = "ai-analysis",
                capturedAt = 1000L,
                provenance = ProvenanceLevel.HYPOTHESIS
            )
        )
        assertFalse(record.isSourceBacked)
    }

    @Test
    fun `derivation with source refs is not source-backed`() {
        val record = ProvenanceRecord(
            claimId = "claim-3",
            content = "Topic cluster: Android Development",
            sourceRefs = listOf("file-1", "file-2"),
            trust = TrustMetadata(
                sourceId = "clustering-algo",
                capturedAt = 1000L,
                provenance = ProvenanceLevel.DERIVATION
            )
        )
        assertFalse(record.isSourceBacked)
    }

    @Test
    fun `unverified hypothesis needs user verification`() {
        val record = ProvenanceRecord(
            claimId = "claim-4",
            content = "AI suggests this is about authentication",
            sourceRefs = listOf("file-1"),
            trust = TrustMetadata(
                sourceId = "ai-output",
                capturedAt = 1000L,
                provenance = ProvenanceLevel.HYPOTHESIS,
                verification = VerificationState.Unverified
            )
        )
        assertTrue(record.needsUserVerification)
    }

    @Test
    fun `confirmed extraction does not need user verification`() {
        val record = ProvenanceRecord(
            claimId = "claim-5",
            content = "File contains 3 TODO items",
            sourceRefs = listOf("file-1"),
            trust = TrustMetadata(
                sourceId = "file-1",
                capturedAt = 1000L,
                provenance = ProvenanceLevel.EXTRACTION,
                verification = VerificationState.Confirmed(listOf("file-1"))
            )
        )
        assertFalse(record.needsUserVerification)
    }

    @Test
    fun `confirmed hypothesis does not need user verification`() {
        val record = ProvenanceRecord(
            claimId = "claim-6",
            content = "AI-generated summary confirmed by user",
            sourceRefs = listOf("file-1"),
            trust = TrustMetadata(
                sourceId = "ai-output",
                capturedAt = 1000L,
                provenance = ProvenanceLevel.HYPOTHESIS,
                verification = VerificationState.Confirmed(listOf("user-review"))
            )
        )
        assertFalse(record.needsUserVerification)
    }

    @Test
    fun `record with parent claim id preserves derivation chain`() {
        val record = ProvenanceRecord(
            claimId = "claim-7",
            content = "Derived from parent",
            sourceRefs = listOf("file-1"),
            trust = TrustMetadata(
                sourceId = "file-1",
                capturedAt = 1000L,
                provenance = ProvenanceLevel.DERIVATION
            ),
            parentClaimId = "claim-1"
        )
        assertTrue(record.parentClaimId == "claim-1")
    }
}
