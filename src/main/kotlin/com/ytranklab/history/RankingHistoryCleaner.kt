package com.ytranklab.history

import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

class RankingHistoryCleaner(
    private val historyDirectory: Path,
    private val rankingHistoryDays: Int,
    private val fileWalker: RetentionFileWalker = RetentionFileWalker(),
    private val dateParser: HistoryDateParser = HistoryDateParser(),
) {
    fun cleanup(currentDate: LocalDate): Int {
        if (!historyDirectory.exists()) return 0

        val oldestAllowed = currentDate.minusDays(rankingHistoryDays.toLong() - 1)
        var deleted = 0
        fileWalker.walkFiles(historyDirectory).forEach { file ->
            val date = dateParser.parse(file) ?: return@forEach
            if (date.isBefore(oldestAllowed)) {
                file.deleteIfExists()
                deleted += 1
            }
        }
        fileWalker.pruneEmptyDirectories(historyDirectory)
        return deleted
    }
}
