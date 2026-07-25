package com.ytranklab.youtube

import com.ytranklab.domain.YouTubeVideo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class KtorYouTubeApiClient(
    private val apiKey: String,
    private val maxRetries: Int = 3,
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 20_000
        }
    },
) : YouTubeApiClient {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun searchVideoIds(keyword: String, maxResults: Int): List<String> {
        if (keyword.isBlank() || maxResults <= 0) return emptyList()
        val response = getJson(
            path = "search",
            parameters = mapOf(
                "part" to "snippet",
                "type" to "video",
                "q" to keyword,
                "order" to "date",
                "maxResults" to maxResults.coerceIn(1, 50).toString(),
                "safeSearch" to "none",
            ),
        )
        return json.decodeFromString(SearchListResponse.serializer(), response)
            .items
            .mapNotNull { it.id.videoId }
    }

    override suspend fun fetchPopularVideoIds(regionCode: String, maxResults: Int): List<String> {
        if (maxResults <= 0) return emptyList()
        val response = getJson(
            path = "videos",
            parameters = mapOf(
                "part" to "snippet",
                "chart" to "mostPopular",
                "regionCode" to regionCode.ifBlank { "JP" },
                "maxResults" to maxResults.coerceIn(1, 50).toString(),
            ),
        )
        return json.decodeFromString(VideoListResponse.serializer(), response)
            .items
            .map { it.id }
    }

    override suspend fun fetchLatestVideoIdsForChannel(channelId: String, maxResults: Int): List<String> {
        if (channelId.isBlank() || maxResults <= 0) return emptyList()
        val channelResponse = getJson(
            path = "channels",
            parameters = mapOf(
                "part" to "contentDetails",
                "id" to channelId,
                "maxResults" to "1",
            ),
        )
        val uploadsPlaylistId = json.decodeFromString(ChannelListResponse.serializer(), channelResponse)
            .items
            .firstOrNull()
            ?.contentDetails
            ?.relatedPlaylists
            ?.uploads
            ?: return emptyList()

        val playlistResponse = getJson(
            path = "playlistItems",
            parameters = mapOf(
                "part" to "contentDetails,status",
                "playlistId" to uploadsPlaylistId,
                "maxResults" to maxResults.coerceIn(1, 50).toString(),
            ),
        )
        return json.decodeFromString(PlaylistItemListResponse.serializer(), playlistResponse)
            .items
            .filter { it.status?.privacyStatus == null || it.status.privacyStatus == "public" }
            .mapNotNull { it.contentDetails.videoId }
    }

    override suspend fun fetchVideos(videoIds: List<String>): List<YouTubeVideo> {
        val ids = videoIds.distinct().filter { it.isNotBlank() }
        if (ids.isEmpty()) return emptyList()

        val videos = mutableListOf<VideoItem>()
        ids.chunked(50).forEach { chunk ->
            val response = getJson(
                path = "videos",
                parameters = mapOf(
                    "part" to "snippet,statistics,status",
                    "id" to chunk.joinToString(","),
                    "maxResults" to "50",
                ),
            )
            videos.addAll(json.decodeFromString(VideoListResponse.serializer(), response).items)
        }

        val subscriberCounts = fetchSubscriberCounts(videos.map { it.snippet.channelId }.distinct())
        return videos.mapNotNull { item ->
            val status = item.status?.privacyStatus ?: "public"
            if (status != "public") {
                null
            } else {
                YouTubeVideo(
                    videoId = item.id,
                    title = item.snippet.title,
                    description = item.snippet.description.orEmpty(),
                    channelId = item.snippet.channelId,
                    channelName = item.snippet.channelTitle,
                    youtubeCategoryId = item.snippet.categoryId,
                    thumbnailUrl = item.snippet.thumbnails.bestUrl(),
                    publishedAt = item.snippet.publishedAt,
                    viewCount = item.statistics?.viewCount?.toLongOrNull() ?: 0L,
                    likeCount = item.statistics?.likeCount?.toLongOrNull(),
                    commentCount = item.statistics?.commentCount?.toLongOrNull(),
                    subscriberCount = subscriberCounts[item.snippet.channelId],
                    status = status,
                )
            }
        }
    }

    override fun close() {
        httpClient.close()
    }

    private suspend fun fetchSubscriberCounts(channelIds: List<String>): Map<String, Long?> {
        if (channelIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, Long?>()
        channelIds.distinct().chunked(50).forEach { chunk ->
            val response = getJson(
                path = "channels",
                parameters = mapOf(
                    "part" to "statistics",
                    "id" to chunk.joinToString(","),
                    "maxResults" to "50",
                ),
            )
            json.decodeFromString(ChannelListResponse.serializer(), response)
                .items
                .forEach { channel ->
                    result[channel.id] = if (channel.statistics?.hiddenSubscriberCount == true) {
                        null
                    } else {
                        channel.statistics?.subscriberCount?.toLongOrNull()
                    }
                }
        }
        return result
    }

    private suspend fun getJson(path: String, parameters: Map<String, String>): String {
        var lastError: Throwable? = null
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

                val error = json.decodeFromString(YouTubeErrorResponse.serializer(), body)
                val reason = error.error.errors.firstOrNull()?.reason
                    ?: error.error.message?.take(120)
                    ?: error.error.status
                    ?: "unknown"
                if (reason in nonRetryableReasons) {
                    throw YouTubeApiException("YouTube API request failed: $reason")
                }
                lastError = YouTubeApiException("YouTube API request failed: $reason")
            } catch (error: YouTubeApiException) {
                throw error
            } catch (error: IOException) {
                if (error.message?.contains("Permission denied", ignoreCase = true) == true) {
                    return getJsonWithCurl(path, parameters)
                }
                lastError = error
            }

            if (attempt < maxRetries - 1) {
                delay(backoffMillis(attempt))
            }
        }

        throw YouTubeApiException("YouTube API request failed after retries", lastError)
    }

    private suspend fun getJsonWithCurl(path: String, parameters: Map<String, String>): String = withContext(Dispatchers.IO) {
        val url = buildString {
            append("https://www.googleapis.com/youtube/v3/")
            append(path)
            append("?")
            append((parameters + ("key" to apiKey)).entries.joinToString("&") { (key, value) ->
                "${urlEncode(key)}=${urlEncode(value)}"
            })
        }
        val curlCommand = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) "curl.exe" else "curl"
        val process = ProcessBuilder(curlCommand, "-sS", "--fail-with-body", url)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val reason = runCatching {
                json.decodeFromString(YouTubeErrorResponse.serializer(), output)
                    .error
                    .errors
                    .firstOrNull()
                    ?.reason
            }.getOrNull() ?: output.lineSequence().firstOrNull()?.take(120) ?: "curlRequestFailed"
            throw YouTubeApiException("YouTube API request failed: $reason")
        }
        output
    }

    private fun backoffMillis(attempt: Int): Long = 500L * (1 shl attempt)

    private val nonRetryableReasons = setOf(
        "keyInvalid",
        "dailyLimitExceeded",
        "quotaExceeded",
        "forbidden",
        "badRequest",
    )
}

