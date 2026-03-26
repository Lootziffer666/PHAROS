package com.flow.pharos.core.sync

import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference

class CloudSyncClientTest {

    @Test
    fun `downloadManifest fetches manifest JSON`() {
        val manifest = SyncManifest(
            deviceId = "cloud",
            createdAt = 123L,
            entries = listOf(
                ManifestEntry("a.txt", "abc", 10, 111L)
            )
        )

        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/manifest") { exchange ->
            val json = """{"version":1,"deviceId":"cloud","createdAt":123,"entries":[{"relativePath":"a.txt","sha256":"abc","size":10,"lastModified":111}]}"""
            exchange.sendResponseHeaders(200, json.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(json.toByteArray()) }
        }
        server.start()
        try {
            val client = CloudSyncClient()
            val actual = client.downloadManifest("http://localhost:${server.address.port}/manifest")
            assertEquals(manifest.deviceId, actual.deviceId)
            assertEquals(1, actual.entries.size)
            assertEquals("a.txt", actual.entries[0].relativePath)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `uploadManifest sends PUT payload`() {
        val capturedBody = AtomicReference<String>("")
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/manifest") { exchange ->
            capturedBody.set(exchange.requestBody.bufferedReader().readText())
            exchange.sendResponseHeaders(200, 0)
            exchange.responseBody.close()
        }
        server.start()
        try {
            val client = CloudSyncClient()
            val manifest = SyncManifest(
                deviceId = "desktop",
                createdAt = 456L,
                entries = listOf(ManifestEntry("b.txt", "def", 20, 222L))
            )
            client.uploadManifest("http://localhost:${server.address.port}/manifest", manifest)

            val body = capturedBody.get()
            org.junit.Assert.assertTrue(body.contains("\"deviceId\":\"desktop\""))
            org.junit.Assert.assertTrue(body.contains("\"relativePath\":\"b.txt\""))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `downloadManifest sends bearer token when provided`() {
        val authHeader = AtomicReference<String>("")
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/manifest") { exchange ->
            authHeader.set(exchange.requestHeaders.getFirst("Authorization") ?: "")
            val json = """{"version":1,"deviceId":"cloud","createdAt":1,"entries":[]}"""
            exchange.sendResponseHeaders(200, json.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(json.toByteArray()) }
        }
        server.start()
        try {
            val client = CloudSyncClient()
            client.downloadManifest(
                "http://localhost:${server.address.port}/manifest",
                bearerToken = "abc123"
            )
            assertEquals("Bearer abc123", authHeader.get())
        } finally {
            server.stop(0)
        }
    }

    @Test(expected = CloudSyncException::class)
    fun `uploadManifest throws CloudSyncException for invalid url`() {
        CloudSyncClient().uploadManifest(
            manifestUrl = "ftp://invalid",
            manifest = SyncManifest(deviceId = "x", createdAt = 0, entries = emptyList())
        )
    }

    @Test
    fun `downloadManifest wraps non-2xx as CloudSyncException`() {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/manifest") { exchange ->
            val body = "nope"
            exchange.sendResponseHeaders(500, body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        server.start()
        try {
            val ex = runCatching {
                CloudSyncClient().downloadManifest("http://localhost:${server.address.port}/manifest")
            }.exceptionOrNull()
            assertTrue(ex is CloudSyncException)
        } finally {
            server.stop(0)
        }
    }
}
