package com.ytranklab.app

import com.ytranklab.app.generation.GenerateResult
import com.ytranklab.collection.CollectionReport
import com.ytranklab.bootstrap.RankingApplicationDependencies
import java.nio.file.Path

class RankingApplication(private val dependencies: RankingApplicationDependencies) {
    constructor(projectRoot: Path) : this(RankingApplicationDependencies.fromProjectRoot(projectRoot))

    fun generateMockRankings(): GenerateResult {
        val mockData = dependencies.mockVideoDataSource.load()
        return generateRankings(
            capturedAt = mockData.capturedAt,
            videos = mockData.videos,
            useFallbackStatistics = true,
            collectionReport = CollectionReport(
                sourceResults = emptyList(),
                uniqueCandidateIds = mockData.videos.size,
                fetchedVideoIds = mockData.videos.size,
                publicVideos = mockData.videos.count { it.status == "public" },
                estimatedQuotaUnits = 0,
            ),
        )
    }

    fun generateYouTubeRankings(apiKey: String): GenerateResult {
        val sourceConfig = dependencies.configLoader.loadSourceConfig()
        val statisticsRepository = dependencies.statisticsRepositoryFactory.create(useFallbackStatistics = false)
        val trackedVideoIds = statisticsRepository.loadLatest().keys.toList()
        val client = dependencies.youtubeApiClientFactory.create(apiKey)
        return client.use {
            val collected = dependencies.videoCollectionService.collect(sourceConfig, client, trackedVideoIds)
            require(collected.videos.isNotEmpty()) {
                "No public YouTube videos were collected. Existing ranking JSON was not overwritten."
            }
            generateRankings(
                capturedAt = collected.capturedAt,
                videos = collected.videos,
                useFallbackStatistics = false,
                collectionReport = collected.report,
                statisticsRepositoryOverride = statisticsRepository,
            )
        }
    }

    private fun generateRankings(
        capturedAt: String,
        videos: List<com.ytranklab.domain.YouTubeVideo>,
        useFallbackStatistics: Boolean,
        collectionReport: CollectionReport,
        statisticsRepositoryOverride: com.ytranklab.statistics.StatisticsRepository? = null,
    ): GenerateResult {
        val rankingConfig = dependencies.configLoader.loadRankingConfig()
        val genreRules = dependencies.configLoader.loadGenreRules()
        val statisticsRepository = statisticsRepositoryOverride
            ?: dependencies.statisticsRepositoryFactory.create(useFallbackStatistics)
        val previousStatistics = statisticsRepository.loadLatest()
        val sevenDayBaselines = statisticsRepository.loadSevenDayBaselines(capturedAt)
        val writer = dependencies.rankingJsonWriter

        val candidateFactory = dependencies.candidateFactoryFactory.create(rankingConfig, genreRules)
        val candidates = candidateFactory.create(videos, previousStatistics, capturedAt, sevenDayBaselines)
        val previousRanking = writer.loadPreviousOverallRanks()
        val documents = dependencies.documentGenerator.generate(capturedAt, candidates, previousRanking, rankingConfig)

        return dependencies.persistenceFactory
            .create(statisticsRepository)
            .persist(capturedAt, videos, documents, genreRules, collectionReport)
    }
}
