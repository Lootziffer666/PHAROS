package com.flow.pharos.core.sync

import com.google.gson.Gson
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Minimal cloud sync client for manifest exchange over HTTP.
 *
 * This focuses on syncing [SyncManifest] payloads so desktop/mobile clients
 * can compare local state with a cloud-hosted manifest endpoint.
 */
class CloudSyncClient(
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build(),
    private val gson: Gson = Gson()
) {

    fun downloadManifest(manifestUrl: String, bearerToken: String? = null): SyncManifest {
        return try {
            val request = authorizedRequest(manifestUrl, bearerToken)
                .GET()
                .header("Accept", "application/json")
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            require(response.statusCode() in 200..299) {
                "Cloud manifest download failed (${response.statusCode()}): ${response.body()}"
            }
            gson.fromJson(response.body(), SyncManifest::class.java)
        } catch (e: Exception) {
            throw CloudSyncException("Failed to download cloud manifest from $manifestUrl", e)
        }
    }

    fun uploadManifest(
        manifestUrl: String,
        manifest: SyncManifest,
        bearerToken: String? = null
    ) {
        try {
            val payload = gson.toJson(manifest)
            val request = authorizedRequest(manifestUrl, bearerToken)
                .PUT(HttpRequest.BodyPublishers.ofString(payload))
                .header("Content-Type", "application/json")
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            require(response.statusCode() in 200..299) {
                "Cloud manifest upload failed (${response.statusCode()}): ${response.body()}"
            }
        } catch (e: Exception) {
            throw CloudSyncException("Failed to upload cloud manifest to $manifestUrl", e)
        }
    }

    private fun authorizedRequest(
        manifestUrl: String,
        bearerToken: String?
    ): HttpRequest.Builder {
        val uri = URI.create(manifestUrl.trim())
        require(uri.scheme == "https" || uri.scheme == "http") {
            "Cloud manifest URL must start with http:// or https://"
        }
        return HttpRequest.newBuilder()
            .uri(uri)
            .timeout(Duration.ofSeconds(30))
            .apply {
                if (!bearerToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $bearerToken")
                }
            }
    }
}

class CloudSyncException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
