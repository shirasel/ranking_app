package com.ytranklab.statistics

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlinx.serialization.json.Json

class VideoStatisticsHistoryStore(
    private val videoStatisticsDirectory: Path,
    private val maxHistoryItems: Int = 90,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    },
    private val fileWriter: AtomicStatisticsFileWriter = AtomicStatisticsFileWriter(),
) {
    fun append(statistic: VideoStatistic) {
        val historyFile = videoStatisticsDirectory.resolve("${statistic.videoId}.json")
        val existing = if (historyFile.exists()) {
            json.decodeFromString(VideoStatisticsHistoryDocument.serializer(), historyFile.readText())
        } else {
            VideoStatisticsHistoryDocument(videoId = statistic.videoId, statistics = emptyList())
        }
        val nextStatistics = (existing.statistics.filterNot { it.capturedAt == statistic.capturedAt } + statistic)
            .sortedBy { it.capturedAt }
            .takeLast(maxHistoryItems)
        val nextDocument = VideoStatisticsHistoryDocument(
            videoId = statistic.videoId,
            statistics = nextStatistics,
        )
        fileWriter.write(historyFile, json.encodeToString(VideoStatisticsHistoryDocument.serializer(), nextDocument))
    }
}
