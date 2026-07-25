package com.ytranklab.app.collection

import com.ytranklab.collection.CollectedVideos
import com.ytranklab.collection.VideoCollector
import com.ytranklab.collection.reporting.CollectionReporter
import com.ytranklab.config.SourceConfig
import com.ytranklab.youtube.YouTubeApiClient
import kotlinx.coroutines.runBlocking

class YouTubeVideoCollectionService(private val reporter: CollectionReporter) {
    fun collect(sourceConfig: SourceConfig, client: YouTubeApiClient): CollectedVideos =
        runBlocking {
            VideoCollector(sourceConfig, client, reporter).collect()
        }
}
