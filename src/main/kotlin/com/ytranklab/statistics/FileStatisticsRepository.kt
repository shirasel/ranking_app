package com.ytranklab.statistics

import com.ytranklab.domain.YouTubeVideo
import java.nio.file.Path

class FileStatisticsRepository(
    statisticsFile: Path,
    fallbackFile: Path? = null,
    preferFallback: Boolean = false,
    private val statisticFactory: VideoStatisticFactory = VideoStatisticFactory(),
    private val latestStore: LatestStatisticsStore = LatestStatisticsStore(
        statisticsFile = statisticsFile,
        fallbackFile = fallbackFile,
        preferFallback = preferFallback,
    ),
    private val historyStore: VideoStatisticsHistoryStore = VideoStatisticsHistoryStore(
        videoStatisticsDirectory = statisticsFile.parent.resolve("videos"),
    ),
) : StatisticsRepository {
    override fun loadLatest(): Map<String, VideoStatistic> =
        latestStore.load()

    override fun saveLatest(capturedAt: String, videos: List<YouTubeVideo>) {
        val statistics = videos.map { video -> statisticFactory.create(capturedAt, video) }
        latestStore.save(capturedAt, statistics)
        statistics.forEach { statistic -> historyStore.append(statistic) }
    }
}
