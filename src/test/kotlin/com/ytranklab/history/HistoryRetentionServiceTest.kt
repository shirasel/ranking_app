package com.ytranklab.history

import com.ytranklab.config.RetentionConfig
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistoryRetentionServiceTest {
    @Test
    fun removesExpiredHistoryAndInactiveVideoDetails() {
        val dataDirectory = Files.createTempDirectory("yt-rank-retention-test")
        val oldHistory = dataDirectory.resolve("history/2025/07/24.json")
        val currentHistory = dataDirectory.resolve("history/2026/07/25.json")
        oldHistory.parent.createDirectories()
        currentHistory.parent.createDirectories()
        oldHistory.writeText("{}")
        currentHistory.writeText("{}")

        val activeVideo = dataDirectory.resolve("videos/activeVideo.json")
        val inactiveVideo = dataDirectory.resolve("videos/inactiveVideo.json")
        activeVideo.parent.createDirectories()
        activeVideo.writeText("{}")
        inactiveVideo.writeText("{}")

        val result = HistoryRetentionService(
            dataDirectory = dataDirectory,
            retentionConfig = RetentionConfig(
                maxTrackedVideos = 500,
                detailedStatisticsDays = 90,
                rankingHistoryDays = 365,
            ),
        ).cleanup("2026-07-25T06:00:00+09:00", setOf("activeVideo"))

        assertEquals(1, result.historyDeleted)
        assertEquals(1, result.videoDetailsDeleted)
        assertFalse(oldHistory.exists())
        assertTrue(currentHistory.exists())
        assertTrue(activeVideo.exists())
        assertFalse(inactiveVideo.exists())
    }
}
