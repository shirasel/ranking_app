package com.ytranklab.history

import com.ytranklab.config.RankingConfig
import com.ytranklab.domain.RankingDocument
import com.ytranklab.domain.RankingEntry
import com.ytranklab.ranking.RankingNormalizer
import com.ytranklab.ranking.round1
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.walk
import kotlinx.serialization.json.Json

class HistoryRankingRescorer(
    private val dataDirectory: Path,
    rankingConfig: RankingConfig,
    private val writer: HistoryRankingRescoreWriter = HistoryRankingRescoreWriter(),
    private val normalizer: RankingNormalizer = RankingNormalizer(),
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    },
) {
    private val shortScoreMultiplier = rankingConfig.shortScoreMultiplier.coerceIn(0.0, 1.0)

    fun rescore(): HistoryRankingRescoreResult {
        val historyDirectory = dataDirectory.resolve("history")
        if (!historyDirectory.exists()) return HistoryRankingRescoreResult(0, 0)

        val files = historyDirectory.walk()
            .filter { it.isRegularFile() && it.fileName.toString().endsWith(".json") }
            .sorted()
            .toList()

        var updated = 0
        files.forEach { file ->
            val document = json.decodeFromString(RankingDocument.serializer(), file.readText())
            val rescored = document.copy(ranking = rescoreEntries(document.ranking))
            if (rescored != document) {
                writer.write(file, rescored)
                updated += 1
            }
        }

        return HistoryRankingRescoreResult(scannedFiles = files.size, updatedFiles = updated)
    }

    private fun rescoreEntries(entries: List<RankingEntry>): List<RankingEntry> {
        val rescored = entries
            .map { entry ->
                val multiplier = if (entry.isShort) shortScoreMultiplier else 1.0
                val rawScore = round1(baseScore(entry) * multiplier)
                entry.copy(
                    rawScore = rawScore,
                    scoreBreakdown = entry.scoreBreakdown.copy(
                        formatAdjustment = round1(multiplier * 100.0),
                    ),
                )
            }
            .sortedWith(compareByDescending<RankingEntry> { it.rawScore }.thenBy { it.rank })

        var lastScore: Double? = null
        var lastRank = 0
        return rescored.mapIndexed { index, entry ->
            val rank = if (lastScore == entry.rawScore) lastRank else index + 1
            lastScore = entry.rawScore
            lastRank = rank
            entry.copy(
                rank = rank,
                rankChange = entry.previousRank?.minus(rank),
                normalizedScore = normalizer.normalize(rank, rescored.size),
            )
        }
    }

    private fun baseScore(entry: RankingEntry): Double {
        val currentAdjustment = (entry.scoreBreakdown.formatAdjustment / 100.0).takeIf { it > 0.0 } ?: 1.0
        return entry.rawScore / currentAdjustment
    }
}

data class HistoryRankingRescoreResult(
    val scannedFiles: Int,
    val updatedFiles: Int,
)
