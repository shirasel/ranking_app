package com.ytranklab.bootstrap

import com.ytranklab.app.reporting.GenerationReporter
import com.ytranklab.app.reporting.SystemGenerationReporter
import com.ytranklab.config.AppConfigLoader
import com.ytranklab.collection.reporting.CollectionReporter
import com.ytranklab.collection.reporting.SystemCollectionReporter
import com.ytranklab.history.HistoryRetentionService
import com.ytranklab.mock.MockVideoDataSource
import com.ytranklab.output.RankingJsonWriter
import com.ytranklab.statistics.FileStatisticsRepository
import com.ytranklab.statistics.StatisticsRepository
import com.ytranklab.youtube.KtorYouTubeApiClient
import com.ytranklab.youtube.YouTubeApiClient
import java.nio.file.Path

fun interface YouTubeApiClientFactory {
    fun create(apiKey: String): YouTubeApiClient
}

fun interface StatisticsRepositoryFactory {
    fun create(useFallbackStatistics: Boolean): StatisticsRepository
}

fun interface HistoryRetentionServiceFactory {
    fun create(): HistoryRetentionService
}

data class RankingApplicationDependencies(
    val configLoader: AppConfigLoader,
    val mockVideoDataSource: MockVideoDataSource,
    val youtubeApiClientFactory: YouTubeApiClientFactory,
    val statisticsRepositoryFactory: StatisticsRepositoryFactory,
    val rankingJsonWriter: RankingJsonWriter,
    val historyRetentionServiceFactory: HistoryRetentionServiceFactory,
    val generationReporter: GenerationReporter,
    val collectionReporter: CollectionReporter,
) {
    companion object {
        fun fromProjectRoot(projectRoot: Path): RankingApplicationDependencies {
            val paths = AppPaths(projectRoot)
            val configLoader = AppConfigLoader(paths.configDirectory)

            return RankingApplicationDependencies(
                configLoader = configLoader,
                mockVideoDataSource = MockVideoDataSource(paths.mockDirectory),
                youtubeApiClientFactory = YouTubeApiClientFactory { apiKey -> KtorYouTubeApiClient(apiKey) },
                statisticsRepositoryFactory = StatisticsRepositoryFactory { useFallbackStatistics ->
                    FileStatisticsRepository(
                        statisticsFile = paths.statisticsFile,
                        fallbackFile = if (useFallbackStatistics) paths.fallbackStatisticsFile else null,
                        preferFallback = useFallbackStatistics,
                    )
                },
                rankingJsonWriter = RankingJsonWriter(paths.dataDirectory),
                historyRetentionServiceFactory = HistoryRetentionServiceFactory {
                    HistoryRetentionService(paths.dataDirectory, configLoader.loadRankingConfig().retention)
                },
                generationReporter = SystemGenerationReporter(),
                collectionReporter = SystemCollectionReporter(),
            )
        }
    }
}
