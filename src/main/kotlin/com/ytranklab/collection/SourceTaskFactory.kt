package com.ytranklab.collection

import com.ytranklab.config.SourceConfig
import com.ytranklab.youtube.YouTubeApiClient
import java.time.OffsetDateTime
import java.time.ZoneOffset

class SourceTaskFactory {
    internal fun create(
        sourceConfig: SourceConfig,
        client: YouTubeApiClient,
        trackedVideoIds: List<String> = emptyList(),
    ): List<SourceTask> {
        val tasks = mutableListOf<SourceTask>()
        var order = 0

        sourceConfig.channels
            .filter { it.enabled }
            .forEach { channel ->
                tasks += SourceTask(
                    sourceName = "channel:${channel.id}",
                    requested = sourceConfig.collection.maxChannelVideos,
                    cost = CHANNEL_UPLOAD_COST,
                    priority = channel.priority,
                    order = order++,
                    fetch = { client.fetchLatestVideoIdsForChannel(channel.id, sourceConfig.collection.maxChannelVideos) },
                )
            }

        sourceConfig.categoryPopular.forEach { category ->
            tasks += SourceTask(
                sourceName = "category-popular:${category.youtubeCategoryId}:${category.name}",
                requested = category.maxResults,
                cost = POPULAR_COST,
                priority = category.priority,
                order = order++,
                fetch = {
                    client.fetchPopularVideoIds(
                        regionCode = sourceConfig.collection.regionCode,
                        maxResults = category.maxResults,
                        videoCategoryId = category.youtubeCategoryId,
                    )
                },
            )
        }

        sourceConfig.keywords.forEach { keyword ->
            tasks += SourceTask(
                sourceName = "keyword:${keyword.term}",
                requested = sourceConfig.collection.maxSearchResultsPerKeyword,
                cost = SEARCH_COST,
                priority = keyword.priority,
                order = order++,
                fetch = { client.searchVideoIds(keyword.term, sourceConfig.collection.maxSearchResultsPerKeyword) },
            )
        }

        sourceConfig.recentViewCountSearches.forEach { search ->
            tasks += SourceTask(
                sourceName = "recent-view-count:${search.term}",
                requested = search.maxResults,
                cost = SEARCH_COST,
                priority = search.priority,
                order = order++,
                fetch = {
                    client.searchRecentPopularVideoIds(
                        keyword = search.term,
                        maxResults = search.maxResults,
                        publishedAfter = OffsetDateTime.now(ZoneOffset.UTC)
                            .minusDays(search.publishedAfterDays.toLong())
                            .toString(),
                    )
                },
            )
        }

        if (sourceConfig.collection.includePopularVideos) {
            tasks += SourceTask(
                sourceName = "popular:${sourceConfig.collection.regionCode}",
                requested = sourceConfig.collection.maxPopularVideos,
                cost = POPULAR_COST,
                priority = sourceConfig.collection.popularPriority,
                order = order++,
                fetch = {
                    client.fetchPopularVideoIds(
                        regionCode = sourceConfig.collection.regionCode,
                        maxResults = sourceConfig.collection.maxPopularVideos,
                    )
                },
            )
        }

        if (trackedVideoIds.isNotEmpty()) {
            tasks += SourceTask(
                sourceName = "tracked-previous",
                requested = sourceConfig.collection.maxTrackedPreviousVideos,
                cost = 0,
                priority = TRACKED_PREVIOUS_PRIORITY,
                order = order++,
                fetch = { trackedVideoIds.take(sourceConfig.collection.maxTrackedPreviousVideos) },
            )
        }

        return tasks.sortedWith(compareBy<SourceTask> { it.priority }.thenBy { it.order })
    }
}

private const val SEARCH_COST = 100
private const val POPULAR_COST = 1
private const val CHANNEL_UPLOAD_COST = 2
private const val TRACKED_PREVIOUS_PRIORITY = 250
