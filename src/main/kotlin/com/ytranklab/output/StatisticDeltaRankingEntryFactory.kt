package com.ytranklab.output

import com.ytranklab.domain.RankingEntry
import com.ytranklab.statistics.VideoStatistic
import java.time.Duration
import java.time.OffsetDateTime

class StatisticDeltaRankingEntryFactory {
    fun withStatisticDelta(entry: RankingEntry, statistics: List<VideoStatistic>): RankingEntry {
        val first = statistics.firstOrNull()
        val last = statistics.lastOrNull()
        val viewDelta = if (first != null && last != null) {
            (last.viewCount - first.viewCount).coerceAtLeast(0)
        } else {
            0
        }
        val likeDelta = if (first?.likeCount != null && last?.likeCount != null) {
            (last.likeCount - first.likeCount).coerceAtLeast(0)
        } else {
            null
        }
        val commentDelta = if (first?.commentCount != null && last?.commentCount != null) {
            (last.commentCount - first.commentCount).coerceAtLeast(0)
        } else {
            null
        }
        val elapsedHours = if (first != null && last != null) {
            Duration.between(OffsetDateTime.parse(first.capturedAt), OffsetDateTime.parse(last.capturedAt)).toMinutes().toDouble() / 60.0
        } else {
            0.0
        }
        val velocity = if (elapsedHours > 0.0) viewDelta.toDouble() / elapsedHours else 0.0

        return entry.copy(
            viewIncrease = viewDelta,
            likeIncrease = likeDelta,
            commentIncrease = commentDelta,
            rawScore = viewDelta.toDouble(),
            scoreBreakdown = entry.scoreBreakdown.copy(velocity = velocity),
        )
    }
}
