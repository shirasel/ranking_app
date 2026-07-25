package com.ytranklab.app

import com.ytranklab.domain.GenreScore
import com.ytranklab.domain.YouTubeVideo
import com.ytranklab.ranking.ScoreResult
import com.ytranklab.statistics.StatisticDelta

data class RankingCandidate(
    val video: YouTubeVideo,
    val delta: StatisticDelta,
    val genres: List<GenreScore>,
    val score: ScoreResult,
)
