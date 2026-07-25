package com.ytranklab.ranking

import com.ytranklab.config.RankingConfig
import com.ytranklab.domain.ScoreBreakdown
import com.ytranklab.domain.YouTubeVideo
import com.ytranklab.statistics.StatisticDelta
import com.ytranklab.statistics.hoursBetween
import kotlin.math.log10
import kotlin.math.pow

class RankingCalculator(private val config: RankingConfig) {
    fun calculate(video: YouTubeVideo, delta: StatisticDelta, capturedAt: String): ScoreResult {
        val subscriberCount = (video.subscriberCount ?: config.unknownSubscriberCount).coerceAtLeast(config.minimumSubscriberCount)
        val publishedAgeHours = hoursBetween(video.publishedAt, capturedAt).coerceAtLeast(0.0)

        val viewVelocity = delta.viewVelocity.coerceAtLeast(0.0)
        val viewIncrease = delta.viewIncrease.coerceAtLeast(0)
        val subscriberRatio = viewIncrease.toDouble() / subscriberCount.toDouble()
        val likeRate = rate(delta.likeIncrease ?: 0, viewIncrease, config.maxLikeRate)
        val commentRate = rate(delta.commentIncrease ?: 0, viewIncrease, config.maxCommentRate)
        val ageDecay = 1.0 / (publishedAgeHours + 2.0).pow(config.ageDecayExponent)

        val velocityComponent = log10(viewVelocity + 1.0) * config.weights.velocity
        val subscriberComponent = log10(subscriberRatio * 10000.0 + 1.0) * config.weights.subscriberRatio
        val likeComponent = log10(likeRate * 1000.0 + 1.0) * config.weights.likeRate
        val commentComponent = log10(commentRate * 5000.0 + 1.0) * config.weights.commentRate
        val rawScore = safeScore((velocityComponent + subscriberComponent + likeComponent + commentComponent) * ageDecay)

        return ScoreResult(
            rawScore = round1(rawScore),
            breakdown = ScoreBreakdown(
                velocity = round1(velocityComponent),
                engagement = round1(likeComponent + commentComponent),
                subscriberRatio = round1(subscriberComponent),
                freshness = round1(ageDecay * 100.0),
            ),
        )
    }

    private fun safeScore(value: Double): Double =
        if (value.isFinite() && !value.isNaN()) value.coerceAtLeast(0.0) else 0.0

    private fun rate(increase: Long, viewIncrease: Long, maxRate: Double): Double {
        if (viewIncrease <= 0) return 0.0
        return (increase.coerceAtLeast(0).toDouble() / viewIncrease.toDouble()).coerceIn(0.0, maxRate)
    }
}

data class ScoreResult(
    val rawScore: Double,
    val breakdown: ScoreBreakdown,
)

fun round1(value: Double): Double = kotlin.math.round(value * 10.0) / 10.0
