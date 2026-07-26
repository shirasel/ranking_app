package com.ytranklab.youtube

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import java.io.IOException
import kotlinx.coroutines.delay

interface YouTubeHttpTransport {
    suspend fun getJson(path: String, parameters: Map<String, String>): String
}

class KtorYouTubeHttpTransport(
    private val apiKey: String,
    private val maxRetries: Int = 3,
    private val httpClient: HttpClient = defaultHttpClient(),
    private val errorParser: YouTubeErrorParser = YouTubeErrorParser(),
    private val curlFallbackClient: YouTubeCurlFallbackClient = YouTubeCurlFallbackClient(apiKey, errorParser),
) : YouTubeHttpTransport, AutoCloseable {
    override suspend fun getJson(path: String, parameters: Map<String, String>): String {
        var lastError: Throwable? = null
        val requestDescription = describeRequest(path, parameters)
        repeat(maxRetries) { attempt ->
            try {
                val response = httpClient.get("https://www.googleapis.com/youtube/v3/$path") {
                    parameter("key", apiKey)
                    parameters.forEach { (key, value) -> parameter(key, value) }
                }
                val body = response.bodyAsText()
                if (response.status.isSuccess()) {
                    return body
                }

                val error = errorParser.parse(body)
                val message = "YouTube API request failed: $requestDescription status=${response.status.value} ${error.message}"
                if (!error.retryable) {
                    throw YouTubeApiException(message)
                }
                lastError = YouTubeApiException(message)
            } catch (error: YouTubeApiException) {
                throw error
            } catch (error: IOException) {
                if (error.message?.contains("Permission denied", ignoreCase = true) == true) {
                    return curlFallbackClient.getJson(path, parameters)
                }
                lastError = error
            }

            if (attempt < maxRetries - 1) {
                delay(backoffMillis(attempt))
            }
        }

        throw YouTubeApiException("YouTube API request failed after retries: ${lastError.safeMessage()}", lastError)
    }

    override fun close() {
        httpClient.close()
    }

    private fun backoffMillis(attempt: Int): Long = 500L * (1 shl attempt)

    private fun describeRequest(path: String, parameters: Map<String, String>): String =
        buildString {
            append(path)
            parameters.entries
                .filterNot { (key, _) -> key.equals("key", ignoreCase = true) }
                .sortedBy { (key, _) -> key }
                .joinToString("&") { (key, value) -> "$key=${value.take(MAX_LOGGED_PARAMETER_LENGTH)}" }
                .takeIf { it.isNotBlank() }
                ?.let { append("?").append(it) }
        }
}

private const val MAX_LOGGED_PARAMETER_LENGTH = 160

private fun Throwable?.safeMessage(): String =
    this?.message?.take(300) ?: "request failed"

private fun defaultHttpClient(): HttpClient =
    HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 20_000
        }
    }
