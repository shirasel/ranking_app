package com.ytranklab

import com.ytranklab.app.RankingApplication
import com.ytranklab.security.SecretLoader
import com.ytranklab.validation.GeneratedDataValidator
import kotlin.io.path.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val command = args.firstOrNull()
    val useMock = args.contains("--mock")

    when (command) {
        "generate" -> runGenerate(useMock)
        "validate" -> runValidate()
        null, "help", "--help", "-h" -> printUsage()
        else -> {
            System.err.println("Unknown command: $command")
            printUsage()
            exitProcess(2)
        }
    }
}

private fun runValidate() {
    val validator = GeneratedDataValidator(Path("docs", "data"))
    val report = validator.validate()
    validator.writeReport(report)

    report.warnings.forEach { warning ->
        println("Warning: $warning")
    }

    if (!report.isSuccess) {
        report.errors.forEach { error ->
            System.err.println("Error: $error")
        }
        exitProcess(1)
    }

    println("Generated ranking JSON validation passed.")
    println("Validation report: ${Path("docs", "data", "latest", "validation-report.json").toAbsolutePath().normalize()}")
}

private fun runGenerate(useMock: Boolean) {
    val secrets = SecretLoader(Path(".")).load()
    val youtubeApiKey = secrets.youtubeApiKey

    if (!useMock && youtubeApiKey.isNullOrBlank()) {
        System.err.println("YOUTUBE_API_KEY is not set. Use --mock for local mock generation.")
        exitProcess(1)
    }

    val application = RankingApplication(Path("."))
    val result = if (useMock) {
        application.generateMockRankings()
    } else {
        application.generateYouTubeRankings(requireNotNull(youtubeApiKey))
    }
    println("Generated overall ranking: ${result.overallCount} videos")
    println("Generated genre rankings: ${result.genreCount} genres")
    println("Output directory: ${Path("docs", "data", "latest").toAbsolutePath().normalize()}")
}

private fun printUsage() {
    println(
        """
        Usage:
          gradle run --args="generate --mock"
          YOUTUBE_API_KEY=xxxxx gradle run --args="generate"
          gradle run --args="validate"
        """.trimIndent(),
    )
}
