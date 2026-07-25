package com.ytranklab.history

import com.ytranklab.config.RetentionConfig
import java.nio.file.Path
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class HistoryRetentionService(
    private val dataDirectory: Path,
    private val retentionConfig: RetentionConfig,
) {
    fun cleanup(generatedAt: String, activeVideoIds: Set<String>): RetentionResult {
        val currentDate = OffsetDateTime.parse(generatedAt).toLocalDate()
        val historyDeleted = cleanupRankingHistory(currentDate)
        val videoDetailsDeleted = cleanupVideoScopedJson(dataDirectory.resolve("videos"), activeVideoIds)
        cleanupVideoScopedJson(dataDirectory.resolve("rankings").resolve("videos"), activeVideoIds)
        return RetentionResult(
            historyDeleted = historyDeleted,
            videoDetailsDeleted = videoDetailsDeleted,
        )
    }

    private fun cleanupRankingHistory(currentDate: LocalDate): Int {
        val historyDirectory = dataDirectory.resolve("history")
        if (!historyDirectory.exists()) return 0

        val oldestAllowed = currentDate.minusDays(retentionConfig.rankingHistoryDays.toLong() - 1)
        var deleted = 0
        historyDirectory.walkFiles().forEach { file ->
            val date = file.toHistoryDate() ?: return@forEach
            if (date.isBefore(oldestAllowed)) {
                file.deleteIfExists()
                deleted += 1
            }
        }
        historyDirectory.pruneEmptyDirectories()
        return deleted
    }

    private fun cleanupVideoScopedJson(videoDirectory: Path, activeVideoIds: Set<String>): Int {
        if (!videoDirectory.exists()) return 0

        var deleted = 0
        videoDirectory.walkFiles().forEach { file ->
            if (file.extension() == "json" && file.name.removeSuffix(".json") !in activeVideoIds) {
                file.deleteIfExists()
                deleted += 1
            }
        }
        return deleted
    }
}

data class RetentionResult(
    val historyDeleted: Int,
    val videoDetailsDeleted: Int,
)

private fun Path.walkFiles(): Sequence<Path> = sequence {
    if (!exists()) return@sequence
    listDirectoryEntries().forEach { child ->
        if (child.isRegularFile()) {
            yield(child)
        } else {
            yieldAll(child.walkFiles())
        }
    }
}

private fun Path.pruneEmptyDirectories() {
    if (!exists() || isRegularFile()) return
    listDirectoryEntries().forEach { child -> child.pruneEmptyDirectories() }
    if (listDirectoryEntries().isEmpty()) {
        deleteIfExists()
    }
}

private fun Path.toHistoryDate(): LocalDate? {
    val day = name.removeSuffix(".json")
    val month = parent?.name ?: return null
    val year = parent?.parent?.name ?: return null
    return runCatching { LocalDate.parse("$year-$month-$day") }.getOrNull()
}

private fun Path.extension(): String = name.substringAfterLast('.', missingDelimiterValue = "")
