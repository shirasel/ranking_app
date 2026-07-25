package com.ytranklab.ranking

import com.ytranklab.app.RankingCandidate
import com.ytranklab.domain.RankingDocument

class TrendingRankingGenerator(
    private val periodHours: Int,
    private val maxItems: Int,
    private val entryFactory: RankingEntryFactory,
) {
    fun generate(generatedAt: String, candidates: List<RankingCandidate>, previousRanks: Map<String, Int>): RankingDocument {
        val entries = entryFactory.create(candidates.sortedByDescending { it.delta.viewVelocity }, previousRanks)
            .take(maxItems)
        return RankingDocument(generatedAt = generatedAt, period = "${periodHours}h", ranking = entries)
    }
}
