package com.ytranklab.output

import com.ytranklab.domain.RankingEntry
import com.ytranklab.domain.VideoDetailDocument
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlinx.serialization.json.Json

class VideoDetailWriter(
    private val videoDirectory: Path,
    private val videoRankingHistoryDirectory: Path,
    private val fileWriter: JsonFileWriter,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun write(entries: List<RankingEntry>, generatedAt: String) {
        entries.forEach { entry ->
            val document = VideoDetailDocument(
                generatedAt = generatedAt,
                video = entry,
                scoreBreakdown = entry.scoreBreakdown,
                genres = entry.genres,
            )
            fileWriter.write(
                videoDirectory.resolve("${entry.videoId}.json"),
                fileWriter.encode(VideoDetailDocument.serializer(), document),
            )
            appendVideoRankingHistory(generatedAt, entry)
        }
    }

    private fun appendVideoRankingHistory(generatedAt: String, entry: RankingEntry) {
        val historyFile = videoRankingHistoryDirectory.resolve("${entry.videoId}.json")
        val existing = if (historyFile.exists()) {
            json.decodeFromString(VideoRankingHistoryDocument.serializer(), historyFile.readText())
        } else {
            VideoRankingHistoryDocument(videoId = entry.videoId, rankings = emptyList())
        }
        val next = VideoRankingHistoryItem(
            capturedAt = generatedAt,
            rank = entry.rank,
            previousRank = entry.previousRank,
            rankChange = entry.rankChange,
            rawScore = entry.rawScore,
            normalizedScore = entry.normalizedScore,
        )
        val rankings = (existing.rankings.filterNot { it.capturedAt == generatedAt } + next)
            .sortedBy { it.capturedAt }
            .takeLast(365)
        val document = VideoRankingHistoryDocument(videoId = entry.videoId, rankings = rankings)
        fileWriter.write(historyFile, fileWriter.encode(VideoRankingHistoryDocument.serializer(), document))
    }
}
