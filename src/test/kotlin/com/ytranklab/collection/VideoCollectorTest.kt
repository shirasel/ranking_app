package com.ytranklab.collection

import com.ytranklab.config.CollectionConfig
import com.ytranklab.config.SourceChannel
import com.ytranklab.config.SourceConfig
import com.ytranklab.domain.YouTubeVideo
import com.ytranklab.youtube.YouTubeApiClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class VideoCollectorTest {
    @Test
    fun collectsDeduplicatedVideoIdsAndFetchesVideosOnce() = runBlocking {
        val client = FakeYouTubeApiClient()
        val config = SourceConfig(
            channels = listOf(SourceChannel("channel-1", enabled = true)),
            keywords = listOf("Kotlin"),
            videos = listOf("manualVideo01", "bad id"),
            collection = CollectionConfig(
                maxVideos = 10,
                maxSearchResultsPerKeyword = 5,
                includePopularVideos = true,
                regionCode = "JP",
                maxPopularVideos = 5,
                maxChannelVideos = 5,
            ),
        )

        val result = VideoCollector(config, client).collect()

        assertEquals(listOf("manualVideo01", "channelVideo01", "searchVideo01", "popularVideo01"), client.fetchedIds)
        assertEquals(4, result.videos.size)
    }

    private class FakeYouTubeApiClient : YouTubeApiClient {
        var fetchedIds: List<String> = emptyList()

        override suspend fun searchVideoIds(keyword: String, maxResults: Int): List<String> =
            listOf("searchVideo01", "manualVideo01")

        override suspend fun fetchPopularVideoIds(regionCode: String, maxResults: Int): List<String> =
            listOf("popularVideo01")

        override suspend fun fetchLatestVideoIdsForChannel(channelId: String, maxResults: Int): List<String> =
            listOf("channelVideo01")

        override suspend fun fetchVideos(videoIds: List<String>): List<YouTubeVideo> {
            fetchedIds = videoIds
            return videoIds.map {
                YouTubeVideo(
                    videoId = it,
                    title = it,
                    channelId = "channel",
                    channelName = "channel",
                    youtubeCategoryId = "20",
                    publishedAt = "2026-07-25T00:00:00Z",
                    viewCount = 1,
                )
            }
        }

        override fun close() = Unit
    }
}
