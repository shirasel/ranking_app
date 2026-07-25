package com.ytranklab.genre

import com.ytranklab.config.GenreRule
import com.ytranklab.domain.YouTubeVideo

class GenreRuleScorer(
    private val textMatcher: GenreTextMatcher = GenreTextMatcher(),
) {
    fun score(video: YouTubeVideo, rule: GenreRule): Double {
        val titleScore = textMatcher.score(video.title, rule.titleKeywords)
        val descriptionScore = textMatcher.score(video.description, rule.descriptionKeywords)
        val categoryScore = if (video.youtubeCategoryId in rule.youtubeCategoryIds) CATEGORY_MATCH_SCORE else 0.0
        val channelScore = textMatcher.score(video.channelName, rule.titleKeywords) * CHANNEL_TITLE_KEYWORD_WEIGHT
        return titleScore + descriptionScore + categoryScore + channelScore
    }

    private companion object {
        const val CATEGORY_MATCH_SCORE = 3.0
        const val CHANNEL_TITLE_KEYWORD_WEIGHT = 0.5
    }
}
