package com.ytranklab.app

import com.ytranklab.config.AppConfigLoader
import com.ytranklab.collection.VideoCollector
import com.ytranklab.genre.RuleBasedGenreClassifier
import com.ytranklab.mock.MockVideoDataSource
import com.ytranklab.output.RankingJsonWriter
import com.ytranklab.ranking.RankingCalculator
import com.ytranklab.ranking.RankingGenerator
import com.ytranklab.ranking.RankingNormalizer
import com.ytranklab.statistics.FileStatisticsRepository
import com.ytranklab.statistics.StatisticsDiffer
import com.ytranklab.youtube.KtorYouTubeApiClient
import java.nio.file.Path
import kotlinx.coroutines.runBlocking

class RankingApplication(private val projectRoot: Path) {
    fun generateMockRankings(): GenerateResult {
        val configLoader = AppConfigLoader(projectRoot.resolve("config"))
        val rankingConfig = configLoader.loadRankingConfig()
        val genreRules = configLoader.loadGenreRules()
        val mockData = MockVideoDataSource(projectRoot.resolve("mock")).load()
        return generateRankings(mockData.capturedAt, mockData.videos, useFallbackStatistics = true)
    }

    fun generateYouTubeRankings(apiKey: String): GenerateResult {
        val configLoader = AppConfigLoader(projectRoot.resolve("config"))
        val sourceConfig = configLoader.loadSourceConfig()
        val client = KtorYouTubeApiClient(apiKey)
        return client.use {
            val collected = runBlocking {
                VideoCollector(sourceConfig, client).collect()
            }
            require(collected.videos.isNotEmpty()) {
                "No public YouTube videos were collected. Existing ranking JSON was not overwritten."
            }
            generateRankings(collected.capturedAt, collected.videos, useFallbackStatistics = false)
        }
    }

    private fun generateRankings(
        capturedAt: String,
        videos: List<com.ytranklab.domain.YouTubeVideo>,
        useFallbackStatistics: Boolean,
    ): GenerateResult {
        val configLoader = AppConfigLoader(projectRoot.resolve("config"))
        val rankingConfig = configLoader.loadRankingConfig()
        val genreRules = configLoader.loadGenreRules()

        val statisticsRepository = FileStatisticsRepository(
            statisticsFile = projectRoot.resolve("docs/data/statistics/latest.json"),
            fallbackFile = if (useFallbackStatistics) projectRoot.resolve("mock/previous-statistics.json") else null,
        )
        val previousStatistics = statisticsRepository.loadLatest()
        val differ = StatisticsDiffer(rankingConfig.periodHours)
        val calculator = RankingCalculator(rankingConfig)
        val classifier = RuleBasedGenreClassifier(genreRules)
        val normalizer = RankingNormalizer()
        val generator = RankingGenerator(rankingConfig, normalizer)
        val writer = RankingJsonWriter(projectRoot.resolve("docs/data"))

        val candidates = videos
            .filter { it.status == "public" }
            .map { video ->
                val delta = differ.calculate(video, previousStatistics[video.videoId], capturedAt)
                val genres = classifier.classify(video)
                val score = calculator.calculate(video, delta, capturedAt)
                RankingCandidate(video, delta, genres, score)
            }

        val previousRanking = writer.loadPreviousOverallRanks()
        val overall = generator.generateOverall(capturedAt, candidates, previousRanking)
        val genres = generator.generateGenres(capturedAt, candidates, previousRanking)
        val discovery = generator.generateDiscovery(capturedAt, candidates, previousRanking)
        val trending = generator.generateTrending(capturedAt, candidates, previousRanking)

        writer.writeAll(overall, genres, trending, discovery)
        writer.writeVideoDetails(overall.ranking, capturedAt)
        statisticsRepository.saveLatest(capturedAt, videos)

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
