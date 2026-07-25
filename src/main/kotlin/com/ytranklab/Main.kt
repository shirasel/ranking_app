package com.ytranklab

import com.ytranklab.app.RankingApplication
import com.ytranklab.security.SecretLoader
import kotlin.io.path.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val command = args.firstOrNull()
    val useMock = args.contains("--mock")

    when (command) {
        "generate" -> runGenerate(useMock)
        null, "help", "--help", "-h" -> printUsage()
        else -> {
            System.err.println("Unknown command: $command")
            printUsage()
            exitProcess(2)
        }
    }
}

private fun runGenerate(useMock: Boolean) {
    val secrets = SecretLoader(Path(".")).load()

    if (!useMock && secrets.youtubeApiKey.isNullOrBlank()) {
        System.err.println("YOUTUBE_API_KEY is not set. Use --mock for local mock generation.")
        exitProcess(1)
    }

    if (!useMock) {
        System.err.println("YouTube API generation will be implemented in phase 5. Use --mock for now.")
        exitProcess(1)
    }

    val result = RankingApplication(Path(".")).generateMockRankings()
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
        """.trimIndent(),
    )
}
