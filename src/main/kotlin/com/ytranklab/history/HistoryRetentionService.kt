package com.ytranklab.history

import com.ytranklab.config.RetentionConfig
import java.nio.file.Path
import java.time.OffsetDateTime

class HistoryRetentionService(
    private val dataDirectory: Path,
    retentionConfig: RetentionConfig,
    private val rankingHistoryCleaner: RankingHistoryCleaner = RankingHistoryCleaner(
        historyDirectory = dataDirectory.resolve("history"),
        rankingHistoryDays = retentionConfig.rankingHistoryDays,
    ),
    private val videoScopedJsonCleaner: VideoScopedJsonCleaner = VideoScopedJsonCleaner(),
) {
    fun cleanup(generatedAt: String, activeVideoIds: Set<String>): RetentionResult {
        val currentDate = OffsetDateTime.parse(generatedAt).toLocalDate()
        val historyDeleted = rankingHistoryCleaner.cleanup(currentDate)
        val videoDetailsDeleted = videoScopedJsonCleaner.cleanup(dataDirectory.resolve("videos"), activeVideoIds)
        videoScopedJsonCleaner.cleanup(dataDirectory.resolve("rankings").resolve("videos"), activeVideoIds)
        return RetentionResult(
            historyDeleted = historyDeleted,
            videoDetailsDeleted = videoDetailsDeleted,
        )
    }
}
