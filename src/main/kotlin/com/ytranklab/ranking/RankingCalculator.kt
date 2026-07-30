package com.ytranklab.ranking

import com.ytranklab.config.RankingConfig
import com.ytranklab.domain.ScoreBreakdown
import com.ytranklab.domain.YouTubeVideo
import com.ytranklab.domain.isShortVideo
import com.ytranklab.statistics.StatisticDelta

class RankingCalculator(
    config: RankingConfig,
    private val velocityCalculator: VelocityScoreCalculator = VelocityScoreCalculator(config.weights.velocity),
    private val sevenDayVelocityCalculator: SevenDayVelocityScoreCalculator = SevenDayVelocityScoreCalculator(
        config.weights.sevenDayVelocity,
    ),
    private val subscriberRatioCalculator: SubscriberRatioScoreCalculator = SubscriberRatioScoreCalculator(
        weight = config.weights.subscriberRatio,
        minimumSubscriberCount = config.minimumSubscriberCount,
        unknownSubscriberCount = config.unknownSubscriberCount,
    ),
    private val engagementCalculator: EngagementScoreCalculator = EngagementScoreCalculator(
        likeWeight = config.weights.likeRate,
        commentWeight = config.weights.commentRate,
        maxLikeRate = config.maxLikeRate,
        maxCommentRate = config.maxCommentRate,
    ),
    private val freshnessCalculator: FreshnessScoreCalculator = FreshnessScoreCalculator(config.ageDecayExponent),
    private val sanitizer: ScoreSanitizer = ScoreSanitizer(),
) {
    private val shortScoreMultiplier = config.shortScoreMultiplier.coerceIn(0.0, 1.0)

    fun calculate(video: YouTubeVideo, delta: StatisticDelta, capturedAt: String): ScoreResult {
        val velocityComponent = velocityCalculator.calculate(delta.viewVelocity)
        val sevenDayVelocityComponent = sevenDayVelocityCalculator.calculate(delta.sevenDayViewVelocity)
        val subscriberComponent = subscriberRatioCalculator.calculate(delta.viewIncrease, video.subscriberCount)
        val engagementScore = engagementCalculator.calculate(
            EngagementDelta(
                viewIncrease = delta.viewIncrease,
                likeIncrease = delta.likeIncrease,
                commentIncrease = delta.commentIncrease,
            ),
        )
        val ageDecay = freshnessCalculator.calculate(video.publishedAt, capturedAt)
        val baseRawScore = sanitizer.safeScore(
            (velocityComponent + subscriberComponent + engagementScore.total) * ageDecay,
        ) + sevenDayVelocityComponent
        val formatMultiplier = if (video.isShortVideo()) shortScoreMultiplier else 1.0
        val rawScore = baseRawScore * formatMultiplier
        val safeRawScore = sanitizer.safeScore(
            rawScore,
        )

        return ScoreResult(
            rawScore = round1(safeRawScore),
            breakdown = ScoreBreakdown(
                velocity = round1(velocityComponent),
                engagement = round1(engagementScore.total),
                subscriberRatio = round1(subscriberComponent),
                freshness = round1(ageDecay * 100.0),
                sevenDayVelocity = round1(sevenDayVelocityComponent),
                formatAdjustment = round1(formatMultiplier * 100.0),
            ),
        )
    }
}
