package com.ytranklab.config

data class RankingConfig(
    val periodHours: Int,
    val maxOverallItems: Int,
    val maxGenreItems: Int,
    val minimumSubscriberCount: Long,
    val unknownSubscriberCount: Long,
    val minimumViewIncrease: Long,
    val ageDecayExponent: Double,
    val maxLikeRate: Double,
    val maxCommentRate: Double,
    val weights: RankingWeights,
    val genreRanking: GenreRankingConfig,
    val retention: RetentionConfig,
)

data class RankingWeights(
    val velocity: Double,
    val subscriberRatio: Double,
    val likeRate: Double,
    val commentRate: Double,
)

data class GenreRankingConfig(
    val minimumVideos: Int,
    val minimumChannels: Int,
)

data class RetentionConfig(
    val maxTrackedVideos: Int,
    val detailedStatisticsDays: Int,
    val rankingHistoryDays: Int,
)

data class GenreRule(
    val slug: String,
    val name: String,
    val parent: String?,
    val visible: Boolean = true,
    val titleKeywords: Map<String, Double>,
    val descriptionKeywords: Map<String, Double>,
    val youtubeCategoryIds: List<String>,
)

data class SourceConfig(
    val channels: List<SourceChannel>,
    val keywords: List<SourceKeyword>,
    val videos: List<String>,
    val collection: CollectionConfig,
)

data class SourceChannel(
    val id: String,
    val enabled: Boolean,
    val priority: Int = 100,
)

data class SourceKeyword(
    val term: String,
    val priority: Int = 200,
)

data class CollectionConfig(
    val maxVideos: Int,
    val maxSearchResultsPerKeyword: Int,
    val includePopularVideos: Boolean,
    val regionCode: String,
    val maxPopularVideos: Int,
    val maxChannelVideos: Int,
    val maxEstimatedQuotaUnits: Int,
    val reservedDetailQuotaUnits: Int,
    val popularPriority: Int,
)
