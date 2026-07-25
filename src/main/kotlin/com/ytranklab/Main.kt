package com.ytranklab

fun main(args: Array<String>) {
    val command = args.firstOrNull()
    val useMock = args.contains("--mock")

    when (command) {
        "generate" -> {
            val mode = if (useMock) "mock" else "youtube-api"
            println("YT Rank Lab generator is ready. mode=$mode")
            println("Ranking generation will be implemented in phase 3.")
        }
        null, "help", "--help", "-h" -> printUsage()
        else -> {
            System.err.println("Unknown command: $command")
            printUsage()
            kotlin.system.exitProcess(2)
        }
    }
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
