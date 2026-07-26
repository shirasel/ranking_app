package com.ytranklab.youtube

import kotlinx.serialization.json.Json

class YouTubeErrorParser(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val nonRetryableReasons = setOf(
        "keyInvalid",
        "dailyLimitExceeded",
        "quotaExceeded",
        "forbidden",
        "badRequest",
        "notFound",
        "videoNotFound",
    )

    fun parse(body: String): YouTubeApiError {
        val error = json.decodeFromString(YouTubeErrorResponse.serializer(), body).error
        val reason = error.safeReason()
        return YouTubeApiError(
            message = error.safeMessage(),
            retryable = reason !in nonRetryableReasons,
        )
    }

    fun safeMessage(body: String): String =
        runCatching { parse(body).message }
            .getOrNull()
            ?: body.lineSequence().firstOrNull()?.take(120)
            ?: "request failed"
}

data class YouTubeApiError(
    val message: String,
    val retryable: Boolean,
)

private fun YouTubeError.safeReason(): String =
    errors.firstOrNull()?.reason ?: status ?: "unknown"

private fun YouTubeError.safeMessage(): String {
    val reason = safeReason()
    val detail = message?.take(120)
    return if (detail.isNullOrBlank() || detail == reason) reason else "$reason: $detail"
}
