package com.ytranklab.app.generation

import com.ytranklab.app.RankingCandidate
import com.ytranklab.config.RankingConfig
import com.ytranklab.domain.YouTubeVideo
import com.ytranklab.genre.GenreClassifier
import com.ytranklab.ranking.RankingCalculator
import com.ytranklab.statistics.StatisticDelta
import com.ytranklab.statistics.StatisticsDiffer
import com.ytranklab.statistics.VideoStatistic

class RankingCandidateFactory(
    private val differ: StatisticsDiffer,
    private val calculator: RankingCalculator,
    private val classifier: GenreClassifier,
    private val rankingConfig: RankingConfig,
) {
    fun create(
        videos: List<YouTubeVideo>,
        previousStatistics: Map<String, VideoStatistic>,
        capturedAt: String,
    ): List<RankingCandidate> =
        videos
            .filter { it.status == "public" }
            .map { video ->
                createCandidate(
                    video = video,
                    delta = differ.calculate(video, previousStatistics[video.videoId], capturedAt),
                    capturedAt = capturedAt,
                )
            }
            .filter { it.delta.viewIncrease >= rankingConfig.minimumViewIncrease }

    private fun createCandidate(video: YouTubeVideo, delta: StatisticDelta, capturedAt: String): RankingCandidate =
        RankingCandidate(
            video = video,
            delta = delta,
            genres = classifier.classify(video),
            score = calculator.calculate(video, delta, capturedAt),
        )
}
