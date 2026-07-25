package com.ytranklab.app

import com.ytranklab.config.AppConfigLoader
import com.ytranklab.genre.RuleBasedGenreClassifier
import com.ytranklab.mock.MockVideoDataSource
import com.ytranklab.output.RankingJsonWriter
import com.ytranklab.ranking.RankingCalculator
import com.ytranklab.ranking.RankingGenerator
import com.ytranklab.ranking.RankingNormalizer
import com.ytranklab.statistics.FileStatisticsRepository
import com.ytranklab.statistics.StatisticsDiffer
import java.nio.file.Path

class RankingApplication(private val projectRoot: Path) {
    fun generateMockRankings(): GenerateResult {
        val configLoader = AppConfigLoader(projectRoot.resolve("config"))
        val rankingConfig = configLoader.loadRankingConfig()
        val genreRules = configLoader.loadGenreRules()
        val mockData = MockVideoDataSource(projectRoot.resolve("mock")).load()

        val statisticsRepository = FileStatisticsRepository(
            statisticsFile = projectRoot.resolve("docs/data/statistics/latest.json"),
            fallbackFile = projectRoot.resolve("mock/previous-statistics.json"),
        )
        val previousStatistics = statisticsRepository.loadLatest()
        val differ = StatisticsDiffer(rankingConfig.periodHours)
        val calculator = RankingCalculator(rankingConfig)
        val classifier = RuleBasedGenreClassifier(genreRules)
        val normalizer = RankingNormalizer()
        val generator = RankingGenerator(rankingConfig, normalizer)
        val writer = RankingJsonWriter(projectRoot.resolve("docs/data"))

        val candidates = mockData.videos
            .filter { it.status == "public" }
            .map { video ->
                val delta = differ.calculate(video, previousStatistics[video.videoId], mockData.capturedAt)
                val genres = classifier.classify(video)
                val score = calculator.calculate(video, delta, mockData.capturedAt)
                RankingCandidate(video, delta, genres, score)
            }

        val previousRanking = writer.loadPreviousOverallRanks()
        val overall = generator.generateOverall(mockData.capturedAt, candidates, previousRanking)
        val genres = generator.generateGenres(mockData.capturedAt, candidates, previousRanking)
        val discovery = generator.generateDiscovery(mockData.capturedAt, candidates, previousRanking)
        val trending = generator.generateTrending(mockData.capturedAt, candidates, previousRanking)

        writer.writeAll(overall, genres, trending, discovery)
        writer.writeVideoDetails(overall.ranking, mockData.capturedAt)
        statisticsRepository.saveLatest(mockData.capturedAt, mockData.videos)

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
