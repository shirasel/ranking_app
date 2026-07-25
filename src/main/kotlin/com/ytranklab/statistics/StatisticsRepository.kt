package com.ytranklab.statistics

import com.ytranklab.domain.YouTubeVideo

interface StatisticsRepository {
    fun loadLatest(): Map<String, VideoStatistic>

    fun saveLatest(capturedAt: String, videos: List<YouTubeVideo>)
}
