package com.flow.pharos.core.truth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ProvenanceClassifier] — verifies that content is classified
 * into the correct provenance level based on its origin.
 *
 * These tests ensure that the Pharos lighthouse principle holds:
 * AI-generated content must never be presented as source fact.
 */
class ProvenanceClassifierTest {

    // --- Analysis classification ---

    @Test
    fun `high confidence analysis with source refs is DERIVATION`() {
        assertEquals(
            ProvenanceLevel.DERIVATION,
            ProvenanceClassifier.classifyAnalysis(confidence = 0.9, hasSourceRefs = true)
        )
    }

    @Test
    fun `high confidence analysis without source refs is HYPOTHESIS`() {
        assertEquals(
            ProvenanceLevel.HYPOTHESIS,
            ProvenanceClassifier.classifyAnalysis(confidence = 0.9, hasSourceRefs = false)
        )
    }

    @Test
    fun `low confidence analysis with source refs is HYPOTHESIS`() {
        assertEquals(
            ProvenanceLevel.HYPOTHESIS,
            ProvenanceClassifier.classifyAnalysis(confidence = 0.3, hasSourceRefs = true)
        )
    }

    @Test
    fun `low confidence analysis without source refs is HYPOTHESIS`() {
        assertEquals(
            ProvenanceLevel.HYPOTHESIS,
            ProvenanceClassifier.classifyAnalysis(confidence = 0.3, hasSourceRefs = false)
        )
    }

    @Test
    fun `borderline confidence of 0_8 with source refs is DERIVATION`() {
        assertEquals(
            ProvenanceLevel.DERIVATION,
            ProvenanceClassifier.classifyAnalysis(confidence = 0.8, hasSourceRefs = true)
        )
    }

    @Test
    fun `analysis is never classified as SOURCE`() {
        for (conf in listOf(0.0, 0.5, 0.8, 0.99, 1.0)) {
            for (refs in listOf(true, false)) {
                val level = ProvenanceClassifier.classifyAnalysis(conf, refs)
                assertTrue(
                    "Analysis at confidence=$conf, hasRefs=$refs should never be SOURCE",
                    level != ProvenanceLevel.SOURCE
                )
            }
        }
    }

    @Test
    fun `analysis is never classified as EXTRACTION`() {
        for (conf in listOf(0.0, 0.5, 0.8, 0.99, 1.0)) {
            for (refs in listOf(true, false)) {
                val level = ProvenanceClassifier.classifyAnalysis(conf, refs)
                assertTrue(
                    "Analysis at confidence=$conf, hasRefs=$refs should never be EXTRACTION",
                    level != ProvenanceLevel.EXTRACTION
                )
            }
        }
    }

    // --- Fixed classifications ---

    @Test
    fun `file hash is EXTRACTION`() {
        assertEquals(ProvenanceLevel.EXTRACTION, ProvenanceClassifier.classifyFileHash())
    }

    @Test
    fun `raw file is SOURCE`() {
        assertEquals(ProvenanceLevel.SOURCE, ProvenanceClassifier.classifyRawFile())
    }

    @Test
    fun `topic clustering is DERIVATION`() {
        assertEquals(ProvenanceLevel.DERIVATION, ProvenanceClassifier.classifyTopicClustering())
    }

    @Test
    fun `AI summary is always HYPOTHESIS`() {
        assertEquals(ProvenanceLevel.HYPOTHESIS, ProvenanceClassifier.classifyAiSummary())
    }

    @Test
    fun `project suggestion is always HYPOTHESIS`() {
        assertEquals(ProvenanceLevel.HYPOTHESIS, ProvenanceClassifier.classifyProjectSuggestion())
    }

    // --- Labels ---

    @Test
    fun `labels are provided for all levels`() {
        for (level in ProvenanceLevel.entries) {
            val label = ProvenanceClassifier.labelFor(level)
            assertTrue(
                "Label for $level should not be blank",
                label.isNotBlank()
            )
        }
    }

    @Test
    fun `hypothesis label indicates AI and unverified`() {
        val label = ProvenanceClassifier.labelFor(ProvenanceLevel.HYPOTHESIS)
        assertTrue(
            "Hypothesis label should mention AI or unverified",
            label.contains("AI") || label.contains("unverified")
        )
    }

    @Test
    fun `source label indicates source material`() {
        val label = ProvenanceClassifier.labelFor(ProvenanceLevel.SOURCE)
        assertTrue(
            "Source label should mention source",
            label.lowercase().contains("source")
        )
    }
}
