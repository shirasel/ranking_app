package com.ytranklab.output

import com.ytranklab.statistics.VideoStatistic
import com.ytranklab.statistics.VideoStatisticsHistoryDocument
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlinx.serialization.json.Json

class VideoStatisticsHistoryReader(
    private val videoStatisticsHistoryDirectory: Path,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun load(videoId: String): List<VideoStatistic> {
        val file = videoStatisticsHistoryDirectory.resolve("$videoId.json")
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString(VideoStatisticsHistoryDocument.serializer(), file.readText()).statistics
        }.getOrDefault(emptyList())
    }
}
