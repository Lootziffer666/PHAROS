package com.flow.pharos.core.truth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ProvenanceLevel] semantics.
 *
 * These verify that the trust hierarchy is correctly ordered and that
 * grounded vs. inferred classification works as expected.
 */
class ProvenanceLevelTest {

    @Test
    fun `SOURCE is the most trustworthy level`() {
        assertEquals(0, ProvenanceLevel.SOURCE.ordinal)
    }

    @Test
    fun `HYPOTHESIS is the least trustworthy level`() {
        assertEquals(ProvenanceLevel.entries.size - 1, ProvenanceLevel.HYPOTHESIS.ordinal)
    }

    @Test
    fun `trust levels are ordered from most to least trustworthy`() {
        assertTrue(ProvenanceLevel.SOURCE.ordinal < ProvenanceLevel.EXTRACTION.ordinal)
        assertTrue(ProvenanceLevel.EXTRACTION.ordinal < ProvenanceLevel.DERIVATION.ordinal)
        assertTrue(ProvenanceLevel.DERIVATION.ordinal < ProvenanceLevel.HYPOTHESIS.ordinal)
    }

    @Test
    fun `SOURCE is grounded`() {
        assertTrue(ProvenanceLevel.SOURCE.isGrounded)
        assertFalse(ProvenanceLevel.SOURCE.isInferred)
    }

    @Test
    fun `EXTRACTION is grounded`() {
        assertTrue(ProvenanceLevel.EXTRACTION.isGrounded)
        assertFalse(ProvenanceLevel.EXTRACTION.isInferred)
    }

    @Test
    fun `DERIVATION is inferred`() {
        assertFalse(ProvenanceLevel.DERIVATION.isGrounded)
        assertTrue(ProvenanceLevel.DERIVATION.isInferred)
    }

    @Test
    fun `HYPOTHESIS is inferred`() {
        assertFalse(ProvenanceLevel.HYPOTHESIS.isGrounded)
        assertTrue(ProvenanceLevel.HYPOTHESIS.isInferred)
    }

    @Test
    fun `grounded and inferred are mutually exclusive for all levels`() {
        for (level in ProvenanceLevel.entries) {
            assertTrue(
                "Level $level must be either grounded or inferred, not both or neither",
                level.isGrounded xor level.isInferred
            )
        }
    }

    @Test
    fun `exactly four provenance levels exist`() {
        assertEquals(4, ProvenanceLevel.entries.size)
    }
}
