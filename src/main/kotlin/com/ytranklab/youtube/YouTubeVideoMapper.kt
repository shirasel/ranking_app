package com.ytranklab.youtube

import com.ytranklab.domain.YouTubeVideo

class YouTubeVideoMapper {
    internal fun toDomain(item: VideoItem, subscriberCounts: Map<String, Long?>): YouTubeVideo? {
        val status = item.status?.privacyStatus ?: "public"
        if (status != "public") return null

        return YouTubeVideo(
            videoId = item.id,
            title = item.snippet.title,
            description = item.snippet.description.orEmpty(),
            channelId = item.snippet.channelId,
            channelName = item.snippet.channelTitle,
            youtubeCategoryId = item.snippet.categoryId,
            thumbnailUrl = item.snippet.thumbnails.bestUrl(),
            publishedAt = item.snippet.publishedAt,
            viewCount = item.statistics?.viewCount?.toLongOrNull() ?: 0L,
            likeCount = item.statistics?.likeCount?.toLongOrNull(),
            commentCount = item.statistics?.commentCount?.toLongOrNull(),
            subscriberCount = subscriberCounts[item.snippet.channelId],
            durationSeconds = item.contentDetails?.duration?.toDurationSeconds(),
            status = status,
        )
    }
}

private fun String.toDurationSeconds(): Long? {
    val match = Regex("""PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?""").matchEntire(this) ?: return null
    val hours = match.groupValues[1].toLongOrNull() ?: 0L
    val minutes = match.groupValues[2].toLongOrNull() ?: 0L
    val seconds = match.groupValues[3].toLongOrNull() ?: 0L
    return hours * 3600L + minutes * 60L + seconds
}

private fun Map<String, Thumbnail>.bestUrl(): String =
    listOf("maxres", "standard", "high", "medium", "default")
        .firstNotNullOfOrNull { this[it]?.url }
        .orEmpty()
