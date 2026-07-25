package com.ytranklab.collection

import com.ytranklab.config.SourceConfig
import com.ytranklab.youtube.YouTubeApiClient

class SourceTaskFactory {
    internal fun create(sourceConfig: SourceConfig, client: YouTubeApiClient): List<SourceTask> {
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

        return tasks.sortedWith(compareBy<SourceTask> { it.priority }.thenBy { it.order })
    }
}

private const val SEARCH_COST = 100
private const val POPULAR_COST = 1
private const val CHANNEL_UPLOAD_COST = 2
