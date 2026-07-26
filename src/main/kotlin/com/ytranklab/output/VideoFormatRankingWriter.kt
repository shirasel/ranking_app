package com.ytranklab.output

import com.ytranklab.domain.RankingDocument
import com.ytranklab.domain.RankingEntry
import java.nio.file.Path

class VideoFormatRankingWriter(
    private val latestDirectory: Path,
    private val fileWriter: JsonFileWriter,
) {
    fun write(overall: RankingDocument) {
        writeFormatRanking("shorts.json", "shorts", overall, overall.ranking.filter { it.isShort })
        writeFormatRanking("long-form.json", "long-form", overall, overall.ranking.filterNot { it.isShort })
    }

    private fun writeFormatRanking(
        fileName: String,
        period: String,
        overall: RankingDocument,
        entries: List<RankingEntry>,
    ) {
        fileWriter.write(
            latestDirectory.resolve(fileName),
            fileWriter.encode(
                RankingDocument.serializer(),
                RankingDocument(
                    generatedAt = overall.generatedAt,
                    period = period,
                    ranking = entries.toFormatEntries(),
                ),
            ),
        )
    }

    private fun List<RankingEntry>.toFormatEntries(): List<RankingEntry> =
        sortedWith(compareByDescending<RankingEntry> { it.rawScore }.thenBy { it.rank })
            .mapIndexed { index, entry ->
                entry.copy(
                    rank = index + 1,
                    previousRank = null,
                    rankChange = null,
                    normalizedScore = normalize(index, size),
                )
            }
            .take(100)

    private fun normalize(index: Int, total: Int): Double {
        if (total <= 1) return 100.0
        return ((total - index).toDouble() / total.toDouble()) * 100.0
    }
}
