package com.ytranklab.app.reporting

import com.ytranklab.collection.CollectionReport

interface GenerationReporter {
    fun report(inputVideos: Int, rankingVideos: Int, collectionReport: CollectionReport, retentionResult: RetentionResultSummary)
}

class SystemGenerationReporter : GenerationReporter {
    override fun report(
        inputVideos: Int,
        rankingVideos: Int,
        collectionReport: CollectionReport,
        retentionResult: RetentionResultSummary,
    ) {
        println("Input videos: $inputVideos")
        println("Ranking videos: $rankingVideos")
        println("Estimated YouTube quota units: ${collectionReport.estimatedQuotaUnits}")
        println("Retention cleanup: history=${retentionResult.historyDeleted}, videoDetails=${retentionResult.videoDetailsDeleted}")
    }
}

data class RetentionResultSummary(
    val historyDeleted: Int,
    val videoDetailsDeleted: Int,
)
