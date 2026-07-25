package com.ytranklab.genre

import com.ytranklab.domain.GenreScore

class UncategorizedGenreProvider {
    fun provide(): List<GenreScore> =
        listOf(GenreScore(slug = "uncategorized", name = "未分類", confidence = 0.1))
}
