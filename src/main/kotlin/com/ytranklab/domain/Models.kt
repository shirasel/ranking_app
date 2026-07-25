package com.ytranklab.domain

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeVideo(
    val videoId: String,
    val title: String,
    val description: String = "",
    val channelId: String,
    val channelName: String,
    val youtubeCategoryId: String,
    val thumbnailUrl: String = "",
    val publishedAt: String,
    val viewCount: Long,
    val likeCount: Long? = null,
    val commentCount: Long? = null,
    val subscriberCount: Long? = null,
    val status: String = "public",
)

@Serializable
data class GenreScore(
    val slug: String,
    val name: String,
    val confidence: Double,
)

@Serializable
data class ScoreBreakdown(
    val velocity: Double,
    val engagement: Double,
    val subscriberRatio: Double,
    val freshness: Double,
)

@Serializable
data class RankingEntry(
    val rank: Int,
    val previousRank: Int? = null,
    val rankChange: Int? = null,
    val videoId: String,
    val title: String,
    val channelId: String,
    val channelName: String,
    val thumbnailUrl: String,
    val publishedAt: String,
    val viewCount: Long,
    val viewIncrease: Long,
    val likeCount: Long? = null,
    val likeIncrease: Long? = null,
    val commentCount: Long? = null,
    val commentIncrease: Long? = null,
    val subscriberCount: Long? = null,
    val rawScore: Double,
    val normalizedScore: Double,
    val genres: List<GenreScore>,
    val scoreBreakdown: ScoreBreakdown,
)

@Serializable
data class RankingDocument(
    val generatedAt: String,
    val period: String,
    val ranking: List<RankingEntry>,
)

@Serializable
data class GenreRankingDocument(
    val generatedAt: String,
    val period: String,
    val genre: GenreScore,
    val status: String,
    val totalVideos: Int,
    val totalChannels: Int,
    val ranking: List<RankingEntry>,
)

@Serializable
data class VideoDetailDocument(
    val generatedAt: String,
    val video: RankingEntry,
    val scoreBreakdown: ScoreBreakdown,
    val genres: List<GenreScore>,
)
