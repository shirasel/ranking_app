package com.ytranklab.security

class DotEnvParser {
    fun parse(lines: List<String>): Map<String, String> =
        lines
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

    private fun String.trimMatchingQuotes(): String =
        if (length >= 2 && ((first() == '"' && last() == '"') || (first() == '\'' && last() == '\''))) {
            substring(1, length - 1)
        } else {
            this
        }
}
