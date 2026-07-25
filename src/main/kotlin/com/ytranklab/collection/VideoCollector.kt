package com.ytranklab.collection

import com.ytranklab.config.SourceConfig
import com.ytranklab.collection.reporting.CollectionReporter
import com.ytranklab.collection.reporting.SystemCollectionReporter
import com.ytranklab.youtube.YouTubeApiClient
import java.time.OffsetDateTime
import java.time.ZoneOffset

class VideoCollector(
    private val sourceConfig: SourceConfig,
    private val client: YouTubeApiClient,
    private val reporter: CollectionReporter = SystemCollectionReporter(),
    private val idValidator: YouTubeVideoIdValidator = YouTubeVideoIdValidator(),
    private val sourceTaskFactory: SourceTaskFactory = SourceTaskFactory(),
    private val quotaEstimator: CollectionQuotaEstimator = CollectionQuotaEstimator(),
) {
    suspend fun collect(): CollectedVideos {
        val videoIds = linkedSetOf<String>()
        val sourceResults = mutableListOf<SourceCollectionResult>()
        val quotaBudget = QuotaBudget(sourceConfig.collection.maxEstimatedQuotaUnits, sourceConfig.collection.reservedDetailQuotaUnits)
        val sourceFetchService = SourceFetchService(reporter)

        val manualVideoIds = sourceConfig.videos.filter { idValidator.isValid(it) }
        videoIds.addAll(manualVideoIds)
        sourceResults += SourceCollectionResult(
            source = "manual-videos",
            requested = sourceConfig.videos.size,
            collected = manualVideoIds.size,
            status = "ok",
        )

        sourceTaskFactory.create(sourceConfig, client).forEach { task ->
            if (!quotaBudget.trySpend(task.cost)) {
                sourceResults += skippedByQuota(task.sourceName, task.requested)
                return@forEach
            }
            val result = sourceFetchService.collect(task)
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
                estimatedQuotaUnits = quotaEstimator.estimate(sourceResults, limitedIds.size, videos.map { it.channelId }.distinct().size),
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
}
