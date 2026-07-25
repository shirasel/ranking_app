package com.ytranklab.ranking

import com.ytranklab.app.RankingCandidate
import com.ytranklab.config.RankingConfig
import com.ytranklab.domain.GenreRankingDocument
import com.ytranklab.domain.GenreScore
import com.ytranklab.domain.RankingDocument
import com.ytranklab.domain.RankingEntry

class RankingGenerator(
    private val config: RankingConfig,
    private val normalizer: RankingNormalizer,
) {
    fun generateOverall(
        generatedAt: String,
        candidates: List<RankingCandidate>,
        previousRanks: Map<String, Int>,
    ): RankingDocument {
        val entries = toEntries(candidates.sortedByDescending { it.score.rawScore }, previousRanks)
            .take(config.maxOverallItems)
        return RankingDocument(generatedAt = generatedAt, period = "${config.periodHours}h", ranking = entries)
    }

    fun generateGenres(
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
                genreCandidates.size >= config.genreRanking.minimumVideos &&
                totalChannels >= config.genreRanking.minimumChannels
            ) {
                "official"
            } else {
                "reference"
            }
            val entries = toEntries(genreCandidates.sortedByDescending { it.score.rawScore }, previousRanks).take(config.maxGenreItems)
            genre.slug to GenreRankingDocument(
                generatedAt = generatedAt,
                period = "${config.periodHours}h",
                genre = genre,
                status = status,
                totalVideos = genreCandidates.size,
                totalChannels = totalChannels,
                ranking = entries,
            )
        }
    }

    fun generateTrending(
        generatedAt: String,
        candidates: List<RankingCandidate>,
        previousRanks: Map<String, Int>,
    ): RankingDocument {
        val entries = toEntries(candidates.sortedByDescending { it.delta.viewVelocity }, previousRanks)
            .take(config.maxOverallItems)
        return RankingDocument(generatedAt = generatedAt, period = "${config.periodHours}h", ranking = entries)
    }

    fun generateDiscovery(
        generatedAt: String,
        candidates: List<RankingCandidate>,
        previousRanks: Map<String, Int>,
    ): RankingDocument {
        val entries = toEntries(
            candidates.sortedWith(
                compareByDescending<RankingCandidate> {
                    val subscribers = it.video.subscriberCount ?: config.minimumSubscriberCount
                    it.delta.viewIncrease.toDouble() / subscribers.coerceAtLeast(config.minimumSubscriberCount).toDouble()
                }.thenByDescending { it.score.rawScore },
            ),
            previousRanks,
        ).take(config.maxOverallItems)
        return RankingDocument(generatedAt = generatedAt, period = "${config.periodHours}h", ranking = entries)
    }

    private fun toEntries(sorted: List<RankingCandidate>, previousRanks: Map<String, Int>): List<RankingEntry> {
        var lastScore: Double? = null
        var lastRank = 0

        return sorted.mapIndexed { index, candidate ->
            val rank = if (lastScore == candidate.score.rawScore) lastRank else index + 1
            lastScore = candidate.score.rawScore
            lastRank = rank
            val previousRank = previousRanks[candidate.video.videoId]
            RankingEntry(
                rank = rank,
                previousRank = previousRank,
                rankChange = previousRank?.minus(rank),
                videoId = candidate.video.videoId,
                title = candidate.video.title,
                channelId = candidate.video.channelId,
                channelName = candidate.video.channelName,
                thumbnailUrl = candidate.video.thumbnailUrl,
                publishedAt = candidate.video.publishedAt,
                viewCount = candidate.video.viewCount,
                viewIncrease = candidate.delta.viewIncrease,
                likeCount = candidate.video.likeCount,
                likeIncrease = candidate.delta.likeIncrease,
                commentCount = candidate.video.commentCount,
                commentIncrease = candidate.delta.commentIncrease,
                subscriberCount = candidate.video.subscriberCount,
                rawScore = candidate.score.rawScore,
                normalizedScore = normalizer.normalize(rank, sorted.size),
                genres = candidate.genres,
                scoreBreakdown = candidate.score.breakdown,
            )
        }
    }
}
