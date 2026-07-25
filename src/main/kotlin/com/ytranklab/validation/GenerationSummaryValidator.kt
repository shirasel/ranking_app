package com.ytranklab.validation

import com.ytranklab.domain.RankingDocument
import com.ytranklab.output.GenerationSummaryDocument

class GenerationSummaryValidator(private val generatedAtValidator: GeneratedAtValidator) {
    fun validate(summary: GenerationSummaryDocument, overall: RankingDocument, messages: ValidationMessages) {
        generatedAtValidator.validate(summary.generatedAt, "generation-summary", messages)
        if (summary.rankingVideos != overall.ranking.size) {
            messages.errors += "generation-summary のランキング反映数と overall.json の件数が一致しません。"
        }
        if (summary.inputVideos < summary.rankingVideos) {
            messages.errors += "generation-summary の入力動画数がランキング反映数より少ないです。"
        }
        if (summary.collection.publicVideos < summary.rankingVideos) {
            messages.errors += "generation-summary の公開動画数がランキング反映数より少ないです。"
        }
        if (summary.collection.estimatedQuotaUnits >= 8_000) {
            messages.warnings += "推定YouTube API使用量が高めです。"
        }
    }
}
