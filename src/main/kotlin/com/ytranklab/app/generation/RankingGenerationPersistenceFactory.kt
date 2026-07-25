package com.ytranklab.app.generation

import com.ytranklab.app.reporting.GenerationReporter
import com.ytranklab.history.HistoryRetentionService
import com.ytranklab.output.RankingJsonWriter
import com.ytranklab.statistics.StatisticsRepository

fun interface RankingGenerationPersistenceFactory {
    fun create(statisticsRepository: StatisticsRepository): RankingGenerationPersistence
}

class DefaultRankingGenerationPersistenceFactory(
    private val writer: RankingJsonWriter,
    private val retentionServiceFactory: () -> HistoryRetentionService,
    private val reporter: GenerationReporter,
) : RankingGenerationPersistenceFactory {
    override fun create(statisticsRepository: StatisticsRepository): RankingGenerationPersistence =
        RankingGenerationPersistence(
            statisticsRepository = statisticsRepository,
            writer = writer,
            retentionService = retentionServiceFactory(),
            reporter = reporter,
        )
}
