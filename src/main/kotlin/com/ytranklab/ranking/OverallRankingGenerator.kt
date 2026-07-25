package com.ytranklab.ranking

import com.ytranklab.app.RankingCandidate
import com.ytranklab.domain.RankingDocument

class OverallRankingGenerator(
    private val periodHours: Int,
    private val maxItems: Int,
    private val entryFactory: RankingEntryFactory,
    private val diversityBalancer: GenreDiversityBalancer,
) {
    fun generate(generatedAt: String, candidates: List<RankingCandidate>, previousRanks: Map<String, Int>): RankingDocument {
        val sortedCandidates = candidates.sortedByDescending { it.score.rawScore }
        val entries = entryFactory.create(diversityBalancer.balance(sortedCandidates, maxItems), previousRanks)
        return RankingDocument(generatedAt = generatedAt, period = "${periodHours}h", ranking = entries)
    }
}
