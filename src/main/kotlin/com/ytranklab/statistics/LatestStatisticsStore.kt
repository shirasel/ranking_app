package com.ytranklab.statistics

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlinx.serialization.json.Json

class LatestStatisticsStore(
    private val statisticsFile: Path,
    private val fallbackFile: Path? = null,
    private val preferFallback: Boolean = false,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    },
    private val fileWriter: AtomicStatisticsFileWriter = AtomicStatisticsFileWriter(),
) {
    fun load(): Map<String, VideoStatistic> {
        val source = when {
            preferFallback && fallbackFile?.exists() == true -> fallbackFile
            statisticsFile.exists() -> statisticsFile
            fallbackFile?.exists() == true -> fallbackFile
            else -> return emptyMap()
        }
        val document = json.decodeFromString(StatisticsDocument.serializer(), source.readText())
        return document.statistics.associateBy { it.videoId }
    }

    fun save(capturedAt: String, statistics: List<VideoStatistic>) {
        val document = StatisticsDocument(capturedAt = capturedAt, statistics = statistics)
        fileWriter.write(statisticsFile, json.encodeToString(StatisticsDocument.serializer(), document))
    }
}
