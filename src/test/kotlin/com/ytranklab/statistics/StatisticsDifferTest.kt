package com.ytranklab.statistics

import com.ytranklab.domain.YouTubeVideo
import kotlin.test.Test
import kotlin.test.assertEquals

class StatisticsDifferTest {
    @Test
    fun calculatesNonNegativeDifferences() {
        val video = YouTubeVideo(
            videoId = "v1",
            title = "video",
            channelId = "c1",
            channelName = "channel",
            youtubeCategoryId = "20",
            publishedAt = "2026-07-24T06:00:00+09:00",
            viewCount = 80,
            likeCount = 9,
            commentCount = 2,
            subscriberCount = 1000,
        )
        val previous = VideoStatistic(
            videoId = "v1",
            capturedAt = "2026-07-24T06:00:00+09:00",
            viewCount = 100,
            likeCount = 10,
            commentCount = 3,
            subscriberCount = 1000,
        )

        val delta = StatisticsDiffer(24).calculate(video, previous, "2026-07-25T06:00:00+09:00")

        assertEquals(0, delta.viewIncrease)
        assertEquals(0, delta.likeIncrease)
        assertEquals(0, delta.commentIncrease)
        assertEquals(24.0, delta.elapsedHours)
    }
}
