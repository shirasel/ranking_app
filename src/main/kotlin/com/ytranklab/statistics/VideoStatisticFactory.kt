package com.ytranklab.statistics

import com.ytranklab.domain.YouTubeVideo

class VideoStatisticFactory {
    fun create(capturedAt: String, video: YouTubeVideo): VideoStatistic =
        VideoStatistic(
            videoId = video.videoId,
            capturedAt = capturedAt,
            viewCount = video.viewCount,
            likeCount = video.likeCount,
            commentCount = video.commentCount,
            subscriberCount = video.subscriberCount,
        )
}
