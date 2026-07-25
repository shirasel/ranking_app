package com.ytranklab.ranking

import com.ytranklab.statistics.hoursBetween
import kotlin.math.pow

class FreshnessScoreCalculator(private val ageDecayExponent: Double) {
    fun calculate(publishedAt: String, capturedAt: String): Double {
        val publishedAgeHours = hoursBetween(publishedAt, capturedAt).coerceAtLeast(0.0)
        return 1.0 / (publishedAgeHours + 2.0).pow(ageDecayExponent)
    }
}
