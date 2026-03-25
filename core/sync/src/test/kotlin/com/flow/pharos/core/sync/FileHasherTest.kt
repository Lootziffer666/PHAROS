package com.flow.pharos.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class FileHasherTest {

    @Test
    fun `sha256 of known string`() {
        val hash = FileHasher.sha256("hello".toByteArray())
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            hash
        )
    }

    @Test
    fun `sha256 of empty bytes`() {
        val hash = FileHasher.sha256(ByteArray(0))
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hash
        )
    }

    @Test
    fun `sha256 from input stream`() {
        val hash = FileHasher.sha256(ByteArrayInputStream("hello".toByteArray()))
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            hash
        )
    }

    @Test
    fun `different content produces different hashes`() {
        val hash1 = FileHasher.sha256("content A".toByteArray())
        val hash2 = FileHasher.sha256("content B".toByteArray())
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `identical content produces identical hashes`() {
        val hash1 = FileHasher.sha256("same content".toByteArray())
        val hash2 = FileHasher.sha256("same content".toByteArray())
        assertEquals(hash1, hash2)
    }

    @Test
    fun `hash has correct length`() {
        val hash = FileHasher.sha256("test".toByteArray())
        assertEquals(64, hash.length)
    }
}
