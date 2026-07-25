package com.ytranklab.app.generation

import com.ytranklab.app.reporting.GenerationReporter
import com.ytranklab.app.reporting.RetentionResultSummary
import com.ytranklab.collection.CollectionReport
import com.ytranklab.domain.YouTubeVideo
import com.ytranklab.history.HistoryRetentionService
import com.ytranklab.output.RankingJsonWriter
import com.ytranklab.statistics.StatisticsRepository

class RankingGenerationPersistence(
    private val statisticsRepository: StatisticsRepository,
    private val writer: RankingJsonWriter,
    private val retentionService: HistoryRetentionService,
    private val reporter: GenerationReporter,
) {
    fun persist(
        capturedAt: String,
        videos: List<YouTubeVideo>,
        documents: RankingDocumentSet,
        collectionReport: CollectionReport,
    ): GenerateResult {
        statisticsRepository.saveLatest(capturedAt, videos)
        writer.writeAll(documents.overall, documents.genres, documents.trending, documents.discovery)
        writer.writeVideoDetails(documents.overall.ranking, capturedAt)

        val retentionResult = retentionService.cleanup(
            generatedAt = capturedAt,
            activeVideoIds = documents.overall.ranking.map { it.videoId }.toSet(),
        )

        writer.writeGenerationSummary(
            generatedAt = capturedAt,
            inputVideos = videos.size,
            rankingVideos = documents.overall.ranking.size,
            genreRankings = documents.genres.size,
            collectionReport = collectionReport,
            historyDeleted = retentionResult.historyDeleted,
            videoDetailsDeleted = retentionResult.videoDetailsDeleted,
        )

        reporter.report(
            inputVideos = videos.size,
            rankingVideos = documents.overall.ranking.size,
            collectionReport = collectionReport,
            retentionResult = RetentionResultSummary(
                historyDeleted = retentionResult.historyDeleted,
                videoDetailsDeleted = retentionResult.videoDetailsDeleted,
            ),
        )

        return GenerateResult(
            overallCount = documents.overall.ranking.size,
            genreCount = documents.genres.size,
        )
    }
}

data class GenerateResult(
    val overallCount: Int,
    val genreCount: Int,
)
