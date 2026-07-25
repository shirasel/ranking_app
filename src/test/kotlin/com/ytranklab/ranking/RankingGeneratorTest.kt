package com.ytranklab.ranking

import com.ytranklab.app.RankingCandidate
import com.ytranklab.config.GenreRankingConfig
import com.ytranklab.config.RankingConfig
import com.ytranklab.config.RankingWeights
import com.ytranklab.config.RetentionConfig
import com.ytranklab.domain.GenreScore
import com.ytranklab.domain.ScoreBreakdown
import com.ytranklab.domain.YouTubeVideo
import com.ytranklab.statistics.StatisticDelta
import kotlin.test.Test
import kotlin.test.assertEquals

class RankingGeneratorTest {
    private val config = RankingConfig(
        periodHours = 24,
        maxOverallItems = 100,
        maxGenreItems = 50,
        minimumSubscriberCount = 1000,
        ageDecayExponent = 0.6,
        weights = RankingWeights(35.0, 25.0, 20.0, 10.0),
        genreRanking = GenreRankingConfig(20, 5),
        retention = RetentionConfig(500, 90, 365),
    )

    @Test
    fun generatesTieRanksAndRankChanges() {
        val generator = RankingGenerator(config, RankingNormalizer())
        val ranking = generator.generateOverall(
            generatedAt = "2026-07-25T06:00:00+09:00",
            candidates = listOf(candidate("a", 10.0), candidate("b", 10.0), candidate("c", 5.0)),
            previousRanks = mapOf("a" to 3, "b" to 1),
        )

        assertEquals(1, ranking.ranking[0].rank)
        assertEquals(1, ranking.ranking[1].rank)
        assertEquals(3, ranking.ranking[2].rank)
        assertEquals(2, ranking.ranking[0].rankChange)
        assertEquals(0, ranking.ranking[1].rankChange)
    }

    private fun candidate(id: String, score: Double): RankingCandidate =
        RankingCandidate(
            video = YouTubeVideo(
                videoId = id,
                title = id,
                channelId = "channel-$id",
                channelName = "channel",
                youtubeCategoryId = "20",
                publishedAt = "2026-07-25T00:00:00+09:00",
                viewCount = 100,
            ),
            delta = StatisticDelta(10, 1, 1, 24.0, 1.0),
            genres = listOf(GenreScore("gaming", "ゲーム", 0.9)),
            score = ScoreResult(score, ScoreBreakdown(1.0, 1.0, 1.0, 1.0)),
        )
}
