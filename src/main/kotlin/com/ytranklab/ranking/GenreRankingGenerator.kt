package com.ytranklab.ranking

import com.ytranklab.app.RankingCandidate
import com.ytranklab.config.GenreRankingConfig
import com.ytranklab.domain.GenreRankingDocument

class GenreRankingGenerator(
    private val periodHours: Int,
    private val maxItems: Int,
    private val genreRankingConfig: GenreRankingConfig,
    private val entryFactory: RankingEntryFactory,
) {
    fun generate(
        generatedAt: String,
        candidates: List<RankingCandidate>,
        previousRanks: Map<String, Int>,
    ): Map<String, GenreRankingDocument> {
        val allGenres = candidates.flatMap { candidate ->
            candidate.genres.map { genre -> genre.slug to genre }
        }.distinctBy { it.first }.map { it.second }

        return allGenres.associate { genre ->
            val genreCandidates = candidates.filter { candidate -> candidate.genres.any { it.slug == genre.slug } }
            val totalChannels = genreCandidates.map { it.video.channelId }.distinct().size
            val status = if (
                genreCandidates.size >= genreRankingConfig.minimumVideos &&
                totalChannels >= genreRankingConfig.minimumChannels
            ) {
                "official"
            } else {
                "reference"
            }
            val entries = entryFactory.create(genreCandidates.sortedByDescending { it.score.rawScore }, previousRanks).take(maxItems)
            genre.slug to GenreRankingDocument(
                generatedAt = generatedAt,
                period = "${periodHours}h",
                genre = genre,
                status = status,
                totalVideos = genreCandidates.size,
                totalChannels = totalChannels,
                ranking = entries,
            )
        }
    }
}
