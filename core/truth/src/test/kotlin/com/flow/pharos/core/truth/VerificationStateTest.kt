package com.flow.pharos.core.truth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [VerificationState] semantics.
 *
 * These verify that trust and issue classification is correct for
 * all verification states.
 */
class VerificationStateTest {

    @Test
    fun `Unverified is not trustworthy`() {
        val state = VerificationState.Unverified
        assertFalse(state.isTrustworthy)
        assertFalse(state.hasIssues)
    }

    @Test
    fun `Confirmed is trustworthy`() {
        val state = VerificationState.Confirmed(
            supportingSourceIds = listOf("source-1"),
            confirmedAt = 1000L
        )
        assertTrue(state.isTrustworthy)
        assertFalse(state.hasIssues)
    }

    @Test
    fun `Disputed has issues`() {
        val state = VerificationState.Disputed(
            conflictId = "conflict-1",
            disputedAt = 1000L
        )
        assertFalse(state.isTrustworthy)
        assertTrue(state.hasIssues)
    }

    @Test
    fun `Outdated has issues`() {
        val state = VerificationState.Outdated(
            supersededBy = "newer-source",
            outdatedSince = 1000L
        )
        assertFalse(state.isTrustworthy)
        assertTrue(state.hasIssues)
    }

    @Test
    fun `Confirmed with multiple sources`() {
        val state = VerificationState.Confirmed(
            supportingSourceIds = listOf("source-1", "source-2", "source-3")
        )
        assertTrue(state.isTrustworthy)
        assertFalse(state.hasIssues)
    }
}
