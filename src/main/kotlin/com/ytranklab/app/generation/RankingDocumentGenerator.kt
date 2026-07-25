package com.ytranklab.app.generation

import com.ytranklab.app.RankingCandidate
import com.ytranklab.config.RankingConfig
import com.ytranklab.domain.GenreRankingDocument
import com.ytranklab.domain.RankingDocument
import com.ytranklab.ranking.RankingGenerator
import com.ytranklab.ranking.RankingNormalizer

class RankingDocumentGenerator(private val normalizer: RankingNormalizer = RankingNormalizer()) {
    fun generate(
        capturedAt: String,
        candidates: List<RankingCandidate>,
        previousRanking: Map<String, Int>,
        rankingConfig: RankingConfig,
    ): RankingDocumentSet {
        val generator = RankingGenerator(rankingConfig, normalizer)
        return RankingDocumentSet(
            overall = generator.generateOverall(capturedAt, candidates, previousRanking),
            genres = generator.generateGenres(capturedAt, candidates, previousRanking),
            discovery = generator.generateDiscovery(capturedAt, candidates, previousRanking),
            trending = generator.generateTrending(capturedAt, candidates, previousRanking),
        )
    }
}

data class RankingDocumentSet(
    val overall: RankingDocument,
    val genres: Map<String, GenreRankingDocument>,
    val discovery: RankingDocument,
    val trending: RankingDocument,
)
