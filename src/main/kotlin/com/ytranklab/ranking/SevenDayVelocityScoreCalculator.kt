package com.ytranklab.ranking

import kotlin.math.log10

class SevenDayVelocityScoreCalculator(private val weight: Double) {
    fun calculate(sevenDayViewVelocity: Double?): Double {
        val velocity = sevenDayViewVelocity?.takeIf { it > 0.0 } ?: return 0.0
        return (log10(velocity + 1.0) * weight).coerceAtMost(weight)
    }
}
