package com.ytranklab.security

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines

class SecretLoader(private val projectRoot: Path) {
    fun load(): Secrets {
        val envValue = System.getenv("YOUTUBE_API_KEY")?.takeIf { it.isNotBlank() }
        if (envValue != null) {
            return Secrets(youtubeApiKey = envValue)
        }

        return Secrets(youtubeApiKey = readDotEnv()["YOUTUBE_API_KEY"]?.takeIf { it.isNotBlank() })
    }

    private fun readDotEnv(): Map<String, String> {
        val file = projectRoot.resolve(".env")
        if (!file.exists()) return emptyMap()

        return file.readLines(Charsets.UTF_8)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) {
                    null
                } else {
                    val key = line.substring(0, separator).trim()
                    val value = line.substring(separator + 1).trim().trimMatchingQuotes()
                    key to value
                }
            }
            .toMap()
    }
}

data class Secrets(
    val youtubeApiKey: String?,
)

private fun String.trimMatchingQuotes(): String =
    if (length >= 2 && ((first() == '"' && last() == '"') || (first() == '\'' && last() == '\''))) {
        substring(1, length - 1)
    } else {
        this
    }
