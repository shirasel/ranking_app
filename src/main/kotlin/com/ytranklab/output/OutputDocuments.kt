package com.ytranklab.output

import com.ytranklab.collection.CollectionReport
import kotlinx.serialization.Serializable

@Serializable
data class GenerationSummaryDocument(
    val generatedAt: String,
    val inputVideos: Int,
    val rankingVideos: Int,
    val genreRankings: Int,
    val collection: CollectionReport,
    val retention: RetentionSummary,
)

@Serializable
data class RetentionSummary(
    val historyDeleted: Int,
    val videoDetailsDeleted: Int,
)

@Serializable
data class VideoRankingHistoryDocument(
    val videoId: String,
    val rankings: List<VideoRankingHistoryItem>,
)

@Serializable
data class VideoRankingHistoryItem(
    val capturedAt: String,
    val rank: Int,
    val previousRank: Int? = null,
    val rankChange: Int? = null,
    val rawScore: Double,
    val normalizedScore: Double,
)

@Serializable
data class HistoryIndexDocument(
    val items: List<HistoryIndexItem>,
)

@Serializable
data class HistoryIndexItem(
    val date: String,
    val generatedAt: String,
    val path: String,
    val totalVideos: Int,
)
