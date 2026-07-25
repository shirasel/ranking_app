package com.ytranklab.ranking

import com.ytranklab.app.RankingCandidate
import com.ytranklab.config.RankingConfig
import com.ytranklab.domain.GenreRankingDocument
import com.ytranklab.domain.RankingDocument

class RankingGenerator(
    private val config: RankingConfig,
    normalizer: RankingNormalizer,
    private val entryFactory: RankingEntryFactory = RankingEntryFactory(normalizer),
    private val overallGenerator: OverallRankingGenerator = OverallRankingGenerator(
        periodHours = config.periodHours,
        maxItems = config.maxOverallItems,
        entryFactory = entryFactory,
    ),
    private val genreGenerator: GenreRankingGenerator = GenreRankingGenerator(
        periodHours = config.periodHours,
        maxItems = config.maxGenreItems,
        genreRankingConfig = config.genreRanking,
        entryFactory = entryFactory,
    ),
    private val trendingGenerator: TrendingRankingGenerator = TrendingRankingGenerator(
        periodHours = config.periodHours,
        maxItems = config.maxOverallItems,
        entryFactory = entryFactory,
    ),
    private val discoveryGenerator: DiscoveryRankingGenerator = DiscoveryRankingGenerator(
        periodHours = config.periodHours,
        maxItems = config.maxOverallItems,
        minimumSubscriberCount = config.minimumSubscriberCount,
        unknownSubscriberCount = config.unknownSubscriberCount,
        entryFactory = entryFactory,
    ),
) {
    fun generateOverall(
        generatedAt: String,
        candidates: List<RankingCandidate>,
        previousRanks: Map<String, Int>,
    ): RankingDocument =
        overallGenerator.generate(generatedAt, candidates, previousRanks)

    fun generateGenres(
        generatedAt: String,
        candidates: List<RankingCandidate>,
        previousRanks: Map<String, Int>,
    ): Map<String, GenreRankingDocument> =
        genreGenerator.generate(generatedAt, candidates, previousRanks)

    fun generateTrending(
        generatedAt: String,
        candidates: List<RankingCandidate>,
        previousRanks: Map<String, Int>,
    ): RankingDocument =
        trendingGenerator.generate(generatedAt, candidates, previousRanks)

    fun generateDiscovery(
        generatedAt: String,
        candidates: List<RankingCandidate>,
        previousRanks: Map<String, Int>,
    ): RankingDocument =
        discoveryGenerator.generate(generatedAt, candidates, previousRanks)
}
