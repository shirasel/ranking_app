package com.ytranklab.genre

import com.ytranklab.config.GenreRule
import com.ytranklab.domain.GenreScore
import com.ytranklab.domain.YouTubeVideo

class RuleBasedGenreClassifier(
    private val rules: List<GenreRule>,
    private val ruleScorer: GenreRuleScorer = GenreRuleScorer(),
    private val confidenceCalculator: GenreConfidenceCalculator = GenreConfidenceCalculator(),
    private val uncategorizedGenreProvider: UncategorizedGenreProvider = UncategorizedGenreProvider(),
) : GenreClassifier {
    override fun classify(video: YouTubeVideo): List<GenreScore> {
        val matches = rules.mapNotNull { rule ->
            val score = ruleScorer.score(video, rule)
            if (score <= 0.0) {
                null
            } else {
                GenreScore(
                    slug = rule.slug,
                    name = rule.name,
                    confidence = confidenceCalculator.calculate(score),
                )
            }
        }.sortedByDescending { it.confidence }

        return matches.ifEmpty { uncategorizedGenreProvider.provide() }
    }
}
