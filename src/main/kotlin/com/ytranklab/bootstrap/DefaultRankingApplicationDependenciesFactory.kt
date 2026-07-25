package com.ytranklab.bootstrap

import com.ytranklab.app.collection.YouTubeVideoCollectionService
import com.ytranklab.app.generation.DefaultRankingCandidateFactoryFactory
import com.ytranklab.app.generation.DefaultRankingGenerationPersistenceFactory
import com.ytranklab.app.generation.RankingDocumentGenerator
import com.ytranklab.app.reporting.SystemGenerationReporter
import com.ytranklab.collection.reporting.SystemCollectionReporter
import com.ytranklab.config.AppConfigLoader
import com.ytranklab.history.HistoryRetentionService
import com.ytranklab.mock.MockVideoDataSource
import com.ytranklab.output.RankingJsonWriter
import com.ytranklab.statistics.FileStatisticsRepository
import com.ytranklab.youtube.KtorYouTubeApiClient
import java.nio.file.Path

class DefaultRankingApplicationDependenciesFactory(private val projectRoot: Path) {
    fun create(): RankingApplicationDependencies {
        val paths = AppPaths(projectRoot)
        val configLoader = AppConfigLoader(paths.configDirectory)
        val reporters = createReporters()
        val rankingJsonWriter = RankingJsonWriter(paths.dataDirectory)
        val historyRetentionServiceFactory = createHistoryRetentionServiceFactory(paths, configLoader)

        return RankingApplicationDependencies(
            configLoader = configLoader,
            mockVideoDataSource = MockVideoDataSource(paths.mockDirectory),
            youtubeApiClientFactory = YouTubeApiClientFactory { apiKey -> KtorYouTubeApiClient(apiKey) },
            statisticsRepositoryFactory = createStatisticsRepositoryFactory(paths),
            rankingJsonWriter = rankingJsonWriter,
            historyRetentionServiceFactory = historyRetentionServiceFactory,
            generationReporter = reporters.generationReporter,
            collectionReporter = reporters.collectionReporter,
            videoCollectionService = YouTubeVideoCollectionService(reporters.collectionReporter),
            candidateFactoryFactory = DefaultRankingCandidateFactoryFactory(),
            documentGenerator = RankingDocumentGenerator(),
            persistenceFactory = DefaultRankingGenerationPersistenceFactory(
                writer = rankingJsonWriter,
                retentionServiceFactory = { historyRetentionServiceFactory.create() },
                reporter = reporters.generationReporter,
            ),
        )
    }

    private fun createReporters(): ApplicationReporters =
        ApplicationReporters(
            generationReporter = SystemGenerationReporter(),
            collectionReporter = SystemCollectionReporter(),
        )

    private fun createHistoryRetentionServiceFactory(
        paths: AppPaths,
        configLoader: AppConfigLoader,
    ): HistoryRetentionServiceFactory =
        HistoryRetentionServiceFactory {
            HistoryRetentionService(paths.dataDirectory, configLoader.loadRankingConfig().retention)
        }

    private fun createStatisticsRepositoryFactory(paths: AppPaths): StatisticsRepositoryFactory =
        StatisticsRepositoryFactory { useFallbackStatistics ->
            FileStatisticsRepository(
                statisticsFile = paths.statisticsFile,
                fallbackFile = if (useFallbackStatistics) paths.fallbackStatisticsFile else null,
                preferFallback = useFallbackStatistics,
            )
        }
}
