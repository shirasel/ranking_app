package com.ytranklab.ranking

class RankingNormalizer {
    fun normalize(rank: Int, total: Int): Double {
        if (total <= 1) return 100.0
        val normalized = 100.0 * (1.0 - (rank - 1).toDouble() / (total - 1).toDouble())
        return round1(normalized.coerceIn(0.0, 100.0))
    }
}
