package com.ytranklab.ranking

import com.ytranklab.config.GenreRankingConfig
import com.ytranklab.config.RankingConfig
import com.ytranklab.config.RankingWeights
import com.ytranklab.config.RetentionConfig
import com.ytranklab.domain.YouTubeVideo
import com.ytranklab.statistics.StatisticDelta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RankingCalculatorTest {
    private val config = RankingConfig(
        periodHours = 24,
        maxOverallItems = 100,
        maxGenreItems = 50,
        minimumSubscriberCount = 1000,
        unknownSubscriberCount = 50000,
        minimumViewIncrease = 1,
        ageDecayExponent = 0.6,
        maxLikeRate = 0.12,
        maxCommentRate = 0.03,
        weights = RankingWeights(35.0, 25.0, 20.0, 10.0),
        genreRanking = GenreRankingConfig(20, 5),
        retention = RetentionConfig(500, 90, 365),
    )

    @Test
    fun calculatesFiniteRawScore() {
        val video = YouTubeVideo(
            videoId = "v1",
            title = "Kotlin tutorial",
            channelId = "c1",
            channelName = "Code",
            youtubeCategoryId = "27",
            publishedAt = "2026-07-24T06:00:00+09:00",
            viewCount = 10000,
            likeCount = 900,
            commentCount = 120,
            subscriberCount = 5000,
        )

        val result = RankingCalculator(config).calculate(
            video = video,
            delta = StatisticDelta(
                viewIncrease = 8000,
                likeIncrease = 700,
                commentIncrease = 80,
                elapsedHours = 24.0,
                viewVelocity = 333.33,
            ),
            capturedAt = "2026-07-25T06:00:00+09:00",
        )

        assertTrue(result.rawScore > 0.0)
        assertTrue(result.rawScore.isFinite())
        assertTrue(result.breakdown.velocity > 0.0)
    }

    @Test
    fun avoidsDivisionByZeroAndNaN() {
        val video = YouTubeVideo(
            videoId = "v1",
            title = "",
            channelId = "c1",
            channelName = "",
            youtubeCategoryId = "20",
            publishedAt = "2026-07-25T06:00:00+09:00",
            viewCount = 0,
            likeCount = null,
            commentCount = null,
            subscriberCount = null,
        )

        val result = RankingCalculator(config).calculate(
            video = video,
            delta = StatisticDelta(0, null, null, 24.0, 0.0),
            capturedAt = "2026-07-25T06:00:00+09:00",
        )

        assertEquals(0.0, result.rawScore)
    }

    @Test
    fun ignoresCumulativeEngagementWhenVideoDidNotGrow() {
        val video = YouTubeVideo(
            videoId = "v1",
            title = "High engagement archive",
            channelId = "c1",
            channelName = "Channel",
            youtubeCategoryId = "24",
            publishedAt = "2026-07-25T00:00:00+09:00",
            viewCount = 1000,
            likeCount = 500,
            commentCount = 200,
            subscriberCount = 10000,
        )

        val result = RankingCalculator(config).calculate(
            video = video,
            delta = StatisticDelta(0, 0, 0, 6.0, 0.0),
            capturedAt = "2026-07-25T06:00:00+09:00",
        )

        assertEquals(0.0, result.rawScore)
        assertEquals(0.0, result.breakdown.engagement)
    }

    @Test
    fun capsExtremeDeltaEngagementRates() {
        val video = YouTubeVideo(
            videoId = "v1",
            title = "Burst",
            channelId = "c1",
            channelName = "Channel",
            youtubeCategoryId = "24",
            publishedAt = "2026-07-25T00:00:00+09:00",
            viewCount = 1000,
            likeCount = 900,
            commentCount = 800,
            subscriberCount = 10000,
        )

        val capped = RankingCalculator(config).calculate(
            video = video,
            delta = StatisticDelta(100, 1000, 1000, 1.0, 100.0),
            capturedAt = "2026-07-25T06:00:00+09:00",
        )
        val atCap = RankingCalculator(config).calculate(
            video = video,
            delta = StatisticDelta(100, 12, 3, 1.0, 100.0),
            capturedAt = "2026-07-25T06:00:00+09:00",
        )

        assertEquals(atCap.breakdown.engagement, capped.breakdown.engagement)
    }

    @Test
    fun appliesShortScoreMultiplierToShortVideos() {
        val longVideo = YouTubeVideo(
            videoId = "long",
            title = "Long form update",
            channelId = "c1",
            channelName = "Channel",
            youtubeCategoryId = "24",
            publishedAt = "2026-07-25T00:00:00+09:00",
            viewCount = 10000,
            likeCount = 900,
            commentCount = 120,
            subscriberCount = 10000,
            durationSeconds = 600,
        )
        val shortVideo = longVideo.copy(videoId = "short", title = "Short update #shorts", durationSeconds = 30)
        val delta = StatisticDelta(
            viewIncrease = 8000,
            likeIncrease = 700,
            commentIncrease = 80,
            elapsedHours = 24.0,
            viewVelocity = 333.33,
        )
        val calculator = RankingCalculator(config)

        val longResult = calculator.calculate(longVideo, delta, "2026-07-25T06:00:00+09:00")
        val shortResult = calculator.calculate(shortVideo, delta, "2026-07-25T06:00:00+09:00")

        assertEquals(round1(longResult.rawScore * config.shortScoreMultiplier), shortResult.rawScore)
        assertEquals(100.0, longResult.breakdown.formatAdjustment)
        assertEquals(85.0, shortResult.breakdown.formatAdjustment)
    }
}
