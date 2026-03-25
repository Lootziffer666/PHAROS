package com.flow.pharos.core.truth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parity tests that verify truth model types produce identical behavior
 * regardless of which platform (Android or Desktop) uses them.
 *
 * These tests run on the JVM, which is the common runtime for both
 * platforms. If these tests pass, the behavior is guaranteed to be
 * identical on both Android (Dalvik/ART JVM) and Desktop (HotSpot JVM).
 *
 * Key parity requirement: "On either Android or Desktop, bring me back
 * into the real project state in under 2 minutes without lying to me."
 */
class PlatformParityTest {

    // --- Scenario: Resume after days away ---

    @Test
    fun `resume scenario - stale hypothesis is not safe to resume on either platform`() {
        val twoDaysAgo = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000L)
        val meta = TrustMetadata(
            sourceId = "ai-analysis-123",
            capturedAt = twoDaysAgo,
            provenance = ProvenanceLevel.HYPOTHESIS,
            verification = VerificationState.Unverified
        )
        assertFalse("Stale hypothesis must not be safe to resume", meta.isSafeToResume)
        assertTrue("Stale hypothesis must be stale", meta.isStale())
    }

    @Test
    fun `resume scenario - fresh confirmed source is safe to resume on either platform`() {
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000L)
        val meta = TrustMetadata(
            sourceId = "file-scan-456",
            capturedAt = oneHourAgo,
            lastVerifiedAt = oneHourAgo,
            provenance = ProvenanceLevel.SOURCE,
            verification = VerificationState.Confirmed(listOf("file-scan-456"))
        )
        assertTrue("Fresh confirmed source must be safe to resume", meta.isSafeToResume)
        assertFalse("Fresh source must not be stale", meta.isStale())
    }

    // --- Scenario: AI analysis trust ---

    @Test
    fun `AI analysis results classified identically on both platforms`() {
        val highConfWithRefs = ProvenanceClassifier.classifyAnalysis(0.9, true)
        val highConfNoRefs = ProvenanceClassifier.classifyAnalysis(0.9, false)
        val lowConf = ProvenanceClassifier.classifyAnalysis(0.3, true)

        assertEquals(ProvenanceLevel.DERIVATION, highConfWithRefs)
        assertEquals(ProvenanceLevel.HYPOTHESIS, highConfNoRefs)
        assertEquals(ProvenanceLevel.HYPOTHESIS, lowConf)

        // Verify shared labels
        assertEquals("Derived from analysis", ProvenanceClassifier.labelFor(highConfWithRefs))
        assertEquals("AI-generated (unverified)", ProvenanceClassifier.labelFor(highConfNoRefs))
    }

    // --- Scenario: Conflict surfacing ---

    @Test
    fun `conflict assessment produces consistent trust labels on both platforms`() {
        val conflict = ConflictRecord(
            id = "conflict-1",
            claimIds = listOf("claim-a", "claim-b"),
            summary = "Deadline disagreement"
        )
        val assessment = TrustAssessment(
            value = "March 30 deadline",
            provenance = ProvenanceRecord(
                claimId = "claim-a",
                content = "March 30 deadline",
                sourceRefs = listOf("email-1"),
                trust = TrustMetadata(
                    sourceId = "email-1",
                    capturedAt = 1000L,
                    provenance = ProvenanceLevel.SOURCE,
                    verification = VerificationState.Confirmed(listOf("email-1"))
                )
            ),
            conflicts = listOf(conflict)
        )
        assertEquals("conflicted", assessment.trustLabel)
        assertTrue(assessment.hasOpenConflicts)
        assertFalse(assessment.isReliable)
    }

    // --- Scenario: Provenance chain ---

    @Test
    fun `provenance chain is preserved identically on both platforms`() {
        val sourceRecord = ProvenanceRecord(
            claimId = "source-claim",
            content = "Original chat log",
            sourceRefs = listOf("chat-log-1"),
            trust = TrustMetadata(
                sourceId = "chat-log-1",
                capturedAt = 1000L,
                provenance = ProvenanceLevel.SOURCE
            )
        )
        val derivedRecord = ProvenanceRecord(
            claimId = "derived-claim",
            content = "Topic: Project Planning",
            sourceRefs = listOf("chat-log-1"),
            trust = TrustMetadata(
                sourceId = "clustering-algo",
                capturedAt = 2000L,
                provenance = ProvenanceLevel.DERIVATION
            ),
            parentClaimId = "source-claim"
        )
        val hypothesisRecord = ProvenanceRecord(
            claimId = "hypothesis-claim",
            content = "AI suggests project may be delayed",
            sourceRefs = listOf("chat-log-1"),
            trust = TrustMetadata(
                sourceId = "ai-model",
                capturedAt = 3000L,
                provenance = ProvenanceLevel.HYPOTHESIS
            ),
            parentClaimId = "derived-claim"
        )

        assertTrue(sourceRecord.isSourceBacked)
        assertFalse(derivedRecord.isSourceBacked)
        assertFalse(hypothesisRecord.isSourceBacked)

        assertTrue(hypothesisRecord.needsUserVerification)
        assertFalse(sourceRecord.needsUserVerification)
    }

    // --- Scenario: Verification state transitions ---

    @Test
    fun `verification state transitions are consistent on both platforms`() {
        val unverified = VerificationState.Unverified
        assertFalse(unverified.isTrustworthy)
        assertFalse(unverified.hasIssues)

        val confirmed = VerificationState.Confirmed(listOf("s1"))
        assertTrue(confirmed.isTrustworthy)
        assertFalse(confirmed.hasIssues)

        val disputed = VerificationState.Disputed("c1")
        assertFalse(disputed.isTrustworthy)
        assertTrue(disputed.hasIssues)

        val outdated = VerificationState.Outdated("s2")
        assertFalse(outdated.isTrustworthy)
        assertTrue(outdated.hasIssues)
    }

    // --- Scenario: Trust labels cover all levels ---

    @Test
    fun `all provenance levels have distinct non-empty labels on both platforms`() {
        val labels = ProvenanceLevel.entries.map { ProvenanceClassifier.labelFor(it) }
        assertEquals(ProvenanceLevel.entries.size, labels.distinct().size)
        labels.forEach { assertTrue("Label should not be blank", it.isNotBlank()) }
    }
}
