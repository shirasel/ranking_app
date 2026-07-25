package com.ytranklab.ranking

import kotlin.math.log10

class SubscriberRatioScoreCalculator(
    private val weight: Double,
    private val minimumSubscriberCount: Long,
    private val unknownSubscriberCount: Long,
) {
    fun calculate(viewIncrease: Long, subscriberCount: Long?): Double {
        val safeSubscriberCount = (subscriberCount ?: unknownSubscriberCount).coerceAtLeast(minimumSubscriberCount)
        val safeViewIncrease = viewIncrease.coerceAtLeast(0)
        val subscriberRatio = safeViewIncrease.toDouble() / safeSubscriberCount.toDouble()
        return log10(subscriberRatio * 10000.0 + 1.0) * weight
    }
}
