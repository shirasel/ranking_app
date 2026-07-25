package com.ytranklab.collection

import com.ytranklab.config.SourceConfig
import com.ytranklab.collection.reporting.CollectionReporter
import com.ytranklab.collection.reporting.SystemCollectionReporter
import com.ytranklab.domain.YouTubeVideo
import com.ytranklab.youtube.YouTubeApiClient
import com.ytranklab.youtube.YouTubeApiException
import java.time.OffsetDateTime
import java.time.ZoneOffset

class VideoCollector(
    private val sourceConfig: SourceConfig,
    private val client: YouTubeApiClient,
    private val reporter: CollectionReporter = SystemCollectionReporter(),
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

        val sourceTasks = buildSourceTasks()
            .sortedWith(compareBy<SourceTask> { it.priority }.thenBy { it.order })

        sourceTasks.forEach { task ->
            if (!quotaBudget.trySpend(task.cost)) {
                sourceResults += skippedByQuota(task.sourceName, task.requested)
                return@forEach
            }
            val result = collectSafely(task.sourceName) { task.fetch() }
            videoIds.addAll(result.videoIds)
            sourceResults += result.toSourceCollectionResult(requested = task.requested)
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

    private fun buildSourceTasks(): List<SourceTask> {
        val tasks = mutableListOf<SourceTask>()
        var order = 0

        sourceConfig.channels
            .filter { it.enabled }
            .forEach { channel ->
                tasks += SourceTask(
                    sourceName = "channel:${channel.id}",
                    requested = sourceConfig.collection.maxChannelVideos,
                    cost = CHANNEL_UPLOAD_COST,
                    priority = channel.priority,
                    order = order++,
                    fetch = { client.fetchLatestVideoIdsForChannel(channel.id, sourceConfig.collection.maxChannelVideos) },
                )
            }

        sourceConfig.keywords.forEach { keyword ->
            tasks += SourceTask(
                sourceName = "keyword:${keyword.term}",
                requested = sourceConfig.collection.maxSearchResultsPerKeyword,
                cost = SEARCH_COST,
                priority = keyword.priority,
                order = order++,
                fetch = { client.searchVideoIds(keyword.term, sourceConfig.collection.maxSearchResultsPerKeyword) },
            )
        }

        if (sourceConfig.collection.includePopularVideos) {
            tasks += SourceTask(
                sourceName = "popular:${sourceConfig.collection.regionCode}",
                requested = sourceConfig.collection.maxPopularVideos,
                cost = POPULAR_COST,
                priority = sourceConfig.collection.popularPriority,
                order = order++,
                fetch = {
                    client.fetchPopularVideoIds(
                        regionCode = sourceConfig.collection.regionCode,
                        maxResults = sourceConfig.collection.maxPopularVideos,
                    )
                },
            )
        }

        return tasks
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

    private suspend fun collectSafely(sourceName: String, fetch: suspend () -> List<String>): SourceFetchResult =
        try {
            SourceFetchResult(source = sourceName, videoIds = fetch(), status = "ok")
        } catch (error: YouTubeApiException) {
            val message = error.safeMessage()
            reporter.skippedSource(sourceName, message)
            SourceFetchResult(source = sourceName, videoIds = emptyList(), status = "skipped", message = message)
        }
}

private const val SEARCH_COST = 100
private const val POPULAR_COST = 1
private const val CHANNEL_UPLOAD_COST = 2

private data class SourceTask(
    val sourceName: String,
    val requested: Int,
    val cost: Int,
    val priority: Int,
    val order: Int,
    val fetch: suspend () -> List<String>,
)

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

private fun YouTubeApiException.safeMessage(): String =
    message
        ?.replace(Regex("key=[^&\\s]+"), "key=***")
        ?.take(180)
        ?: "request failed"
