package com.ytranklab.statistics

import com.ytranklab.domain.YouTubeVideo

class StatisticsDiffer(private val defaultPeriodHours: Int) {
    fun calculate(
        video: YouTubeVideo,
        previous: VideoStatistic?,
        capturedAt: String,
        sevenDayBaseline: VideoStatistic? = null,
    ): StatisticDelta {
        val elapsedHours = previous?.capturedAt
            ?.let { hoursBetween(it, capturedAt) }
            ?.takeIf { it > 0.0 }
            ?: defaultPeriodHours.toDouble()
        val sevenDayElapsedHours = sevenDayBaseline?.capturedAt
            ?.let { hoursBetween(it, capturedAt) }
            ?.takeIf { it > 0.0 }

        val viewIncrease = nonNegativeDifference(video.viewCount, previous?.viewCount)
        val likeIncrease = nullableNonNegativeDifference(video.likeCount, previous?.likeCount)
        val commentIncrease = nullableNonNegativeDifference(video.commentCount, previous?.commentCount)
        val sevenDayViewIncrease = sevenDayBaseline?.let { nonNegativeDifference(video.viewCount, it.viewCount) }

        return StatisticDelta(
            viewIncrease = viewIncrease,
            likeIncrease = likeIncrease,
            commentIncrease = commentIncrease,
            elapsedHours = elapsedHours,
            viewVelocity = viewIncrease / elapsedHours,
            sevenDayViewIncrease = sevenDayViewIncrease,
            sevenDayViewVelocity = if (sevenDayViewIncrease != null && sevenDayElapsedHours != null) {
                sevenDayViewIncrease / sevenDayElapsedHours
            } else {
                null
            },
        )
    }

    private fun nonNegativeDifference(current: Long, previous: Long?): Long =
        previous?.let { (current - it).coerceAtLeast(0) } ?: current.coerceAtLeast(0)

    private fun nullableNonNegativeDifference(current: Long?, previous: Long?): Long? =
        current?.let { value -> previous?.let { (value - it).coerceAtLeast(0) } ?: value.coerceAtLeast(0) }
}

data class StatisticDelta(
    val viewIncrease: Long,
    val likeIncrease: Long?,
    val commentIncrease: Long?,
    val elapsedHours: Double,
    val viewVelocity: Double,
    val sevenDayViewIncrease: Long? = null,
    val sevenDayViewVelocity: Double? = null,
)

fun hoursBetween(from: String, to: String): Double {
    val start = java.time.OffsetDateTime.parse(from)
    val end = java.time.OffsetDateTime.parse(to)
    return java.time.Duration.between(start, end).toMinutes().toDouble() / 60.0
}
