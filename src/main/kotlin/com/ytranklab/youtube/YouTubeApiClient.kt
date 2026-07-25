package com.ytranklab.youtube

import com.ytranklab.domain.YouTubeVideo

interface YouTubeApiClient : AutoCloseable {
    suspend fun searchVideoIds(keyword: String, maxResults: Int): List<String>

    suspend fun fetchPopularVideoIds(regionCode: String, maxResults: Int): List<String>

    suspend fun fetchLatestVideoIdsForChannel(channelId: String, maxResults: Int): List<String>

    suspend fun fetchVideos(videoIds: List<String>): List<YouTubeVideo>
}
