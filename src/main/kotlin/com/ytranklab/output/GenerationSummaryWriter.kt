package com.ytranklab.output

import com.ytranklab.collection.CollectionReport
import java.nio.file.Path

class GenerationSummaryWriter(
    private val latestDirectory: Path,
    private val fileWriter: JsonFileWriter,
) {
    fun write(
        generatedAt: String,
        inputVideos: Int,
        rankingVideos: Int,
        genreRankings: Int,
        collectionReport: CollectionReport,
        historyDeleted: Int,
        videoDetailsDeleted: Int,
    ) {
        val document = GenerationSummaryDocument(
            generatedAt = generatedAt,
            inputVideos = inputVideos,
            rankingVideos = rankingVideos,
            genreRankings = genreRankings,
            collection = collectionReport,
            retention = RetentionSummary(
                historyDeleted = historyDeleted,
                videoDetailsDeleted = videoDetailsDeleted,
            ),
        )
        fileWriter.write(
            latestDirectory.resolve("generation-summary.json"),
            fileWriter.encode(GenerationSummaryDocument.serializer(), document),
        )
    }
}
