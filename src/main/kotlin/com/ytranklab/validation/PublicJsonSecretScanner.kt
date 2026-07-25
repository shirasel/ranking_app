package com.ytranklab.validation

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

class PublicJsonSecretScanner(
    private val dataDirectory: Path,
    private val secretPatterns: List<Regex> = listOf(
        Regex("YOUTUBE_API_KEY", RegexOption.IGNORE_CASE),
        Regex("AIza[0-9A-Za-z_-]{20,}"),
    ),
) {
    fun scan(messages: ValidationMessages) {
        if (!dataDirectory.exists()) return
        dataDirectory.walk()
            .filter { it.isRegularFile() && it.name.endsWith(".json") }
            .forEach { file ->
                val content = file.readText()
                if (secretPatterns.any { it.containsMatchIn(content) }) {
                    messages.errors += "${file.relativeTo(dataDirectory)} にSecretらしき文字列があります。"
                }
            }
    }
}
