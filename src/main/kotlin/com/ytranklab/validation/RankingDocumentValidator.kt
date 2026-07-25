package com.ytranklab.validation

import com.ytranklab.domain.RankingDocument

class RankingDocumentValidator(private val generatedAtValidator: GeneratedAtValidator) {
    fun validate(document: RankingDocument, messages: ValidationMessages) {
        generatedAtValidator.validate(document.generatedAt, "ranking:${document.period}", messages)
        val seenIds = mutableSetOf<String>()
        var previousRank = 0
        document.ranking.forEachIndexed { index, entry ->
            val maxExpectedRank = index + 1
            if (entry.rank < previousRank || entry.rank !in 1..maxExpectedRank) {
                messages.errors += "${document.period} の順位が表示順と一致しません。"
                return@forEachIndexed
            }
            previousRank = entry.rank
            if (!seenIds.add(entry.videoId)) {
                messages.errors += "${document.period} に重複した動画IDがあります。"
            }
            if (entry.videoId.isBlank()) messages.errors += "${document.period} に空の動画IDがあります。"
            if (entry.title.isBlank()) messages.errors += "${document.period} に空のタイトルがあります。"
            if (entry.normalizedScore !in 0.0..100.0) {
                messages.errors += "${document.period} に0から100の範囲外スコアがあります。"
            }
        }
    }
}
