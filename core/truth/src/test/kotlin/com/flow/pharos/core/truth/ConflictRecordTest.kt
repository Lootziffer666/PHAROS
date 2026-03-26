package com.flow.pharos.core.truth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ConflictRecord] semantics.
 *
 * These verify that conflict detection, representation, and resolution
 * work correctly — Pharos must surface conflicts, not hide them.
 */
class ConflictRecordTest {

    @Test
    fun `conflict requires at least two claims`() {
        val conflict = ConflictRecord(
            id = "c-1",
            claimIds = listOf("claim-1", "claim-2"),
            summary = "Two sources disagree on project deadline"
        )
        assertEquals(2, conflict.claimIds.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `conflict with fewer than two claims throws`() {
        ConflictRecord(
            id = "c-1",
            claimIds = listOf("claim-1"),
            summary = "Single claim cannot be a conflict"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `conflict with empty claims throws`() {
        ConflictRecord(
            id = "c-1",
            claimIds = emptyList(),
            summary = "No claims"
        )
    }

    @Test
    fun `new conflict is unresolved by default`() {
        val conflict = ConflictRecord(
            id = "c-1",
            claimIds = listOf("claim-1", "claim-2"),
            summary = "Disagreement"
        )
        assertTrue(conflict.isOpen)
        assertTrue(conflict.resolution is ConflictRecord.Resolution.Unresolved)
    }

    @Test
    fun `user-resolved conflict is no longer open`() {
        val conflict = ConflictRecord(
            id = "c-1",
            claimIds = listOf("claim-1", "claim-2"),
            summary = "Disagreement",
            resolution = ConflictRecord.Resolution.UserResolved(
                chosenClaimId = "claim-1",
                reason = "Newer source material",
                resolvedAt = 2000L
            )
        )
        assertFalse(conflict.isOpen)
    }

    @Test
    fun `superseded-by-source conflict is no longer open`() {
        val conflict = ConflictRecord(
            id = "c-1",
            claimIds = listOf("claim-1", "claim-2"),
            summary = "Disagreement",
            resolution = ConflictRecord.Resolution.SupersededBySource(
                supersedingSourceId = "source-3",
                resolvedAt = 3000L
            )
        )
        assertFalse(conflict.isOpen)
    }

    @Test
    fun `conflict supports three or more claims`() {
        val conflict = ConflictRecord(
            id = "c-1",
            claimIds = listOf("claim-1", "claim-2", "claim-3"),
            summary = "Three-way disagreement"
        )
        assertEquals(3, conflict.claimIds.size)
        assertTrue(conflict.isOpen)
    }
}
