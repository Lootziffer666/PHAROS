package com.flow.pharos.core.truth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [TrustMetadata] semantics.
 *
 * These verify freshness tracking, staleness detection, and safe-to-resume
 * classification used for project-state recovery on both platforms.
 */
class TrustMetadataTest {

    private val now = 1_000_000_000L
    private val oneHourAgo = now - (60 * 60 * 1000L)
    private val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000L)

    @Test
    fun `recent grounded verified metadata is safe to resume`() {
        val meta = TrustMetadata(
            sourceId = "file-1",
            capturedAt = oneHourAgo,
            lastVerifiedAt = oneHourAgo,
            provenance = ProvenanceLevel.SOURCE,
            verification = VerificationState.Confirmed(listOf("file-1"))
        )
        assertTrue(meta.isSafeToResume)
    }

    @Test
    fun `hypothesis is never safe to resume even when confirmed`() {
        val meta = TrustMetadata(
            sourceId = "ai-output",
            capturedAt = oneHourAgo,
            lastVerifiedAt = oneHourAgo,
            provenance = ProvenanceLevel.HYPOTHESIS,
            verification = VerificationState.Confirmed(listOf("ai-output"))
        )
        assertFalse(meta.isSafeToResume)
    }

    @Test
    fun `unverified source is not safe to resume`() {
        val meta = TrustMetadata(
            sourceId = "file-1",
            capturedAt = oneHourAgo,
            provenance = ProvenanceLevel.SOURCE,
            verification = VerificationState.Unverified
        )
        assertFalse(meta.isSafeToResume)
    }

    @Test
    fun `data captured two days ago without re-verification is stale`() {
        val meta = TrustMetadata(
            sourceId = "file-1",
            capturedAt = twoDaysAgo,
            provenance = ProvenanceLevel.SOURCE
        )
        assertTrue(meta.isStale(now))
    }

    @Test
    fun `data captured one hour ago is not stale`() {
        val meta = TrustMetadata(
            sourceId = "file-1",
            capturedAt = oneHourAgo,
            provenance = ProvenanceLevel.SOURCE
        )
        assertFalse(meta.isStale(now))
    }

    @Test
    fun `data with recent verification is not stale even if captured long ago`() {
        val meta = TrustMetadata(
            sourceId = "file-1",
            capturedAt = twoDaysAgo,
            lastVerifiedAt = oneHourAgo,
            provenance = ProvenanceLevel.SOURCE
        )
        assertFalse(meta.isStale(now))
    }

    @Test
    fun `age millis computed correctly`() {
        val meta = TrustMetadata(
            sourceId = "file-1",
            capturedAt = oneHourAgo,
            provenance = ProvenanceLevel.SOURCE
        )
        val age = meta.ageMillis(now)
        assertTrue(age > 0)
        assertTrue(age <= 60 * 60 * 1000L)
    }

    @Test
    fun `disputed source metadata is not safe to resume`() {
        val meta = TrustMetadata(
            sourceId = "file-1",
            capturedAt = oneHourAgo,
            provenance = ProvenanceLevel.SOURCE,
            verification = VerificationState.Disputed("conflict-1")
        )
        assertFalse(meta.isSafeToResume)
    }

    @Test
    fun `outdated extraction metadata is not safe to resume`() {
        val meta = TrustMetadata(
            sourceId = "file-1",
            capturedAt = oneHourAgo,
            provenance = ProvenanceLevel.EXTRACTION,
            verification = VerificationState.Outdated("newer-source")
        )
        assertFalse(meta.isSafeToResume)
    }
}
