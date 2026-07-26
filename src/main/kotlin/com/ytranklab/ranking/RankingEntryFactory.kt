package com.ytranklab.ranking

import com.ytranklab.app.RankingCandidate
import com.ytranklab.domain.RankingEntry

class RankingEntryFactory(private val normalizer: RankingNormalizer) {
    fun create(sorted: List<RankingCandidate>, previousRanks: Map<String, Int>): List<RankingEntry> {
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
                sevenDayViewIncrease = candidate.delta.sevenDayViewIncrease,
                likeCount = candidate.video.likeCount,
                likeIncrease = candidate.delta.likeIncrease,
                commentCount = candidate.video.commentCount,
                commentIncrease = candidate.delta.commentIncrease,
                subscriberCount = candidate.video.subscriberCount,
                durationSeconds = candidate.video.durationSeconds,
                isShort = candidate.video.isShortVideo(),
                rawScore = candidate.score.rawScore,
                normalizedScore = normalizer.normalize(rank, sorted.size),
                genres = candidate.genres,
                scoreBreakdown = candidate.score.breakdown,
            )
        }
    }
}

private fun com.ytranklab.domain.YouTubeVideo.isShortVideo(): Boolean {
    durationSeconds?.let { return it <= 60L }
    val text = "$title\n$description".lowercase()
    return text.contains("#shorts") || text.contains("#short") || text.contains("youtube shorts")
}
