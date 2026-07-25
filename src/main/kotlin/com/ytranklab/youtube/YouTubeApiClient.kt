package com.ytranklab.youtube

import com.ytranklab.domain.YouTubeVideo

interface YouTubeApiClient : AutoCloseable {
    suspend fun searchVideoIds(keyword: String, maxResults: Int): List<String>

    suspend fun searchRecentPopularVideoIds(keyword: String, maxResults: Int, publishedAfter: String): List<String> =
        searchVideoIds(keyword, maxResults)

    suspend fun fetchPopularVideoIds(regionCode: String, maxResults: Int): List<String>

    suspend fun fetchPopularVideoIds(regionCode: String, maxResults: Int, videoCategoryId: String): List<String> =
        fetchPopularVideoIds(regionCode, maxResults)

    suspend fun fetchLatestVideoIdsForChannel(channelId: String, maxResults: Int): List<String>

    suspend fun fetchVideos(videoIds: List<String>): List<YouTubeVideo>
}
