package com.ytranklab.output

import com.ytranklab.domain.RankingDocument
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlinx.serialization.json.Json

class PreviousRankingReader(
    private val latestDirectory: Path,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun loadPreviousOverallRanks(): Map<String, Int> {
        val overallFile = latestDirectory.resolve("overall.json")
        if (!overallFile.exists()) return emptyMap()
        val document = json.decodeFromString(RankingDocument.serializer(), overallFile.readText())
        return document.ranking.associate { it.videoId to it.rank }
    }
}
