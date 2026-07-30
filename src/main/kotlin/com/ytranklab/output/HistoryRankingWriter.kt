package com.ytranklab.output

import com.ytranklab.domain.RankingDocument
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
        val generatedAt = OffsetDateTime.parse(overall.generatedAt).atZoneSameInstant(HISTORY_ZONE)
        val historyFile = historyDirectory
            .resolve(generatedAt.format(DateTimeFormatter.ofPattern("yyyy")))
            .resolve(generatedAt.format(DateTimeFormatter.ofPattern("MM")))
            .resolve("${generatedAt.format(DateTimeFormatter.ofPattern("dd"))}.json")
        fileWriter.write(historyFile, fileWriter.encode(RankingDocument.serializer(), overall))
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
}

private data class HistoryDocumentFile(
    val date: String,
    val path: String,
    val document: RankingDocument,
)

private val HISTORY_ZONE: ZoneId = ZoneId.of("Asia/Tokyo")
