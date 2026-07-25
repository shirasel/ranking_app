package com.ytranklab.youtube

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YouTubeCurlFallbackClient(
    private val apiKey: String,
    private val errorParser: YouTubeErrorParser,
) {
    suspend fun getJson(path: String, parameters: Map<String, String>): String = withContext(Dispatchers.IO) {
        val url = buildUrl(path, parameters + ("key" to apiKey))
        val curlCommand = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) "curl.exe" else "curl"
        val process = ProcessBuilder(curlCommand, "-sS", "--fail-with-body", url)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw YouTubeApiException("YouTube API request failed: ${errorParser.safeMessage(output)}")
        }
        output
    }

    private fun buildUrl(path: String, parameters: Map<String, String>): String =
        buildString {
            append("https://www.googleapis.com/youtube/v3/")
            append(path)
            append("?")
            append(parameters.entries.joinToString("&") { (key, value) ->
                "${urlEncode(key)}=${urlEncode(value)}"
            })
        }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}
