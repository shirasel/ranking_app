package com.ytranklab.app

import com.ytranklab.collection.CollectionReport
import com.ytranklab.collection.VideoCollector
import com.ytranklab.app.reporting.RetentionResultSummary
import com.ytranklab.bootstrap.RankingApplicationDependencies
import com.ytranklab.genre.RuleBasedGenreClassifier
import com.ytranklab.ranking.RankingCalculator
import com.ytranklab.ranking.RankingGenerator
import com.ytranklab.ranking.RankingNormalizer
import com.ytranklab.statistics.StatisticsDiffer
import java.nio.file.Path
import kotlinx.coroutines.runBlocking

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
        val client = dependencies.youtubeApiClientFactory.create(apiKey)
        return client.use {
            val collected = runBlocking {
                VideoCollector(sourceConfig, client, dependencies.collectionReporter).collect()
            }
            require(collected.videos.isNotEmpty()) {
                "No public YouTube videos were collected. Existing ranking JSON was not overwritten."
            }
            generateRankings(
                capturedAt = collected.capturedAt,
                videos = collected.videos,
                useFallbackStatistics = false,
                collectionReport = collected.report,
            )
        }
    }

    private fun generateRankings(
        capturedAt: String,
        videos: List<com.ytranklab.domain.YouTubeVideo>,
        useFallbackStatistics: Boolean,
        collectionReport: CollectionReport,
    ): GenerateResult {
        val rankingConfig = dependencies.configLoader.loadRankingConfig()
        val genreRules = dependencies.configLoader.loadGenreRules()
        val statisticsRepository = dependencies.statisticsRepositoryFactory.create(useFallbackStatistics)
        val previousStatistics = statisticsRepository.loadLatest()
        val differ = StatisticsDiffer(rankingConfig.periodHours)
        val calculator = RankingCalculator(rankingConfig)
        val classifier = RuleBasedGenreClassifier(genreRules)
        val normalizer = RankingNormalizer()
        val generator = RankingGenerator(rankingConfig, normalizer)
        val writer = dependencies.rankingJsonWriter

        val candidates = videos
            .filter { it.status == "public" }
            .map { video ->
                val delta = differ.calculate(video, previousStatistics[video.videoId], capturedAt)
                val genres = classifier.classify(video)
                val score = calculator.calculate(video, delta, capturedAt)
                RankingCandidate(video, delta, genres, score)
            }
            .filter { it.delta.viewIncrease >= rankingConfig.minimumViewIncrease }

        val previousRanking = writer.loadPreviousOverallRanks()
        val overall = generator.generateOverall(capturedAt, candidates, previousRanking)
        val genres = generator.generateGenres(capturedAt, candidates, previousRanking)
        val discovery = generator.generateDiscovery(capturedAt, candidates, previousRanking)
        val trending = generator.generateTrending(capturedAt, candidates, previousRanking)

        statisticsRepository.saveLatest(capturedAt, videos)
        writer.writeAll(overall, genres, trending, discovery)
        writer.writeVideoDetails(overall.ranking, capturedAt)
        val retentionResult = dependencies.historyRetentionServiceFactory
            .create()
            .cleanup(capturedAt, overall.ranking.map { it.videoId }.toSet())
        writer.writeGenerationSummary(
            generatedAt = capturedAt,
            inputVideos = videos.size,
            rankingVideos = overall.ranking.size,
            genreRankings = genres.size,
            collectionReport = collectionReport,
            historyDeleted = retentionResult.historyDeleted,
            videoDetailsDeleted = retentionResult.videoDetailsDeleted,
        )

        dependencies.generationReporter.report(
            inputVideos = videos.size,
            rankingVideos = overall.ranking.size,
            collectionReport = collectionReport,
            retentionResult = RetentionResultSummary(
                historyDeleted = retentionResult.historyDeleted,
                videoDetailsDeleted = retentionResult.videoDetailsDeleted,
            ),
        )

        return GenerateResult(
            overallCount = overall.ranking.size,
            genreCount = genres.size,
        )
    }
}

data class GenerateResult(
    val overallCount: Int,
    val genreCount: Int,
)
