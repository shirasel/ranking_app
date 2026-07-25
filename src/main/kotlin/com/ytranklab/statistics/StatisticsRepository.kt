package com.ytranklab.statistics

import com.ytranklab.domain.YouTubeVideo
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.parent
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface StatisticsRepository {
    fun loadLatest(): Map<String, VideoStatistic>

    fun saveLatest(capturedAt: String, videos: List<YouTubeVideo>)
}

class FileStatisticsRepository(
    private val statisticsFile: Path,
    private val fallbackFile: Path? = null,
) : StatisticsRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override fun loadLatest(): Map<String, VideoStatistic> {
        val source = when {
            statisticsFile.exists() -> statisticsFile
            fallbackFile?.exists() == true -> fallbackFile
            else -> return emptyMap()
        }
        val document = json.decodeFromString(StatisticsDocument.serializer(), source.readText())
        return document.statistics.associateBy { it.videoId }
    }

    override fun saveLatest(capturedAt: String, videos: List<YouTubeVideo>) {
        statisticsFile.parent?.createDirectories()
        val document = StatisticsDocument(
            capturedAt = capturedAt,
            statistics = videos.map {
                VideoStatistic(
                    videoId = it.videoId,
                    capturedAt = capturedAt,
                    viewCount = it.viewCount,
                    likeCount = it.likeCount,
                    commentCount = it.commentCount,
                    subscriberCount = it.subscriberCount,
                )
            },
        )
        val tmp = statisticsFile.resolveSibling("${statisticsFile.fileName}.tmp")
        tmp.writeText(json.encodeToString(StatisticsDocument.serializer(), document), Charsets.UTF_8)
        java.nio.file.Files.move(tmp, statisticsFile, StandardCopyOption.REPLACE_EXISTING)
    }
}

@Serializable
data class StatisticsDocument(
    val capturedAt: String,
    val statistics: List<VideoStatistic>,
)

@Serializable
data class VideoStatistic(
    val videoId: String,
    val capturedAt: String,
    val viewCount: Long,
    val likeCount: Long? = null,
    val commentCount: Long? = null,
    val subscriberCount: Long? = null,
)
