package com.ytranklab.history

import com.ytranklab.config.DiversityConfig
import com.ytranklab.config.GenreRankingConfig
import com.ytranklab.config.RankingConfig
import com.ytranklab.config.RankingWeights
import com.ytranklab.config.RetentionConfig
import com.ytranklab.domain.GenreScore
import com.ytranklab.domain.RankingDocument
import com.ytranklab.domain.RankingEntry
import com.ytranklab.domain.ScoreBreakdown
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HistoryRankingRescorerTest {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun appliesCurrentShortAdjustmentAndReordersHistoryRanking() {
        val dataDirectory = createTempDirectory("history-rescore-test")
        val historyFile = dataDirectory.resolve("history/2026/07/30.json")
        historyFile.parent.createDirectories()
        historyFile.writeText(
            json.encodeToString(
                RankingDocument(
                    generatedAt = "2026-07-30T00:00:00Z",
                    period = "24h",
                    ranking = listOf(
                        entry(rank = 1, videoId = "short", rawScore = 100.0, isShort = true),
                        entry(rank = 2, videoId = "long", rawScore = 90.0, isShort = false),
                    ),
                ),
            ),
        )

        val result = HistoryRankingRescorer(dataDirectory, rankingConfig(shortScoreMultiplier = 0.85)).rescore()
        val document = json.decodeFromString(RankingDocument.serializer(), historyFile.readText())

        assertEquals(1, result.scannedFiles)
        assertEquals(1, result.updatedFiles)
        assertEquals("long", document.ranking[0].videoId)
        assertEquals(90.0, document.ranking[0].rawScore)
        assertEquals(100.0, document.ranking[0].scoreBreakdown.formatAdjustment)
        assertEquals("short", document.ranking[1].videoId)
        assertEquals(85.0, document.ranking[1].rawScore)
        assertEquals(85.0, document.ranking[1].scoreBreakdown.formatAdjustment)
        assertEquals(1, document.ranking[0].rank)
        assertEquals(2, document.ranking[1].rank)
    }

    private fun rankingConfig(shortScoreMultiplier: Double): RankingConfig =
        RankingConfig(
            periodHours = 24,
            maxOverallItems = 100,
            maxGenreItems = 50,
            minimumSubscriberCount = 1000,
            unknownSubscriberCount = 50000,
            minimumViewIncrease = 1,
            ageDecayExponent = 0.6,
            maxLikeRate = 0.12,
            maxCommentRate = 0.03,
            shortScoreMultiplier = shortScoreMultiplier,
            weights = RankingWeights(35.0, 25.0, 20.0, 10.0),
            genreRanking = GenreRankingConfig(20, 5),
            diversity = DiversityConfig(0.4),
            retention = RetentionConfig(500, 90, 365),
        )

    private fun entry(rank: Int, videoId: String, rawScore: Double, isShort: Boolean): RankingEntry =
        RankingEntry(
            rank = rank,
            videoId = videoId,
            title = videoId,
            channelId = "channel-$videoId",
            channelName = "Channel",
            thumbnailUrl = "",
            publishedAt = "2026-07-30T00:00:00Z",
            viewCount = 1000,
            viewIncrease = 100,
            durationSeconds = if (isShort) 30 else 600,
            isShort = isShort,
            rawScore = rawScore,
            normalizedScore = if (rank == 1) 100.0 else 0.0,
            genres = listOf(GenreScore("gaming", "ゲーム", 0.9)),
            scoreBreakdown = ScoreBreakdown(
                velocity = 50.0,
                engagement = 20.0,
                subscriberRatio = 20.0,
                freshness = 100.0,
            ),
        )
}
