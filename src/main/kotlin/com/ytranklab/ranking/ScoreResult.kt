package com.ytranklab.ranking

import com.ytranklab.domain.ScoreBreakdown

data class ScoreResult(
    val rawScore: Double,
    val breakdown: ScoreBreakdown,
)

fun round1(value: Double): Double = kotlin.math.round(value * 10.0) / 10.0
