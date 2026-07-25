package com.ytranklab.ranking

import kotlin.math.log10

class EngagementScoreCalculator(
    private val likeWeight: Double,
    private val commentWeight: Double,
    private val maxLikeRate: Double,
    private val maxCommentRate: Double,
) {
    fun calculate(delta: EngagementDelta): EngagementScore {
        val viewIncrease = delta.viewIncrease.coerceAtLeast(0)
        val likeRate = rate(delta.likeIncrease ?: 0, viewIncrease, maxLikeRate)
        val commentRate = rate(delta.commentIncrease ?: 0, viewIncrease, maxCommentRate)
        val likeComponent = log10(likeRate * 1000.0 + 1.0) * likeWeight
        val commentComponent = log10(commentRate * 5000.0 + 1.0) * commentWeight
        return EngagementScore(likeComponent = likeComponent, commentComponent = commentComponent)
    }

    private fun rate(increase: Long, viewIncrease: Long, maxRate: Double): Double {
        if (viewIncrease <= 0) return 0.0
        return (increase.coerceAtLeast(0).toDouble() / viewIncrease.toDouble()).coerceIn(0.0, maxRate)
    }
}

data class EngagementDelta(
    val viewIncrease: Long,
    val likeIncrease: Long?,
    val commentIncrease: Long?,
)

data class EngagementScore(
    val likeComponent: Double,
    val commentComponent: Double,
) {
    val total: Double = likeComponent + commentComponent
}
