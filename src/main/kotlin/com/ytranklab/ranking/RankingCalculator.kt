package com.ytranklab.ranking

import com.ytranklab.config.RankingConfig
import com.ytranklab.domain.ScoreBreakdown
import com.ytranklab.domain.YouTubeVideo
import com.ytranklab.statistics.StatisticDelta

class RankingCalculator(
    config: RankingConfig,
    private val velocityCalculator: VelocityScoreCalculator = VelocityScoreCalculator(config.weights.velocity),
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
    fun calculate(video: YouTubeVideo, delta: StatisticDelta, capturedAt: String): ScoreResult {
        val velocityComponent = velocityCalculator.calculate(delta.viewVelocity)
        val subscriberComponent = subscriberRatioCalculator.calculate(delta.viewIncrease, video.subscriberCount)
        val engagementScore = engagementCalculator.calculate(
            EngagementDelta(
                viewIncrease = delta.viewIncrease,
                likeIncrease = delta.likeIncrease,
                commentIncrease = delta.commentIncrease,
            ),
        )
        val ageDecay = freshnessCalculator.calculate(video.publishedAt, capturedAt)
        val rawScore = sanitizer.safeScore(
            (velocityComponent + subscriberComponent + engagementScore.total) * ageDecay,
        )

        return ScoreResult(
            rawScore = round1(rawScore),
            breakdown = ScoreBreakdown(
                velocity = round1(velocityComponent),
                engagement = round1(engagementScore.total),
                subscriberRatio = round1(subscriberComponent),
                freshness = round1(ageDecay * 100.0),
            ),
        )
    }
}
