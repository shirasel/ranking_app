package com.ytranklab.cli

import com.ytranklab.bootstrap.AppPaths
import com.ytranklab.app.RankingApplication
import com.ytranklab.security.SecretLoader
import com.ytranklab.validation.GeneratedDataValidator

class CommandLineRankingApp(
    private val rankingApplication: RankingApplication,
    private val secretLoader: SecretLoader,
    private val validator: GeneratedDataValidator,
    private val paths: AppPaths,
    private val console: Console,
) {
    fun execute(args: Array<String>): Int {
        val command = args.firstOrNull()
        val useMock = args.contains("--mock")

        return when (command) {
            "generate" -> generate(useMock)
            "validate" -> validate()
            null, "help", "--help", "-h" -> {
                printUsage()
                0
            }
            else -> {
                console.err("Unknown command: $command")
                printUsage()
                2
            }
        }
    }

    private fun validate(): Int {
        val report = validator.validate()
        validator.writeReport(report)

        report.warnings.forEach { warning ->
            console.out("Warning: $warning")
        }

        if (!report.isSuccess) {
            report.errors.forEach { error ->
                console.err("Error: $error")
            }
            return 1
        }

        console.out("Generated ranking JSON validation passed.")
        console.out("Validation report: ${paths.latestDataDirectory.resolve("validation-report.json").toAbsolutePath().normalize()}")
        return 0
    }

    private fun generate(useMock: Boolean): Int {
        val youtubeApiKey = secretLoader.load().youtubeApiKey

        if (!useMock && youtubeApiKey.isNullOrBlank()) {
            console.err("YOUTUBE_API_KEY is not set. Use --mock for local mock generation.")
            return 1
        }

        val result = if (useMock) {
            rankingApplication.generateMockRankings()
        } else {
            rankingApplication.generateYouTubeRankings(requireNotNull(youtubeApiKey))
        }

        console.out("Generated overall ranking: ${result.overallCount} videos")
        console.out("Generated genre rankings: ${result.genreCount} genres")
        console.out("Output directory: ${paths.latestDataDirectory.toAbsolutePath().normalize()}")
        return 0
    }

    private fun printUsage() {
        console.out(
            """
            Usage:
              gradle run --args="generate --mock"
              YOUTUBE_API_KEY=xxxxx gradle run --args="generate"
              gradle run --args="validate"
            """.trimIndent(),
        )
    }
}