class YouTubeApiException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

@Serializable
private data class SearchListResponse(
    val items: List<SearchItem> = emptyList(),
)

@Serializable
private data class SearchItem(
    val id: SearchId,
)

@Serializable
private data class SearchId(
    val videoId: String? = null,
)

@Serializable
private data class PlaylistItemListResponse(
    val items: List<PlaylistItem> = emptyList(),
)

@Serializable
private data class PlaylistItem(
    val contentDetails: PlaylistContentDetails,
    val status: PlaylistStatus? = null,
)

@Serializable
private data class PlaylistContentDetails(
    val videoId: String? = null,
)

@Serializable
private data class PlaylistStatus(
    val privacyStatus: String? = null,
)

@Serializable
private data class VideoListResponse(
    val items: List<VideoItem> = emptyList(),
)

@Serializable
private data class VideoItem(
    val id: String,
    val snippet: VideoSnippet,
    val statistics: VideoStatistics? = null,
    val status: VideoStatus? = null,
)

@Serializable
private data class VideoSnippet(
    val publishedAt: String,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val thumbnails: Map<String, Thumbnail> = emptyMap(),
    val channelTitle: String,
    val categoryId: String = "",
)

@Serializable
private data class Thumbnail(
    val url: String,
)

@Serializable
private data class VideoStatistics(
    val viewCount: String? = null,
    val likeCount: String? = null,
    val commentCount: String? = null,
)

@Serializable
private data class VideoStatus(
    val privacyStatus: String? = null,
)

@Serializable
private data class ChannelListResponse(
    val items: List<ChannelItem> = emptyList(),
)

@Serializable
private data class ChannelItem(
    val id: String,
    val contentDetails: ChannelContentDetails? = null,
    val statistics: ChannelStatistics? = null,
)

@Serializable
private data class ChannelContentDetails(
    val relatedPlaylists: RelatedPlaylists? = null,
)

@Serializable
private data class RelatedPlaylists(
    val uploads: String? = null,
)

@Serializable
private data class ChannelStatistics(
    val subscriberCount: String? = null,
    val hiddenSubscriberCount: Boolean? = null,
)

@Serializable
private data class YouTubeErrorResponse(
    val error: YouTubeError,
)

@Serializable
private data class YouTubeError(
    val code: Int? = null,
    val message: String? = null,
    val errors: List<YouTubeErrorDetail> = emptyList(),
    val status: String? = null,
)

@Serializable
private data class YouTubeErrorDetail(
    val reason: String? = null,
)

private fun Map<String, Thumbnail>.bestUrl(): String =
    listOf("maxres", "standard", "high", "medium", "default")
        .firstNotNullOfOrNull { this[it]?.url }
        .orEmpty()

private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
