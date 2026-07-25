package com.ytranklab.collection

import com.ytranklab.config.SourceConfig
import com.ytranklab.domain.YouTubeVideo
import com.ytranklab.youtube.YouTubeApiClient
import java.time.OffsetDateTime
import java.time.ZoneOffset

class VideoCollector(
    private val sourceConfig: SourceConfig,
    private val client: YouTubeApiClient,
) {
    suspend fun collect(): CollectedVideos {
        val videoIds = linkedSetOf<String>()

        videoIds.addAll(sourceConfig.videos.filter { it.isValidYouTubeId() })

        sourceConfig.channels
            .filter { it.enabled }
            .forEach { channel ->
                videoIds.addAll(client.fetchLatestVideoIdsForChannel(channel.id, sourceConfig.collection.maxChannelVideos))
            }

        sourceConfig.keywords.forEach { keyword ->
            videoIds.addAll(client.searchVideoIds(keyword, sourceConfig.collection.maxSearchResultsPerKeyword))
        }

        if (sourceConfig.collection.includePopularVideos) {
            videoIds.addAll(
                client.fetchPopularVideoIds(
                    regionCode = sourceConfig.collection.regionCode,
                    maxResults = sourceConfig.collection.maxPopularVideos,
                ),
            )
        }

        val limitedIds = videoIds.take(sourceConfig.collection.maxVideos)
        val videos = client.fetchVideos(limitedIds)
            .filter { it.status == "public" }
            .take(sourceConfig.collection.maxVideos)

        return CollectedVideos(
            capturedAt = OffsetDateTime.now(ZoneOffset.UTC).toString(),
            videos = videos,
        )
    }
}

data class CollectedVideos(
    val capturedAt: String,
    val videos: List<YouTubeVideo>,
)

private fun String.isValidYouTubeId(): Boolean = matches(Regex("^[A-Za-z0-9_-]{6,64}$"))
