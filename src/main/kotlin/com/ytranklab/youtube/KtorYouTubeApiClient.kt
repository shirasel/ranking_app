package com.ytranklab.youtube

import com.ytranklab.domain.YouTubeVideo
import kotlinx.serialization.json.Json

class KtorYouTubeApiClient(
    apiKey: String,
    maxRetries: Int = 3,
    private val transport: YouTubeHttpTransport = KtorYouTubeHttpTransport(apiKey, maxRetries),
    private val mapper: YouTubeVideoMapper = YouTubeVideoMapper(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : YouTubeApiClient {
    override suspend fun searchVideoIds(keyword: String, maxResults: Int): List<String> {
        if (keyword.isBlank() || maxResults <= 0) return emptyList()
        return searchVideoIds(keyword, maxResults, "date", null)
    }

    override suspend fun searchRecentPopularVideoIds(keyword: String, maxResults: Int, publishedAfter: String): List<String> {
        if (keyword.isBlank() || maxResults <= 0 || publishedAfter.isBlank()) return emptyList()
        return searchVideoIds(keyword, maxResults, "viewCount", publishedAfter)
    }

    private suspend fun searchVideoIds(
        keyword: String,
        maxResults: Int,
        order: String,
        publishedAfter: String?,
    ): List<String> {
        val parameters = mutableMapOf(
            "part" to "snippet",
            "type" to "video",
            "q" to keyword,
            "order" to order,
            "maxResults" to maxResults.coerceIn(1, 50).toString(),
            "safeSearch" to "none",
            "regionCode" to "JP",
            "relevanceLanguage" to "ja",
        )
        if (publishedAfter != null) parameters["publishedAfter"] = publishedAfter

        val response = transport.getJson(
            path = "search",
            parameters = parameters,
        )
        return json.decodeFromString(SearchListResponse.serializer(), response)
            .items
            .mapNotNull { it.id.videoId }
    }

    override suspend fun fetchPopularVideoIds(regionCode: String, maxResults: Int): List<String> {
        return fetchPopularVideoIds(regionCode, maxResults, "")
    }

    override suspend fun fetchPopularVideoIds(regionCode: String, maxResults: Int, videoCategoryId: String): List<String> {
        if (maxResults <= 0) return emptyList()
        val parameters = mutableMapOf(
            "part" to "snippet",
            "chart" to "mostPopular",
            "regionCode" to regionCode.ifBlank { "JP" },
            "maxResults" to maxResults.coerceIn(1, 50).toString(),
        )
        if (videoCategoryId.isNotBlank()) parameters["videoCategoryId"] = videoCategoryId

        val response = transport.getJson(
            path = "videos",
            parameters = parameters,
        )
        return json.decodeFromString(VideoListResponse.serializer(), response)
            .items
            .map { it.id }
    }

    override suspend fun fetchLatestVideoIdsForChannel(channelId: String, maxResults: Int): List<String> {
        if (channelId.isBlank() || maxResults <= 0) return emptyList()
        val channelResponse = transport.getJson(
            path = "channels",
            parameters = mapOf(
                "part" to "contentDetails",
                "id" to channelId,
            ),
        )
        val uploadsPlaylistId = json.decodeFromString(ChannelListResponse.serializer(), channelResponse)
            .items
            .firstOrNull()
            ?.contentDetails
            ?.relatedPlaylists
            ?.uploads
            ?: return emptyList()

        val playlistResponse = transport.getJson(
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
            val response = transport.getJson(
                path = "videos",
                parameters = mapOf(
                    "part" to "snippet,statistics,status",
                    "id" to chunk.joinToString(","),
                ),
            )
            videos.addAll(json.decodeFromString(VideoListResponse.serializer(), response).items)
        }

        val subscriberCounts = fetchSubscriberCounts(videos.map { it.snippet.channelId }.distinct())
        return videos.mapNotNull { item -> mapper.toDomain(item, subscriberCounts) }
    }

    override fun close() {
        if (transport is AutoCloseable) {
            transport.close()
        }
    }

    private suspend fun fetchSubscriberCounts(channelIds: List<String>): Map<String, Long?> {
        if (channelIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, Long?>()
        channelIds.distinct().chunked(50).forEach { chunk ->
            val response = transport.getJson(
                path = "channels",
                parameters = mapOf(
                    "part" to "statistics",
                    "id" to chunk.joinToString(","),
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
}
