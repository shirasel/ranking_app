package com.ytranklab.statistics

import com.ytranklab.domain.YouTubeVideo
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
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
    private val videoStatisticsDirectory = statisticsFile.parent.resolve("videos")
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
        videos.forEach { video -> appendVideoHistory(capturedAt, video) }
    }

    private fun appendVideoHistory(capturedAt: String, video: YouTubeVideo) {
        videoStatisticsDirectory.createDirectories()
        val historyFile = videoStatisticsDirectory.resolve("${video.videoId}.json")
        val existing = if (historyFile.exists()) {
            json.decodeFromString(VideoStatisticsHistoryDocument.serializer(), historyFile.readText())
        } else {
            VideoStatisticsHistoryDocument(videoId = video.videoId, statistics = emptyList())
        }
        val nextStatistic = VideoStatistic(
            videoId = video.videoId,
            capturedAt = capturedAt,
            viewCount = video.viewCount,
            likeCount = video.likeCount,
            commentCount = video.commentCount,
            subscriberCount = video.subscriberCount,
        )
        val nextStatistics = (existing.statistics.filterNot { it.capturedAt == capturedAt } + nextStatistic)
            .sortedBy { it.capturedAt }
            .takeLast(90)
        val nextDocument = VideoStatisticsHistoryDocument(
            videoId = video.videoId,
            statistics = nextStatistics,
        )
        val tmp = historyFile.resolveSibling("${historyFile.fileName}.tmp")
        tmp.writeText(json.encodeToString(VideoStatisticsHistoryDocument.serializer(), nextDocument), Charsets.UTF_8)
        java.nio.file.Files.move(tmp, historyFile, StandardCopyOption.REPLACE_EXISTING)
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

@Serializable
data class VideoStatisticsHistoryDocument(
    val videoId: String,
    val statistics: List<VideoStatistic>,
)
