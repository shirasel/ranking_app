package com.ytranklab.genre

class GenreTextMatcher {
    fun score(text: String, keywords: Map<String, Double>): Double {
        val normalized = text.lowercase()
        return keywords.entries.sumOf { (keyword, weight) ->
            if (normalized.contains(keyword.lowercase())) weight else 0.0
        }
    }
}
