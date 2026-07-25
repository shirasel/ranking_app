package com.ytranklab.collection

import com.ytranklab.config.SourceConfig
import com.ytranklab.domain.YouTubeVideo
import com.ytranklab.youtube.YouTubeApiClient
import com.ytranklab.youtube.YouTubeApiException
import java.time.OffsetDateTime
import java.time.ZoneOffset

class VideoCollector(
    private val sourceConfig: SourceConfig,
    private val client: YouTubeApiClient,
) {
    suspend fun collect(): CollectedVideos {
        val videoIds = linkedSetOf<String>()
        val sourceResults = mutableListOf<SourceCollectionResult>()
        val quotaBudget = QuotaBudget(sourceConfig.collection.maxEstimatedQuotaUnits, sourceConfig.collection.reservedDetailQuotaUnits)

        val manualVideoIds = sourceConfig.videos.filter { it.isValidYouTubeId() }
        videoIds.addAll(manualVideoIds)
        sourceResults += SourceCollectionResult(
            source = "manual-videos",
            requested = sourceConfig.videos.size,
            collected = manualVideoIds.size,
            status = "ok",
        )

        sourceConfig.channels
            .filter { it.enabled }
            .forEach { channel ->
                if (!quotaBudget.trySpend(CHANNEL_UPLOAD_COST)) {
                    sourceResults += skippedByQuota("channel:${channel.id}", sourceConfig.collection.maxChannelVideos)
                    return@forEach
                }
                val result = collectSafely("channel:${channel.id}") {
                    client.fetchLatestVideoIdsForChannel(channel.id, sourceConfig.collection.maxChannelVideos)
                }
                videoIds.addAll(result.videoIds)
                sourceResults += result.toSourceCollectionResult(requested = sourceConfig.collection.maxChannelVideos)
            }

        sourceConfig.keywords.forEach { keyword ->
            if (!quotaBudget.trySpend(SEARCH_COST)) {
                sourceResults += skippedByQuota("keyword:$keyword", sourceConfig.collection.maxSearchResultsPerKeyword)
                return@forEach
            }
            val result = collectSafely("keyword:$keyword") {
                client.searchVideoIds(keyword, sourceConfig.collection.maxSearchResultsPerKeyword)
            }
            videoIds.addAll(result.videoIds)
            sourceResults += result.toSourceCollectionResult(requested = sourceConfig.collection.maxSearchResultsPerKeyword)
        }

        if (sourceConfig.collection.includePopularVideos) {
            if (!quotaBudget.trySpend(POPULAR_COST)) {
                sourceResults += skippedByQuota("popular:${sourceConfig.collection.regionCode}", sourceConfig.collection.maxPopularVideos)
            } else {
                val result = collectSafely("popular:${sourceConfig.collection.regionCode}") {
                    client.fetchPopularVideoIds(
                        regionCode = sourceConfig.collection.regionCode,
                        maxResults = sourceConfig.collection.maxPopularVideos,
                    )
                }
                videoIds.addAll(result.videoIds)
                sourceResults += result.toSourceCollectionResult(requested = sourceConfig.collection.maxPopularVideos)
            }
        }

        val limitedIds = videoIds.take(sourceConfig.collection.maxVideos)
        val videos = client.fetchVideos(limitedIds)
            .filter { it.status == "public" }
            .take(sourceConfig.collection.maxVideos)

        return CollectedVideos(
            capturedAt = OffsetDateTime.now(ZoneOffset.UTC).toString(),
            videos = videos,
            report = CollectionReport(
                sourceResults = sourceResults,
                uniqueCandidateIds = videoIds.size,
                fetchedVideoIds = limitedIds.size,
                publicVideos = videos.size,
                estimatedQuotaUnits = estimateQuotaUnits(sourceResults, limitedIds.size, videos.map { it.channelId }.distinct().size),
            ),
        )
    }

    private fun skippedByQuota(source: String, requested: Int): SourceCollectionResult =
        SourceCollectionResult(
            source = source,
            requested = requested,
            collected = 0,
            status = "skipped",
            message = "quota budget limit",
        )

    private fun estimateQuotaUnits(
        sourceResults: List<SourceCollectionResult>,
        fetchedVideoIds: Int,
        uniqueChannels: Int,
    ): Int {
        val searchUnits = sourceResults.count { it.source.startsWith("keyword:") && it.status == "ok" } * 100
        val popularUnits = sourceResults.count { it.source.startsWith("popular:") && it.status == "ok" }
        val channelUnits = sourceResults.count { it.source.startsWith("channel:") && it.status == "ok" }
        val playlistUnits = channelUnits
        val videoUnits = if (fetchedVideoIds > 0) ((fetchedVideoIds - 1) / 50) + 1 else 0
        val subscriberChannelUnits = if (uniqueChannels > 0) ((uniqueChannels - 1) / 50) + 1 else 0
        return searchUnits + popularUnits + channelUnits + playlistUnits + videoUnits + subscriberChannelUnits
    }
}

private const val SEARCH_COST = 100
private const val POPULAR_COST = 1
private const val CHANNEL_UPLOAD_COST = 2

private class QuotaBudget(
    private val maxEstimatedQuotaUnits: Int,
    private val reservedDetailQuotaUnits: Int,
) {
    private var spent = 0

    fun trySpend(cost: Int): Boolean {
        if (maxEstimatedQuotaUnits <= 0) {
            spent += cost
            return true
        }
        if (spent + cost + reservedDetailQuotaUnits > maxEstimatedQuotaUnits) return false
        spent += cost
        return true
    }
}

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

private data class SourceFetchResult(
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

private fun String.isValidYouTubeId(): Boolean = matches(Regex("^[A-Za-z0-9_-]{6,64}$"))

private suspend fun collectSafely(sourceName: String, fetch: suspend () -> List<String>): SourceFetchResult =
    try {
        SourceFetchResult(source = sourceName, videoIds = fetch(), status = "ok")
    } catch (error: YouTubeApiException) {
        val message = error.safeMessage()
        System.err.println("Skipped YouTube source '$sourceName': $message")
        SourceFetchResult(source = sourceName, videoIds = emptyList(), status = "skipped", message = message)
    }

private fun YouTubeApiException.safeMessage(): String =
    message
        ?.replace(Regex("key=[^&\\s]+"), "key=***")
        ?.take(180)
        ?: "request failed"
