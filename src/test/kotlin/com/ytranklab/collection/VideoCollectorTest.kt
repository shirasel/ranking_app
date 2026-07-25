package com.ytranklab.collection

import com.ytranklab.config.CollectionConfig
import com.ytranklab.config.SourceChannel
import com.ytranklab.config.SourceConfig
import com.ytranklab.domain.YouTubeVideo
import com.ytranklab.youtube.YouTubeApiClient
import com.ytranklab.youtube.YouTubeApiException
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
                maxEstimatedQuotaUnits = 9000,
                reservedDetailQuotaUnits = 20,
            ),
        )

        val result = VideoCollector(config, client).collect()

        assertEquals(listOf("manualVideo01", "channelVideo01", "searchVideo01", "popularVideo01"), client.fetchedIds)
        assertEquals(4, result.videos.size)
        assertEquals(105, result.report.estimatedQuotaUnits)
        assertEquals(4, result.report.uniqueCandidateIds)
    }

    @Test
    fun continuesWhenOneSourceFails() = runBlocking {
        val client = FailingSearchYouTubeApiClient()
        val config = SourceConfig(
            channels = emptyList(),
            keywords = listOf("broken"),
            videos = listOf("manualVideo01"),
            collection = CollectionConfig(
                maxVideos = 10,
                maxSearchResultsPerKeyword = 5,
                includePopularVideos = true,
                regionCode = "JP",
                maxPopularVideos = 5,
                maxChannelVideos = 5,
                maxEstimatedQuotaUnits = 9000,
                reservedDetailQuotaUnits = 20,
            ),
        )

        val result = VideoCollector(config, client).collect()

        assertEquals(listOf("manualVideo01", "popularVideo01"), client.fetchedIds)
        assertEquals(2, result.videos.size)
        assertEquals("skipped", result.report.sourceResults.first { it.source == "keyword:broken" }.status)
        assertEquals(3, result.report.estimatedQuotaUnits)
    }

    @Test
    fun skipsSourcesThatWouldExceedQuotaBudget() = runBlocking {
        val client = FakeYouTubeApiClient()
        val config = SourceConfig(
            channels = emptyList(),
            keywords = listOf("Kotlin", "Minecraft"),
            videos = listOf("manualVideo01"),
            collection = CollectionConfig(
                maxVideos = 10,
                maxSearchResultsPerKeyword = 5,
                includePopularVideos = true,
                regionCode = "JP",
                maxPopularVideos = 5,
                maxChannelVideos = 5,
                maxEstimatedQuotaUnits = 150,
                reservedDetailQuotaUnits = 20,
            ),
        )

        val result = VideoCollector(config, client).collect()

        assertEquals(listOf("manualVideo01", "searchVideo01", "popularVideo01"), client.fetchedIds)
        assertEquals("ok", result.report.sourceResults.first { it.source == "keyword:Kotlin" }.status)
        assertEquals("skipped", result.report.sourceResults.first { it.source == "keyword:Minecraft" }.status)
        assertEquals("quota budget limit", result.report.sourceResults.first { it.source == "keyword:Minecraft" }.message)
        assertEquals(103, result.report.estimatedQuotaUnits)
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

    private class FailingSearchYouTubeApiClient : YouTubeApiClient {
        var fetchedIds: List<String> = emptyList()

        override suspend fun searchVideoIds(keyword: String, maxResults: Int): List<String> {
            throw YouTubeApiException("YouTube API request failed: badRequest")
        }

        override suspend fun fetchPopularVideoIds(regionCode: String, maxResults: Int): List<String> =
            listOf("popularVideo01")

        override suspend fun fetchLatestVideoIdsForChannel(channelId: String, maxResults: Int): List<String> =
            emptyList()

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
