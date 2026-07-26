package com.ytranklab.output

import com.ytranklab.domain.RankingDocument
import com.ytranklab.domain.RankingEntry
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.ZoneId

class PeriodRankingWriter(
    private val latestDirectory: Path,
    private val statisticsReader: VideoStatisticsHistoryReader,
    private val deltaFactory: StatisticDeltaRankingEntryFactory,
    private val fileWriter: JsonFileWriter,
) {
    fun writeToday(overall: RankingDocument) {
        val generatedAt = OffsetDateTime.parse(overall.generatedAt).atZoneSameInstant(PERIOD_ZONE)
        val targetDate = generatedAt.toLocalDate()
        val entries = overall.ranking
            .map { entry ->
                val statistics = statisticsReader.load(entry.videoId)
                    .filter { OffsetDateTime.parse(it.capturedAt).atZoneSameInstant(PERIOD_ZONE).toLocalDate() == targetDate }
                    .sortedBy { it.capturedAt }
                deltaFactory.withStatisticDelta(entry, statistics)
            }
            .filter { it.viewIncrease > 0 }
            .toPeriodEntries(overall.ranking.size)

        write("today.json", RankingDocument(generatedAt = overall.generatedAt, period = "today", ranking = entries))
    }

    fun writeSevenDays(overall: RankingDocument) {
        val generatedAt = OffsetDateTime.parse(overall.generatedAt)
        val startAt = generatedAt.minusDays(7)
        val entries = overall.ranking
            .map { entry ->
                val statistics = statisticsReader.load(entry.videoId)
                    .filter {
                        val capturedAt = OffsetDateTime.parse(it.capturedAt)
                        !capturedAt.isBefore(startAt) && !capturedAt.isAfter(generatedAt)
                    }
                    .sortedBy { it.capturedAt }
                deltaFactory.withStatisticDelta(entry, statistics)
            }
            .toPeriodEntries(overall.ranking.size)

        write("seven-days.json", RankingDocument(generatedAt = overall.generatedAt, period = "7d", ranking = entries))
    }

    private fun List<RankingEntry>.toPeriodEntries(total: Int): List<RankingEntry> =
        sortedWith(compareByDescending<RankingEntry> { it.viewIncrease }.thenByDescending { it.rawScore })
            .mapIndexed { index, entry ->
                entry.copy(
                    rank = index + 1,
                    previousRank = null,
                    rankChange = null,
                    normalizedScore = normalize(index, total),
                )
            }
            .take(100)

    private fun write(fileName: String, document: RankingDocument) {
        fileWriter.write(
            latestDirectory.resolve(fileName),
            fileWriter.encode(RankingDocument.serializer(), document),
        )
    }

    private fun normalize(index: Int, total: Int): Double {
        if (total <= 1) return 100.0
        return ((total - index).toDouble() / total.toDouble()) * 100.0
    }
}

private val PERIOD_ZONE: ZoneId = ZoneId.of("Asia/Tokyo")
