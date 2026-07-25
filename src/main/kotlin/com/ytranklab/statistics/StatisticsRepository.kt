package com.ytranklab.statistics

import com.ytranklab.domain.YouTubeVideo

interface StatisticsRepository {
    fun loadLatest(): Map<String, VideoStatistic>

    fun loadSevenDayBaselines(capturedAt: String): Map<String, VideoStatistic> = emptyMap()

    fun saveLatest(capturedAt: String, videos: List<YouTubeVideo>)
}
