package com.ytranklab.ranking

class ScoreSanitizer {
    fun safeScore(value: Double): Double =
        if (value.isFinite() && !value.isNaN()) value.coerceAtLeast(0.0) else 0.0
}
