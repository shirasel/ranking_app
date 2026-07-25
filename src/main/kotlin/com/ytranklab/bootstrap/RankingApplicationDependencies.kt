package com.ytranklab.bootstrap

import com.ytranklab.app.collection.YouTubeVideoCollectionService
import com.ytranklab.app.generation.RankingCandidateFactoryFactory
import com.ytranklab.app.generation.RankingDocumentGenerator
import com.ytranklab.app.generation.RankingGenerationPersistenceFactory
import com.ytranklab.app.reporting.GenerationReporter
import com.ytranklab.config.AppConfigLoader
import com.ytranklab.collection.reporting.CollectionReporter
import com.ytranklab.history.HistoryRetentionService
import com.ytranklab.mock.MockVideoDataSource
import com.ytranklab.output.RankingJsonWriter
import com.ytranklab.statistics.StatisticsRepository
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
    val videoCollectionService: YouTubeVideoCollectionService,
    val candidateFactoryFactory: RankingCandidateFactoryFactory,
    val documentGenerator: RankingDocumentGenerator,
    val persistenceFactory: RankingGenerationPersistenceFactory,
) {
    companion object {
        fun fromProjectRoot(projectRoot: Path): RankingApplicationDependencies =
            DefaultRankingApplicationDependenciesFactory(projectRoot).create()
    }
}
