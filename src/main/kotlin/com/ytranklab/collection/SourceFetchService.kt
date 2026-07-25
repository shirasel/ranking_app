package com.ytranklab.collection

import com.ytranklab.collection.reporting.CollectionReporter
import com.ytranklab.youtube.YouTubeApiException

class SourceFetchService(private val reporter: CollectionReporter) {
    internal suspend fun collect(task: SourceTask): SourceFetchResult =
        try {
            SourceFetchResult(source = task.sourceName, videoIds = task.fetch(), status = "ok")
        } catch (error: YouTubeApiException) {
            val message = error.safeMessage()
            reporter.skippedSource(task.sourceName, message)
            SourceFetchResult(source = task.sourceName, videoIds = emptyList(), status = "skipped", message = message)
        }
}

private fun YouTubeApiException.safeMessage(): String =
    message
        ?.replace(Regex("key=[^&\\s]+"), "key=***")
        ?.take(180)
        ?: "request failed"
