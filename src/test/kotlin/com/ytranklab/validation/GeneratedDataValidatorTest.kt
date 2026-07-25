package com.ytranklab.validation

import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeneratedDataValidatorTest {
    @Test
    fun `validates generated public JSON`() {
        val dataDirectory = createFixture()

        val report = GeneratedDataValidator(dataDirectory).validate()

        assertTrue(report.isSuccess)
    }

    @Test
    fun `fails when public JSON contains secret-like text`() {
        val dataDirectory = createFixture()
        dataDirectory.resolve("latest").resolve("leaked.json")
            .writeText("""{"key":"AIza123456789012345678901234567890"}""")

        val report = GeneratedDataValidator(dataDirectory).validate()

        assertFalse(report.isSuccess)
        assertTrue(report.errors.any { it.contains("Secret") })
    }

    private fun createFixture(): java.nio.file.Path {
        val dataDirectory = createTempDirectory("generated-data-validator-test")
        val latestDirectory = dataDirectory.resolve("latest")
        val genreDirectory = latestDirectory.resolve("genres")
        val videoDirectory = dataDirectory.resolve("videos")
        genreDirectory.createDirectories()
        videoDirectory.createDirectories()

        latestDirectory.resolve("overall.json").writeText(rankingDocument())
        latestDirectory.resolve("trending.json").writeText(rankingDocument())
        latestDirectory.resolve("discovery.json").writeText(rankingDocument())
        latestDirectory.resolve("generation-summary.json").writeText(generationSummary())
        genreDirectory.resolve("gaming.json").writeText(genreDocument())
        videoDirectory.resolve("video123").writeText("")
        videoDirectory.resolve("video123.json").writeText(videoDetailDocument())
        return dataDirectory
    }

    private fun rankingDocument(): String = """
        {
          "generatedAt": "2026-07-25T06:00:00+09:00",
          "period": "daily",
          "ranking": [
            {
              "rank": 1,
              "previousRank": null,
              "rankChange": null,
              "videoId": "video123",
              "title": "Sample",
              "channelId": "channel123",
              "channelName": "Channel",
              "thumbnailUrl": "",
              "publishedAt": "2026-07-25T01:00:00+09:00",
              "viewCount": 1000,
              "viewIncrease": 100,
              "likeCount": 100,
              "likeIncrease": 10,
              "commentCount": 5,
              "commentIncrease": 1,
              "subscriberCount": 10000,
              "rawScore": 10.0,
              "normalizedScore": 90.0,
              "genres": [
                {"slug": "gaming", "name": "ゲーム", "confidence": 1.0}
              ],
              "scoreBreakdown": {
                "velocity": 1.0,
                "engagement": 1.0,
                "subscriberRatio": 1.0,
                "freshness": 1.0
              }
            }
          ]
        }
    """.trimIndent()

    private fun genreDocument(): String = """
        {
          "generatedAt": "2026-07-25T06:00:00+09:00",
          "period": "daily",
          "genre": {"slug": "gaming", "name": "ゲーム", "confidence": 1.0},
          "status": "official",
          "totalVideos": 1,
          "totalChannels": 1,
          "ranking": []
        }
    """.trimIndent()

    private fun videoDetailDocument(): String = """
        {
          "generatedAt": "2026-07-25T06:00:00+09:00",
          "video": ${rankingEntry()},
          "scoreBreakdown": {
            "velocity": 1.0,
            "engagement": 1.0,
            "subscriberRatio": 1.0,
            "freshness": 1.0
          },
          "genres": [
            {"slug": "gaming", "name": "ゲーム", "confidence": 1.0}
          ]
        }
    """.trimIndent()

    private fun rankingEntry(): String = """
        {
          "rank": 1,
          "previousRank": null,
          "rankChange": null,
          "videoId": "video123",
          "title": "Sample",
          "channelId": "channel123",
          "channelName": "Channel",
          "thumbnailUrl": "",
          "publishedAt": "2026-07-25T01:00:00+09:00",
          "viewCount": 1000,
          "viewIncrease": 100,
          "likeCount": 100,
          "likeIncrease": 10,
          "commentCount": 5,
          "commentIncrease": 1,
          "subscriberCount": 10000,
          "rawScore": 10.0,
          "normalizedScore": 90.0,
          "genres": [
            {"slug": "gaming", "name": "ゲーム", "confidence": 1.0}
          ],
          "scoreBreakdown": {
            "velocity": 1.0,
            "engagement": 1.0,
            "subscriberRatio": 1.0,
            "freshness": 1.0
          }
        }
    """.trimIndent()

    private fun generationSummary(): String = """
        {
          "generatedAt": "2026-07-25T06:00:00+09:00",
          "inputVideos": 1,
          "rankingVideos": 1,
          "genreRankings": 1,
          "collection": {
            "sourceResults": [],
            "uniqueCandidateIds": 1,
            "fetchedVideoIds": 1,
            "publicVideos": 1,
            "estimatedQuotaUnits": 0
          },
          "retention": {
            "historyDeleted": 0,
            "videoDetailsDeleted": 0
          }
        }
    """.trimIndent()
}
