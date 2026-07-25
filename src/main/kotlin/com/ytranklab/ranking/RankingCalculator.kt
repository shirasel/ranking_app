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
        val viewCount = video.viewCount.coerceAtLeast(0)
        val likeCount = video.likeCount?.coerceAtLeast(0) ?: 0
        val commentCount = video.commentCount?.coerceAtLeast(0) ?: 0
        val subscriberCount = (video.subscriberCount ?: config.minimumSubscriberCount).coerceAtLeast(config.minimumSubscriberCount)
        val publishedAgeHours = hoursBetween(video.publishedAt, capturedAt).coerceAtLeast(0.0)

        val viewVelocity = delta.viewVelocity.coerceAtLeast(0.0)
        val subscriberRatio = delta.viewIncrease.toDouble() / subscriberCount.toDouble()
        val likeRate = likeCount.toDouble() / viewCount.coerceAtLeast(1).toDouble()
        val commentRate = commentCount.toDouble() / viewCount.coerceAtLeast(1).toDouble()
        val ageDecay = 1.0 / (publishedAgeHours + 2.0).pow(config.ageDecayExponent)

        val velocityComponent = log10(viewVelocity + 1.0) * config.weights.velocity
        val subscriberComponent = log10(subscriberRatio * 10000.0 + 1.0) * config.weights.subscriberRatio
        val likeComponent = likeRate * 1000.0 * config.weights.likeRate
        val commentComponent = commentRate * 5000.0 * config.weights.commentRate
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
}

data class ScoreResult(
    val rawScore: Double,
    val breakdown: ScoreBreakdown,
)

fun round1(value: Double): Double = kotlin.math.round(value * 10.0) / 10.0
