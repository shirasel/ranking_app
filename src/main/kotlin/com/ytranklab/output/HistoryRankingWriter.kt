package com.ytranklab.output

import com.ytranklab.domain.RankingDocument
import com.ytranklab.domain.RankingEntry
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlinx.serialization.json.Json

class HistoryRankingWriter(
    private val historyDirectory: Path,
    private val latestDirectory: Path,
    private val fileWriter: JsonFileWriter,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun writeHistory(overall: RankingDocument) {
        overall.ranking
            .groupBy { entry -> OffsetDateTime.parse(entry.publishedAt).atZoneSameInstant(HISTORY_ZONE).toLocalDate() }
            .forEach { (publishedDate, entries) ->
                val historyFile = historyDirectory
                    .resolve(publishedDate.format(DateTimeFormatter.ofPattern("yyyy")))
                    .resolve(publishedDate.format(DateTimeFormatter.ofPattern("MM")))
                    .resolve("${publishedDate.format(DateTimeFormatter.ofPattern("dd"))}.json")
                val document = RankingDocument(
                    generatedAt = overall.generatedAt,
                    period = "history",
                    ranking = entries.toDailyEntries(),
                )
                fileWriter.write(historyFile, fileWriter.encode(RankingDocument.serializer(), document))
            }
    }

    fun writeHistoryIndex() {
        val items = loadHistoryDocuments().map { history ->
            HistoryIndexItem(
                date = history.date,
                generatedAt = history.document.generatedAt,
                path = history.path,
                totalVideos = history.document.ranking.size,
            )
        }
        fileWriter.write(
            latestDirectory.resolve("history-index.json"),
            fileWriter.encode(HistoryIndexDocument.serializer(), HistoryIndexDocument(items = items)),
        )
    }

    private fun loadHistoryDocuments(): List<HistoryDocumentFile> {
        if (!historyDirectory.exists()) return emptyList()
        return historyDirectory.walk()
            .filter { it.isRegularFile() && it.toString().endsWith(".json") }
            .sortedBy { it.relativeTo(historyDirectory).toString() }
            .mapNotNull { file ->
                runCatching {
                    val relativePath = file.relativeTo(historyDirectory).toString().replace('\\', '/')
                    val date = relativePath.removeSuffix(".json").replace('/', '-')
                    HistoryDocumentFile(
                        date = date,
                        path = "history/$relativePath",
                        document = json.decodeFromString(RankingDocument.serializer(), file.readText()),
                    )
                }.getOrNull()
            }
            .toList()
    }

    private fun List<RankingEntry>.toDailyEntries(): List<RankingEntry> =
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

private data class HistoryDocumentFile(
    val date: String,
    val path: String,
    val document: RankingDocument,
)

private val HISTORY_ZONE: ZoneId = ZoneId.of("Asia/Tokyo")
