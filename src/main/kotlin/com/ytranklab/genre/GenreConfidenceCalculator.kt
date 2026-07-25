package com.ytranklab.genre

import kotlin.math.min
import kotlin.math.round

class GenreConfidenceCalculator(
    private val maxConfidence: Double = 0.99,
    private val scoreDivisor: Double = 10.0,
) {
    fun calculate(rawScore: Double): Double = round2(min(maxConfidence, rawScore / scoreDivisor))

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0
}
