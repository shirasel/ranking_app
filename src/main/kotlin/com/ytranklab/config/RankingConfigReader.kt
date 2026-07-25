package com.ytranklab.config

import java.nio.file.Path

class RankingConfigReader(
    private val configDirectory: Path,
    private val yamlReader: YamlConfigReader,
) {
    fun read(): RankingConfig {
        val root = yamlReader.readMap(configDirectory.resolve("ranking.yml"))
        val ranking = root.map("ranking")
        val weights = ranking.map("weights")
        val genreRanking = ranking.map("genreRanking")
        val diversity = ranking.map("diversity")
        val retention = ranking.map("retention")

        return RankingConfig(
            periodHours = ranking.int("periodHours", 24),
            maxOverallItems = ranking.int("maxOverallItems", 100),
            maxGenreItems = ranking.int("maxGenreItems", 50),
            minimumSubscriberCount = ranking.long("minimumSubscriberCount", 1000),
            unknownSubscriberCount = ranking.long("unknownSubscriberCount", 50000),
            minimumViewIncrease = ranking.long("minimumViewIncrease", 1),
            ageDecayExponent = ranking.double("ageDecayExponent", 0.6),
            maxLikeRate = ranking.double("maxLikeRate", 0.12),
            maxCommentRate = ranking.double("maxCommentRate", 0.03),
            weights = RankingWeights(
                velocity = weights.double("velocity", 35.0),
                subscriberRatio = weights.double("subscriberRatio", 25.0),
                likeRate = weights.double("likeRate", 20.0),
                commentRate = weights.double("commentRate", 10.0),
                sevenDayVelocity = weights.double("sevenDayVelocity", 10.0),
            ),
            genreRanking = GenreRankingConfig(
                minimumVideos = genreRanking.int("minimumVideos", 20),
                minimumChannels = genreRanking.int("minimumChannels", 5),
            ),
            diversity = DiversityConfig(
                maxPrimaryGenreShare = diversity.double("maxPrimaryGenreShare", 0.4),
            ),
            retention = RetentionConfig(
                maxTrackedVideos = retention.int("maxTrackedVideos", 500),
                detailedStatisticsDays = retention.int("detailedStatisticsDays", 90),
                rankingHistoryDays = retention.int("rankingHistoryDays", 365),
            ),
        )
    }
}
