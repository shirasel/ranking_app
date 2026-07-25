package com.ytranklab.app.generation

import com.ytranklab.config.GenreRule
import com.ytranklab.config.RankingConfig
import com.ytranklab.genre.RuleBasedGenreClassifier
import com.ytranklab.ranking.RankingCalculator
import com.ytranklab.statistics.StatisticsDiffer

fun interface RankingCandidateFactoryFactory {
    fun create(rankingConfig: RankingConfig, genreRules: List<GenreRule>): RankingCandidateFactory
}

class DefaultRankingCandidateFactoryFactory : RankingCandidateFactoryFactory {
    override fun create(rankingConfig: RankingConfig, genreRules: List<GenreRule>): RankingCandidateFactory =
        RankingCandidateFactory(
            differ = StatisticsDiffer(rankingConfig.periodHours),
            calculator = RankingCalculator(rankingConfig),
            classifier = RuleBasedGenreClassifier(genreRules),
            rankingConfig = rankingConfig,
        )
}
