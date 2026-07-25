package com.ytranklab.config

import java.nio.file.Path

class SourceConfigReader(
    private val configDirectory: Path,
    private val yamlReader: YamlConfigReader,
) {
    fun read(): SourceConfig {
        val root = yamlReader.readMap(configDirectory.resolve("sources.yml"))
        val collection = root.map("collection")
        return SourceConfig(
            channels = readChannels(root),
            keywords = readKeywords(root),
            categoryPopular = readCategoryPopular(root),
            recentViewCountSearches = readRecentViewCountSearches(root),
            videos = root.list("videos").map { it.toString() }.filter { it.isNotBlank() },
            collection = CollectionConfig(
                maxVideos = collection.int("maxVideos", 500),
                maxSearchResultsPerKeyword = collection.int("maxSearchResultsPerKeyword", 10),
                includePopularVideos = collection.boolean("includePopularVideos", true),
                regionCode = collection.optionalString("regionCode") ?: "JP",
                maxPopularVideos = collection.int("maxPopularVideos", 25),
                maxChannelVideos = collection.int("maxChannelVideos", 10),
                maxEstimatedQuotaUnits = collection.int("maxEstimatedQuotaUnits", 9000),
                reservedDetailQuotaUnits = collection.int("reservedDetailQuotaUnits", 20),
                popularPriority = collection.int("popularPriority", 300),
                maxTrackedPreviousVideos = collection.int("maxTrackedPreviousVideos", 75),
            ),
        )
    }

    private fun readChannels(root: ConfigMap): List<SourceChannel> =
        root.list("channels").mapNotNull { item ->
            val map = item.asConfigMap()
            val id = map.string("id")
            if (id.isBlank()) {
                null
            } else {
                SourceChannel(id = id, enabled = map.boolean("enabled", true), priority = map.int("priority", 100))
            }
        }

    private fun readKeywords(root: ConfigMap): List<SourceKeyword> =
        root.list("keywords").mapNotNull { item ->
            val map = item.asConfigMap()
            val term = if (item is Map<*, *>) {
                map.optionalString("term")
                    ?: map.optionalString("keyword")
                    ?: map.optionalString("query")
                    ?: ""
            } else {
                item.toString()
            }
            if (term.isBlank()) null else SourceKeyword(term = term, priority = map.int("priority", 200))
        }

    private fun readCategoryPopular(root: ConfigMap): List<SourceCategoryPopular> =
        root.list("categoryPopular").mapNotNull { item ->
            val map = item.asConfigMap()
            val categoryId = map.string("youtubeCategoryId")
            if (categoryId.isBlank()) {
                null
            } else {
                SourceCategoryPopular(
                    name = map.optionalString("name") ?: categoryId,
                    youtubeCategoryId = categoryId,
                    maxResults = map.int("maxResults", 20),
                    priority = map.int("priority", 100),
                )
            }
        }

    private fun readRecentViewCountSearches(root: ConfigMap): List<SourceRecentSearch> =
        root.list("recentViewCountSearches").mapNotNull { item ->
            val map = item.asConfigMap()
            val term = map.optionalString("term")
                ?: map.optionalString("keyword")
                ?: map.optionalString("query")
                ?: ""
            if (term.isBlank()) {
                null
            } else {
                SourceRecentSearch(
                    term = term,
                    publishedAfterDays = map.int("publishedAfterDays", 7),
                    maxResults = map.int("maxResults", 8),
                    priority = map.int("priority", 300),
                )
            }
        }
}
