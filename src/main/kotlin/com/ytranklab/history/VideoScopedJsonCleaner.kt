package com.ytranklab.history

import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.name

class VideoScopedJsonCleaner(
    private val fileWalker: RetentionFileWalker = RetentionFileWalker(),
) {
    fun cleanup(videoDirectory: Path, activeVideoIds: Set<String>): Int {
        if (!videoDirectory.exists()) return 0

        var deleted = 0
        fileWalker.walkFiles(videoDirectory).forEach { file ->
            if (file.extension() == "json" && file.name.removeSuffix(".json") !in activeVideoIds) {
                file.deleteIfExists()
                deleted += 1
            }
        }
        return deleted
    }

    private fun Path.extension(): String = name.substringAfterLast('.', missingDelimiterValue = "")
}
