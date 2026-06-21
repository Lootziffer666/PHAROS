package com.flow.pharos

import com.flow.pharos.core.llm.ChatRequest
import com.flow.pharos.core.llm.ChatResponse
import com.flow.pharos.core.llm.LlmGateway
import com.flow.pharos.usecase.ClaimExtractionUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaimExtractionParsingTest {

    private val dummyGateway = object : LlmGateway {
        override suspend fun ping(): Result<String> = Result.success("ok")
        override suspend fun models(): Result<List<String>> = Result.success(emptyList())
        override suspend fun chat(request: ChatRequest): Result<ChatResponse> =
            Result.success(ChatResponse(content = "", model = "test"))
    }

    private val useCase = ClaimExtractionUseCase(dummyGateway)

    @Test
    fun `parseClaimExtractionResponse parses valid JSON`() {
        val json = """
        {
            "claims": [
                {"content": "The Earth orbits the Sun.", "confidence": 0.99, "cluster": "astronomy"},
                {"content": "Water boils at 100 degrees Celsius at sea level.", "confidence": 0.95, "cluster": "physics"},
                {"content": "Kotlin is a JVM language.", "confidence": 0.9, "cluster": "programming"}
            ]
        }
        """.trimIndent()

        val result = useCase.parseClaimExtractionResponse(json)
        assertNotNull(result)
        assertEquals(3, result!!.claims.size)
        assertEquals("The Earth orbits the Sun.", result.claims[0].content)
        assertEquals(0.99, result.claims[0].confidence, 0.001)
        assertEquals("astronomy", result.claims[0].cluster)
        assertEquals("Water boils at 100 degrees Celsius at sea level.", result.claims[1].content)
        assertEquals(0.95, result.claims[1].confidence, 0.001)
        assertEquals("physics", result.claims[1].cluster)
        assertEquals("Kotlin is a JVM language.", result.claims[2].content)
        assertEquals(0.9, result.claims[2].confidence, 0.001)
        assertEquals("programming", result.claims[2].cluster)
    }

    @Test
    fun `parseClaimExtractionResponse handles code fences`() {
        val json = "```json\n{\"claims\": [{\"content\": \"Test claim\", \"confidence\": 0.8, \"cluster\": \"test\"}]}\n```"
        val result = useCase.parseClaimExtractionResponse(json)
        assertNotNull(result)
        assertEquals(1, result!!.claims.size)
        assertEquals("Test claim", result.claims[0].content)
        assertEquals(0.8, result.claims[0].confidence, 0.001)
        assertEquals("test", result.claims[0].cluster)
    }

    @Test
    fun `parseClaimExtractionResponse handles backticks only`() {
        val json = "```\n{\"claims\": [{\"content\": \"A claim\", \"confidence\": 0.75, \"cluster\": null}]}\n```"
        val result = useCase.parseClaimExtractionResponse(json)
        assertNotNull(result)
        assertEquals(1, result!!.claims.size)
        assertEquals("A claim", result.claims[0].content)
        assertNull(result.claims[0].cluster)
    }

    @Test
    fun `parseClaimExtractionResponse handles null cluster`() {
        val json = """{"claims": [{"content": "No cluster claim", "confidence": 0.6}]}"""
        val result = useCase.parseClaimExtractionResponse(json)
        assertNotNull(result)
        assertEquals(1, result!!.claims.size)
        assertEquals("No cluster claim", result.claims[0].content)
        assertEquals(0.6, result.claims[0].confidence, 0.001)
        assertNull(result.claims[0].cluster)
    }

    @Test
    fun `parseClaimExtractionResponse handles empty claims list`() {
        val json = """{"claims": []}"""
        val result = useCase.parseClaimExtractionResponse(json)
        assertNotNull(result)
        assertEquals(0, result!!.claims.size)
    }

    @Test
    fun `parseClaimExtractionResponse returns null for invalid JSON`() {
        val result = useCase.parseClaimExtractionResponse("not json at all")
        assertNull(result)
    }

    @Test
    fun `parseClaimExtractionResponse returns null for empty string`() {
        val result = useCase.parseClaimExtractionResponse("")
        assertNull(result)
    }

    @Test
    fun `parseClaimExtractionResponse handles multiple claims with mixed clusters`() {
        val json = """
        {
            "claims": [
                {"content": "Claim A", "confidence": 0.5, "cluster": "topic1"},
                {"content": "Claim B", "confidence": 0.7, "cluster": null},
                {"content": "Claim C", "confidence": 1.0, "cluster": "topic2"}
            ]
        }
        """.trimIndent()

        val result = useCase.parseClaimExtractionResponse(json)
        assertNotNull(result)
        assertEquals(3, result!!.claims.size)
        assertEquals("topic1", result.claims[0].cluster)
        assertNull(result.claims[1].cluster)
        assertEquals("topic2", result.claims[2].cluster)
    }

    @Test
    fun `parseClaimExtractionResponse handles whitespace in content`() {
        val json = """{"claims": [{"content": "  Claim with spaces  ", "confidence": 0.85, "cluster": "general"}]}"""
        val result = useCase.parseClaimExtractionResponse(json)
        assertNotNull(result)
        assertEquals("  Claim with spaces  ", result!!.claims[0].content)
    }

    @Test
    fun `parseClaimExtractionResponse handles confidence boundary values`() {
        val json = """
        {
            "claims": [
                {"content": "Zero confidence", "confidence": 0.0, "cluster": "test"},
                {"content": "Full confidence", "confidence": 1.0, "cluster": "test"}
            ]
        }
        """.trimIndent()

        val result = useCase.parseClaimExtractionResponse(json)
        assertNotNull(result)
        assertEquals(0.0, result!!.claims[0].confidence, 0.001)
        assertEquals(1.0, result.claims[1].confidence, 0.001)
    }

    @Test
    fun `parseClaimExtractionResponse handles missing claims key`() {
        val json = """{"other_key": "value"}"""
        val result = useCase.parseClaimExtractionResponse(json)
        assertNotNull(result)
        assertTrue(result!!.claims.isEmpty())
    }
}
