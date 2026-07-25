package com.ytranklab.ranking

import kotlin.math.log10

class VelocityScoreCalculator(private val weight: Double) {
    fun calculate(viewVelocity: Double): Double =
        log10(viewVelocity.coerceAtLeast(0.0) + 1.0) * weight
}
