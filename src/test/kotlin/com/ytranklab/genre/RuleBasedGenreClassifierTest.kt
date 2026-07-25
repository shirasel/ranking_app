package com.ytranklab.genre

import com.ytranklab.config.GenreRule
import com.ytranklab.domain.YouTubeVideo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuleBasedGenreClassifierTest {
    @Test
    fun classifiesMultipleGenres() {
        val rules = listOf(
            GenreRule(
                slug = "gaming",
                name = "ゲーム",
                parent = null,
                titleKeywords = mapOf("ゲーム" to 3.0),
                descriptionKeywords = emptyMap(),
                youtubeCategoryIds = listOf("20"),
            ),
            GenreRule(
                slug = "minecraft",
                name = "Minecraft",
                parent = "gaming",
                titleKeywords = mapOf("マイクラ" to 5.0),
                descriptionKeywords = mapOf("minecraft" to 2.0),
                youtubeCategoryIds = listOf("20"),
            ),
        )
        val video = YouTubeVideo(
            videoId = "v1",
            title = "マイクラ ゲーム実況",
            description = "Minecraft survival",
            channelId = "c1",
            channelName = "Craft",
            youtubeCategoryId = "20",
            publishedAt = "2026-07-25T06:00:00+09:00",
            viewCount = 1,
        )

        val genres = RuleBasedGenreClassifier(rules).classify(video)

        assertEquals("minecraft", genres.first().slug)
        assertTrue(genres.any { it.slug == "gaming" })
    }
}
