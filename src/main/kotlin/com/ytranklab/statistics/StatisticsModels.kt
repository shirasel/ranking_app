package com.ytranklab.statistics

import kotlinx.serialization.Serializable

@Serializable
data class StatisticsDocument(
    val capturedAt: String,
    val statistics: List<VideoStatistic>,
)

@Serializable
data class VideoStatistic(
    val videoId: String,
    val capturedAt: String,
    val viewCount: Long,
    val likeCount: Long? = null,
    val commentCount: Long? = null,
    val subscriberCount: Long? = null,
)

@Serializable
data class VideoStatisticsHistoryDocument(
    val videoId: String,
    val statistics: List<VideoStatistic>,
)
