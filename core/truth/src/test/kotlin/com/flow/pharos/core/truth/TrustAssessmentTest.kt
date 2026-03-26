package com.flow.pharos.core.truth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [TrustAssessment] semantics.
 *
 * These are the core "lighthouse tests" — they verify that Pharos
 * correctly classifies information for trustworthy presentation on
 * both Android and Desktop.
 *
 * Key principle: "Incomplete but honest is better than smooth but wrong."
 */
class TrustAssessmentTest {

    private fun sourceRecord(claimId: String = "c-1", content: String = "test") =
        ProvenanceRecord(
            claimId = claimId,
            content = content,
            sourceRefs = listOf("source-1"),
            trust = TrustMetadata(
                sourceId = "source-1",
                capturedAt = 1000L,
                provenance = ProvenanceLevel.SOURCE,
                verification = VerificationState.Confirmed(listOf("source-1"))
            )
        )

    private fun hypothesisRecord(claimId: String = "c-2", content: String = "maybe") =
        ProvenanceRecord(
            claimId = claimId,
            content = content,
            sourceRefs = listOf("ai-output"),
            trust = TrustMetadata(
                sourceId = "ai-output",
                capturedAt = 1000L,
                provenance = ProvenanceLevel.HYPOTHESIS,
                verification = VerificationState.Unverified
            )
        )

    // --- Reliability tests ---

    @Test
    fun `source-backed confirmed assessment with no conflicts is reliable`() {
        val assessment = TrustAssessment(
            value = "Project deadline is March 30",
            provenance = sourceRecord()
        )
        assertTrue(assessment.isReliable)
        assertFalse(assessment.isUncertain)
        assertFalse(assessment.hasOpenConflicts)
    }

    @Test
    fun `hypothesis assessment is uncertain`() {
        val assessment = TrustAssessment(
            value = "The project might be delayed",
            provenance = hypothesisRecord()
        )
        assertFalse(assessment.isReliable)
        assertTrue(assessment.isUncertain)
    }

    @Test
    fun `assessment with open conflict is not reliable`() {
        val conflict = ConflictRecord(
            id = "conflict-1",
            claimIds = listOf("c-1", "c-2"),
            summary = "Two sources disagree"
        )
        val assessment = TrustAssessment(
            value = "Disputed deadline",
            provenance = sourceRecord(),
            conflicts = listOf(conflict)
        )
        assertFalse(assessment.isReliable)
        assertTrue(assessment.hasOpenConflicts)
    }

    @Test
    fun `assessment with resolved conflict is reliable`() {
        val resolvedConflict = ConflictRecord(
            id = "conflict-1",
            claimIds = listOf("c-1", "c-2"),
            summary = "Two sources disagreed",
            resolution = ConflictRecord.Resolution.UserResolved(
                chosenClaimId = "c-1",
                reason = "Newer email"
            )
        )
        val assessment = TrustAssessment(
            value = "Confirmed deadline",
            provenance = sourceRecord(),
            conflicts = listOf(resolvedConflict)
        )
        assertTrue(assessment.isReliable)
        assertFalse(assessment.hasOpenConflicts)
    }

    // --- Trust label tests ---

    @Test
    fun `source assessment has source label`() {
        val assessment = TrustAssessment(
            value = "Raw data",
            provenance = sourceRecord()
        )
        assertEquals("source", assessment.trustLabel)
    }

    @Test
    fun `hypothesis assessment has hypothesis label`() {
        val assessment = TrustAssessment(
            value = "Guess",
            provenance = hypothesisRecord()
        )
        assertEquals("hypothesis", assessment.trustLabel)
    }

    @Test
    fun `conflicted assessment has conflicted label regardless of provenance`() {
        val conflict = ConflictRecord(
            id = "conflict-1",
            claimIds = listOf("c-1", "c-2"),
            summary = "Conflict"
        )
        val assessment = TrustAssessment(
            value = "Data",
            provenance = sourceRecord(),
            conflicts = listOf(conflict)
        )
        assertEquals("conflicted", assessment.trustLabel)
    }

    @Test
    fun `disputed verification produces issues label`() {
        val record = ProvenanceRecord(
            claimId = "c-1",
            content = "test",
            sourceRefs = listOf("source-1"),
            trust = TrustMetadata(
                sourceId = "source-1",
                capturedAt = 1000L,
                provenance = ProvenanceLevel.SOURCE,
                verification = VerificationState.Disputed("conflict-1")
            )
        )
        val assessment = TrustAssessment(value = "Data", provenance = record)
        assertEquals("issues", assessment.trustLabel)
    }

    @Test
    fun `extracted data has extracted label`() {
        val record = ProvenanceRecord(
            claimId = "c-1",
            content = "parsed text",
            sourceRefs = listOf("file-1"),
            trust = TrustMetadata(
                sourceId = "file-1",
                capturedAt = 1000L,
                provenance = ProvenanceLevel.EXTRACTION,
                verification = VerificationState.Confirmed(listOf("file-1"))
            )
        )
        val assessment = TrustAssessment(value = "Parsed", provenance = record)
        assertEquals("extracted", assessment.trustLabel)
    }

    @Test
    fun `derived data has derived label`() {
        val record = ProvenanceRecord(
            claimId = "c-1",
            content = "clustered topic",
            sourceRefs = listOf("file-1", "file-2"),
            trust = TrustMetadata(
                sourceId = "algo",
                capturedAt = 1000L,
                provenance = ProvenanceLevel.DERIVATION
            )
        )
        val assessment = TrustAssessment(value = "Cluster", provenance = record)
        assertEquals("derived", assessment.trustLabel)
    }

    // --- Lighthouse boundary tests ---

    @Test
    fun `never present synthesis as fact - hypothesis is always uncertain`() {
        for (verification in listOf(
            VerificationState.Unverified,
            VerificationState.Confirmed(listOf("anything"))
        )) {
            val record = ProvenanceRecord(
                claimId = "c-1",
                content = "AI generated",
                sourceRefs = listOf("ai"),
                trust = TrustMetadata(
                    sourceId = "ai",
                    capturedAt = 1000L,
                    provenance = ProvenanceLevel.HYPOTHESIS,
                    verification = verification
                )
            )
            val assessment = TrustAssessment(value = "Data", provenance = record)
            assertTrue(
                "Hypothesis must always be marked uncertain, even when confirmed",
                assessment.isUncertain
            )
        }
    }

    @Test
    fun `conflicts must be surfaced not resolved - open conflict blocks reliability`() {
        val conflict = ConflictRecord(
            id = "conflict-1",
            claimIds = listOf("c-1", "c-2"),
            summary = "Disagreement"
        )
        val assessment = TrustAssessment(
            value = "Data",
            provenance = sourceRecord(),
            conflicts = listOf(conflict)
        )
        assertTrue(assessment.hasOpenConflicts)
        assertFalse(assessment.isReliable)
    }
}
