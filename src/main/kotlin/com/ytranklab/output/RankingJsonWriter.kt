package com.ytranklab.output

import com.ytranklab.collection.CollectionReport
import com.ytranklab.domain.GenreRankingDocument
import com.ytranklab.domain.RankingDocument
import com.ytranklab.domain.RankingEntry
import com.ytranklab.domain.VideoDetailDocument
import com.ytranklab.statistics.VideoStatisticsHistoryDocument
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.OffsetDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.io.path.writeText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class RankingJsonWriter(private val dataDirectory: Path) {
    private val latestDirectory = dataDirectory.resolve("latest")
    private val genreDirectory = latestDirectory.resolve("genres")
    private val videoDirectory = dataDirectory.resolve("videos")
    private val videoRankingHistoryDirectory = dataDirectory.resolve("rankings").resolve("videos")
    private val videoStatisticsHistoryDirectory = dataDirectory.resolve("statistics").resolve("videos")
    private val historyDirectory = dataDirectory.resolve("history")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun loadPreviousOverallRanks(): Map<String, Int> {
        val overallFile = latestDirectory.resolve("overall.json")
        if (!overallFile.exists()) return emptyMap()
        val document = json.decodeFromString(RankingDocument.serializer(), overallFile.readText())
        return document.ranking.associate { it.videoId to it.rank }
    }

    fun writeAll(
        overall: RankingDocument,
        genres: Map<String, GenreRankingDocument>,
        trending: RankingDocument,
        discovery: RankingDocument,
    ) {
        writeJson(latestDirectory.resolve("overall.json"), json.encodeToString(RankingDocument.serializer(), overall))
        writeToday(overall)
        writeJson(latestDirectory.resolve("trending.json"), json.encodeToString(RankingDocument.serializer(), trending))
        writeJson(latestDirectory.resolve("discovery.json"), json.encodeToString(RankingDocument.serializer(), discovery))
        writeHistory(overall)
        writeSevenDays(overall)
        writeHistoryIndex()
        genres.forEach { (slug, document) ->
            writeJson(genreDirectory.resolve("$slug.json"), json.encodeToString(GenreRankingDocument.serializer(), document))
        }
    }

    fun writeVideoDetails(entries: List<RankingEntry>, generatedAt: String) {
        entries.forEach { entry ->
            val document = VideoDetailDocument(
                generatedAt = generatedAt,
                video = entry,
                scoreBreakdown = entry.scoreBreakdown,
                genres = entry.genres,
            )
            writeJson(videoDirectory.resolve("${entry.videoId}.json"), json.encodeToString(VideoDetailDocument.serializer(), document))
            appendVideoRankingHistory(generatedAt, entry)
        }
    }

    fun writeGenerationSummary(
        generatedAt: String,
        inputVideos: Int,
        rankingVideos: Int,
        genreRankings: Int,
        collectionReport: CollectionReport,
        historyDeleted: Int,
        videoDetailsDeleted: Int,
    ) {
        val document = GenerationSummaryDocument(
            generatedAt = generatedAt,
            inputVideos = inputVideos,
            rankingVideos = rankingVideos,
            genreRankings = genreRankings,
            collection = collectionReport,
            retention = RetentionSummary(
                historyDeleted = historyDeleted,
                videoDetailsDeleted = videoDetailsDeleted,
            ),
        )
        writeJson(
            latestDirectory.resolve("generation-summary.json"),
            json.encodeToString(GenerationSummaryDocument.serializer(), document),
        )
    }

    private fun writeJson(path: Path, content: String) {
        path.parent?.createDirectories()
        val tmp = path.resolveSibling("${path.fileName}.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        java.nio.file.Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun writeHistory(overall: RankingDocument) {
        val generatedAt = OffsetDateTime.parse(overall.generatedAt)
        val historyFile = historyDirectory
            .resolve(generatedAt.format(DateTimeFormatter.ofPattern("yyyy")))
            .resolve(generatedAt.format(DateTimeFormatter.ofPattern("MM")))
            .resolve("${generatedAt.format(DateTimeFormatter.ofPattern("dd"))}.json")
        writeJson(historyFile, json.encodeToString(RankingDocument.serializer(), overall))
    }

    private fun writeToday(overall: RankingDocument) {
        val generatedAt = OffsetDateTime.parse(overall.generatedAt)
        val targetDate = generatedAt.toLocalDate()
        val entries = overall.ranking
            .map { entry -> entry.withTodayDelta(targetDate) }
            .filter { it.viewIncrease > 0 }
            .sortedWith(compareByDescending<RankingEntry> { it.viewIncrease }.thenByDescending { it.rawScore })
            .mapIndexed { index, entry ->
                entry.copy(
                    rank = index + 1,
                    previousRank = null,
                    rankChange = null,
                    normalizedScore = normalize(index, overall.ranking.size),
                )
            }
            .take(100)
        writeJson(
            latestDirectory.resolve("today.json"),
            json.encodeToString(RankingDocument.serializer(), RankingDocument(generatedAt = overall.generatedAt, period = "today", ranking = entries)),
        )
    }

    private fun RankingEntry.withTodayDelta(targetDate: java.time.LocalDate): RankingEntry {
        val statistics = loadVideoStatistics(videoId)
            .filter { OffsetDateTime.parse(it.capturedAt).toLocalDate() == targetDate }
            .sortedBy { it.capturedAt }
        return withStatisticDelta(statistics)
    }

    private fun loadVideoStatistics(videoId: String): List<com.ytranklab.statistics.VideoStatistic> {
        val file = videoStatisticsHistoryDirectory.resolve("$videoId.json")
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString(VideoStatisticsHistoryDocument.serializer(), file.readText()).statistics
        }.getOrDefault(emptyList())
    }

    private fun writeSevenDays(overall: RankingDocument) {
        val generatedAt = OffsetDateTime.parse(overall.generatedAt)
        val startAt = generatedAt.minusDays(7)
        val entries = overall.ranking
            .map { entry ->
                val statistics = loadVideoStatistics(entry.videoId)
                    .filter {
                        val capturedAt = OffsetDateTime.parse(it.capturedAt)
                        !capturedAt.isBefore(startAt) && !capturedAt.isAfter(generatedAt)
                    }
                    .sortedBy { it.capturedAt }
                entry.withStatisticDelta(statistics)
            }
            .sortedWith(compareByDescending<RankingEntry> { it.viewIncrease }.thenByDescending { it.rawScore })
            .mapIndexed { index, entry ->
                entry.copy(
                    rank = index + 1,
                    previousRank = null,
                    rankChange = null,
                    normalizedScore = normalize(index, overall.ranking.size),
                )
            }
            .take(100)

        writeJson(
            latestDirectory.resolve("seven-days.json"),
            json.encodeToString(RankingDocument.serializer(), RankingDocument(generatedAt = overall.generatedAt, period = "7d", ranking = entries)),
        )
    }

    private fun RankingEntry.withStatisticDelta(statistics: List<com.ytranklab.statistics.VideoStatistic>): RankingEntry {
        val first = statistics.firstOrNull()
        val last = statistics.lastOrNull()
        val viewDelta = if (first != null && last != null) {
            (last.viewCount - first.viewCount).coerceAtLeast(0)
        } else {
            0
        }
        val likeDelta = if (first?.likeCount != null && last?.likeCount != null) {
            (last.likeCount - first.likeCount).coerceAtLeast(0)
        } else {
            null
        }
        val commentDelta = if (first?.commentCount != null && last?.commentCount != null) {
            (last.commentCount - first.commentCount).coerceAtLeast(0)
        } else {
            null
        }
        val elapsedHours = if (first != null && last != null) {
            Duration.between(OffsetDateTime.parse(first.capturedAt), OffsetDateTime.parse(last.capturedAt)).toMinutes().toDouble() / 60.0
        } else {
            0.0
        }
        val velocity = if (elapsedHours > 0.0) viewDelta.toDouble() / elapsedHours else 0.0

        return copy(
            viewIncrease = viewDelta,
            likeIncrease = likeDelta,
            commentIncrease = commentDelta,
            rawScore = viewDelta.toDouble(),
            scoreBreakdown = scoreBreakdown.copy(velocity = velocity),
        )
    }

    private fun writeHistoryIndex() {
        val items = loadHistoryDocuments().map { document ->
            val generatedAt = OffsetDateTime.parse(document.generatedAt)
            HistoryIndexItem(
                date = generatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE),
                generatedAt = document.generatedAt,
                path = "history/${generatedAt.format(DateTimeFormatter.ofPattern("yyyy"))}/${generatedAt.format(DateTimeFormatter.ofPattern("MM"))}/${generatedAt.format(DateTimeFormatter.ofPattern("dd"))}.json",
                totalVideos = document.ranking.size,
            )
        }
        writeJson(
            latestDirectory.resolve("history-index.json"),
            json.encodeToString(HistoryIndexDocument.serializer(), HistoryIndexDocument(items = items)),
        )
    }

    private fun loadHistoryDocuments(): List<RankingDocument> {
        if (!historyDirectory.exists()) return emptyList()
        return historyDirectory.walk()
            .filter { it.isRegularFile() && it.toString().endsWith(".json") }
            .sortedBy { it.relativeTo(historyDirectory).toString() }
            .mapNotNull { file ->
                runCatching {
                    json.decodeFromString(RankingDocument.serializer(), file.readText())
                }.getOrNull()
            }
            .toList()
    }

    private fun normalize(index: Int, total: Int): Double {
        if (total <= 1) return 100.0
        return ((total - index).toDouble() / total.toDouble()) * 100.0
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
        writeJson(historyFile, json.encodeToString(VideoRankingHistoryDocument.serializer(), document))
    }
}

@Serializable
data class GenerationSummaryDocument(
    val generatedAt: String,
    val inputVideos: Int,
    val rankingVideos: Int,
    val genreRankings: Int,
    val collection: CollectionReport,
    val retention: RetentionSummary,
)

@Serializable
data class RetentionSummary(
    val historyDeleted: Int,
    val videoDetailsDeleted: Int,
)

@Serializable
data class VideoRankingHistoryDocument(
    val videoId: String,
    val rankings: List<VideoRankingHistoryItem>,
)

@Serializable
data class VideoRankingHistoryItem(
    val capturedAt: String,
    val rank: Int,
    val previousRank: Int? = null,
    val rankChange: Int? = null,
    val rawScore: Double,
    val normalizedScore: Double,
)

@Serializable
data class HistoryIndexDocument(
    val items: List<HistoryIndexItem>,
)

@Serializable
data class HistoryIndexItem(
    val date: String,
    val generatedAt: String,
    val path: String,
    val totalVideos: Int,
)
