package com.ytranklab.ranking

import com.ytranklab.app.RankingCandidate
import com.ytranklab.domain.RankingDocument

class DiscoveryRankingGenerator(
    private val periodHours: Int,
    private val maxItems: Int,
    private val minimumSubscriberCount: Long,
    private val unknownSubscriberCount: Long,
    private val entryFactory: RankingEntryFactory,
) {
    fun generate(generatedAt: String, candidates: List<RankingCandidate>, previousRanks: Map<String, Int>): RankingDocument {
        val entries = entryFactory.create(
            candidates.sortedWith(
                compareByDescending<RankingCandidate> {
                    val subscribers = it.video.subscriberCount ?: unknownSubscriberCount
                    it.delta.viewIncrease.toDouble() / subscribers.coerceAtLeast(minimumSubscriberCount).toDouble()
                }.thenByDescending { it.score.rawScore },
            ),
            previousRanks,
        ).take(maxItems)
        return RankingDocument(generatedAt = generatedAt, period = "${periodHours}h", ranking = entries)
    }
}
