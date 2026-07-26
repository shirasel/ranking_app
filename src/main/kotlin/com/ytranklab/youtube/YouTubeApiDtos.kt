package com.ytranklab.youtube

import kotlinx.serialization.Serializable

@Serializable
internal data class SearchListResponse(
    val items: List<SearchItem> = emptyList(),
)

@Serializable
internal data class SearchItem(
    val id: SearchId,
)

@Serializable
internal data class SearchId(
    val videoId: String? = null,
)

@Serializable
internal data class PlaylistItemListResponse(
    val items: List<PlaylistItem> = emptyList(),
)

@Serializable
internal data class PlaylistItem(
    val contentDetails: PlaylistContentDetails,
    val status: PlaylistStatus? = null,
)

@Serializable
internal data class PlaylistContentDetails(
    val videoId: String? = null,
)

@Serializable
internal data class PlaylistStatus(
    val privacyStatus: String? = null,
)

@Serializable
internal data class VideoListResponse(
    val items: List<VideoItem> = emptyList(),
)

@Serializable
internal data class VideoItem(
    val id: String,
    val snippet: VideoSnippet,
    val contentDetails: VideoContentDetails? = null,
    val statistics: VideoStatistics? = null,
    val status: VideoStatus? = null,
)

@Serializable
internal data class VideoSnippet(
    val publishedAt: String,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val thumbnails: Map<String, Thumbnail> = emptyMap(),
    val channelTitle: String,
    val categoryId: String = "",
)

@Serializable
internal data class Thumbnail(
    val url: String,
)

@Serializable
internal data class VideoStatistics(
    val viewCount: String? = null,
    val likeCount: String? = null,
    val commentCount: String? = null,
)

@Serializable
internal data class VideoContentDetails(
    val duration: String? = null,
)

@Serializable
internal data class VideoStatus(
    val privacyStatus: String? = null,
)

@Serializable
internal data class ChannelListResponse(
    val items: List<ChannelItem> = emptyList(),
)

@Serializable
internal data class ChannelItem(
    val id: String,
    val contentDetails: ChannelContentDetails? = null,
    val statistics: ChannelStatistics? = null,
)

@Serializable
internal data class ChannelContentDetails(
    val relatedPlaylists: RelatedPlaylists? = null,
)

@Serializable
internal data class RelatedPlaylists(
    val uploads: String? = null,
)

@Serializable
internal data class ChannelStatistics(
    val subscriberCount: String? = null,
    val hiddenSubscriberCount: Boolean? = null,
)

@Serializable
internal data class YouTubeErrorResponse(
    val error: YouTubeError,
)

@Serializable
internal data class YouTubeError(
    val code: Int? = null,
    val message: String? = null,
    val errors: List<YouTubeErrorDetail> = emptyList(),
    val status: String? = null,
)

@Serializable
internal data class YouTubeErrorDetail(
    val reason: String? = null,
)
