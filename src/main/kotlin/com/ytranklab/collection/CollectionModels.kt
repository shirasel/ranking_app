package com.ytranklab.collection

import com.ytranklab.domain.YouTubeVideo

data class CollectedVideos(
    val capturedAt: String,
    val videos: List<YouTubeVideo>,
    val report: CollectionReport,
)

@kotlinx.serialization.Serializable
data class CollectionReport(
    val sourceResults: List<SourceCollectionResult>,
    val uniqueCandidateIds: Int,
    val fetchedVideoIds: Int,
    val publicVideos: Int,
    val estimatedQuotaUnits: Int,
)

@kotlinx.serialization.Serializable
data class SourceCollectionResult(
    val source: String,
    val requested: Int,
    val collected: Int,
    val status: String,
    val message: String? = null,
)

internal data class SourceTask(
    val sourceName: String,
    val requested: Int,
    val cost: Int,
    val priority: Int,
    val order: Int,
    val fetch: suspend () -> List<String>,
)

internal data class SourceFetchResult(
    val source: String,
    val videoIds: List<String>,
    val status: String,
    val message: String? = null,
) {
    fun toSourceCollectionResult(requested: Int): SourceCollectionResult =
        SourceCollectionResult(
            source = source,
            requested = requested,
            collected = videoIds.size,
            status = status,
            message = message,
        )
}
