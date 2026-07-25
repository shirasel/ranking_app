package com.ytranklab.genre

import com.ytranklab.config.GenreRule
import com.ytranklab.domain.GenreScore
import com.ytranklab.domain.YouTubeVideo
import kotlin.math.min

interface GenreClassifier {
    fun classify(video: YouTubeVideo): List<GenreScore>
}

class RuleBasedGenreClassifier(private val rules: List<GenreRule>) : GenreClassifier {
    override fun classify(video: YouTubeVideo): List<GenreScore> {
        val matches = rules.mapNotNull { rule ->
            val titleScore = scoreText(video.title, rule.titleKeywords)
            val descriptionScore = scoreText(video.description, rule.descriptionKeywords)
            val categoryScore = if (video.youtubeCategoryId in rule.youtubeCategoryIds) 3.0 else 0.0
            val channelScore = scoreText(video.channelName, rule.titleKeywords) * 0.5
            val total = titleScore + descriptionScore + categoryScore + channelScore
            if (total <= 0.0) {
                null
            } else {
                GenreScore(
                    slug = rule.slug,
                    name = rule.name,
                    confidence = round1(min(0.99, total / 10.0)),
                )
            }
        }.sortedByDescending { it.confidence }

        return matches.ifEmpty {
            listOf(GenreScore(slug = "uncategorized", name = "未分類", confidence = 0.1))
        }
    }

    private fun scoreText(text: String, keywords: Map<String, Double>): Double {
        val normalized = text.lowercase()
        return keywords.entries.sumOf { (keyword, weight) ->
            if (normalized.contains(keyword.lowercase())) weight else 0.0
        }
    }
}

private fun round1(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0
