package com.ytranklab.config

import java.nio.file.Path
import kotlin.io.path.inputStream
import org.yaml.snakeyaml.Yaml

class AppConfigLoader(private val configDirectory: Path) {
    private val yaml = Yaml()

    fun loadRankingConfig(): RankingConfig {
        val root = readMap(configDirectory.resolve("ranking.yml"))
        val ranking = root.map("ranking")
        val weights = ranking.map("weights")
        val genreRanking = ranking.map("genreRanking")
        val retention = ranking.map("retention")

        return RankingConfig(
            periodHours = ranking.int("periodHours", 24),
            maxOverallItems = ranking.int("maxOverallItems", 100),
            maxGenreItems = ranking.int("maxGenreItems", 50),
            minimumSubscriberCount = ranking.long("minimumSubscriberCount", 1000),
            ageDecayExponent = ranking.double("ageDecayExponent", 0.6),
            weights = RankingWeights(
                velocity = weights.double("velocity", 35.0),
                subscriberRatio = weights.double("subscriberRatio", 25.0),
                likeRate = weights.double("likeRate", 20.0),
                commentRate = weights.double("commentRate", 10.0),
            ),
            genreRanking = GenreRankingConfig(
                minimumVideos = genreRanking.int("minimumVideos", 20),
                minimumChannels = genreRanking.int("minimumChannels", 5),
            ),
            retention = RetentionConfig(
                maxTrackedVideos = retention.int("maxTrackedVideos", 500),
                detailedStatisticsDays = retention.int("detailedStatisticsDays", 90),
                rankingHistoryDays = retention.int("rankingHistoryDays", 365),
            ),
        )
    }

    fun loadGenreRules(): List<GenreRule> {
        val root = readMap(configDirectory.resolve("genres.yml"))
        val genres = root.list("genres")
        return genres.map { item ->
            val map = item.asMap()
            val keywords = map.map("keywords")
            GenreRule(
                slug = map.string("slug"),
                name = map.string("name"),
                parent = map.optionalString("parent"),
                titleKeywords = keywords.map("title").stringDoubleMap(),
                descriptionKeywords = keywords.map("description").stringDoubleMap(),
                youtubeCategoryIds = map.list("youtubeCategoryIds").map { it.toString() },
            )
        }
    }

    fun loadSourceConfig(): SourceConfig {
        val root = readMap(configDirectory.resolve("sources.yml"))
        val channels = root.list("channels").mapNotNull { item ->
            val map = item.asMap()
            val id = map.string("id")
            if (id.isBlank()) {
                null
            } else {
                SourceChannel(id = id, enabled = map.boolean("enabled", true))
            }
        }
        val collection = root.map("collection")
        return SourceConfig(
            channels = channels,
            keywords = root.list("keywords").map { it.toString() }.filter { it.isNotBlank() },
            videos = root.list("videos").map { it.toString() }.filter { it.isNotBlank() },
            collection = CollectionConfig(
                maxVideos = collection.int("maxVideos", 500),
                maxSearchResultsPerKeyword = collection.int("maxSearchResultsPerKeyword", 10),
                includePopularVideos = collection.boolean("includePopularVideos", true),
                regionCode = collection.optionalString("regionCode") ?: "JP",
                maxPopularVideos = collection.int("maxPopularVideos", 25),
                maxChannelVideos = collection.int("maxChannelVideos", 10),
            ),
        )
    }

    private fun readMap(path: Path): Map<String, Any?> =
        path.inputStream().use { yaml.load<Any?>(it).asMap() }
}

data class RankingConfig(
    val periodHours: Int,
    val maxOverallItems: Int,
    val maxGenreItems: Int,
    val minimumSubscriberCount: Long,
    val ageDecayExponent: Double,
    val weights: RankingWeights,
    val genreRanking: GenreRankingConfig,
    val retention: RetentionConfig,
)

data class RankingWeights(
    val velocity: Double,
    val subscriberRatio: Double,
    val likeRate: Double,
    val commentRate: Double,
)

data class GenreRankingConfig(
    val minimumVideos: Int,
    val minimumChannels: Int,
)

data class RetentionConfig(
    val maxTrackedVideos: Int,
    val detailedStatisticsDays: Int,
    val rankingHistoryDays: Int,
)

data class GenreRule(
    val slug: String,
    val name: String,
    val parent: String?,
    val titleKeywords: Map<String, Double>,
    val descriptionKeywords: Map<String, Double>,
    val youtubeCategoryIds: List<String>,
)

data class SourceConfig(
    val channels: List<SourceChannel>,
    val keywords: List<String>,
    val videos: List<String>,
    val collection: CollectionConfig,
)

data class SourceChannel(
    val id: String,
    val enabled: Boolean,
)

data class CollectionConfig(
    val maxVideos: Int,
    val maxSearchResultsPerKeyword: Int,
    val includePopularVideos: Boolean,
    val regionCode: String,
    val maxPopularVideos: Int,
    val maxChannelVideos: Int,
)

private fun Any?.asMap(): Map<String, Any?> {
    if (this !is Map<*, *>) return emptyMap()
    return entries
        .filter { it.key != null }
        .associate { it.key.toString() to it.value }
}

private fun Map<String, Any?>.map(key: String): Map<String, Any?> = this[key].asMap()

private fun Map<String, Any?>.list(key: String): List<Any?> = this[key] as? List<Any?> ?: emptyList()

private fun Map<String, Any?>.string(key: String): String = this[key]?.toString().orEmpty()

private fun Map<String, Any?>.optionalString(key: String): String? = this[key]?.toString()?.takeIf { it.isNotBlank() }

private fun Map<String, Any?>.int(key: String, default: Int): Int = (this[key] as? Number)?.toInt() ?: default

private fun Map<String, Any?>.long(key: String, default: Long): Long = (this[key] as? Number)?.toLong() ?: default

private fun Map<String, Any?>.double(key: String, default: Double): Double = (this[key] as? Number)?.toDouble() ?: default

private fun Map<String, Any?>.boolean(key: String, default: Boolean): Boolean = this[key] as? Boolean ?: default

private fun Map<String, Any?>.stringDoubleMap(): Map<String, Double> =
    entries.associate { it.key to ((it.value as? Number)?.toDouble() ?: 0.0) }
