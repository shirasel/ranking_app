package com.ytranklab.ranking

import com.ytranklab.app.RankingCandidate
import kotlin.math.ceil

class GenreDiversityBalancer(
    maxItems: Int,
    maxPrimaryGenreShare: Double,
) {
    private val maxPerPrimaryGenre = ceil(maxItems * maxPrimaryGenreShare.coerceIn(0.1, 1.0)).toInt().coerceAtLeast(1)

    fun balance(sortedCandidates: List<RankingCandidate>, maxItems: Int): List<RankingCandidate> {
        val selected = mutableListOf<RankingCandidate>()
        val deferred = mutableListOf<RankingCandidate>()
        val genreCounts = mutableMapOf<String, Int>()

        sortedCandidates.forEach { candidate ->
            if (selected.size >= maxItems) {
                deferred += candidate
                return@forEach
            }

            val genre = candidate.primaryGenreSlug()
            val count = genreCounts.getOrDefault(genre, 0)
            if (count < maxPerPrimaryGenre) {
                selected += candidate
                genreCounts[genre] = count + 1
            } else {
                deferred += candidate
            }
        }

        if (selected.size < maxItems) {
            selected += deferred.take(maxItems - selected.size)
        }
        return selected
    }

    private fun RankingCandidate.primaryGenreSlug(): String =
        genres.firstOrNull()?.slug ?: "uncategorized"
}
