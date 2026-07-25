package com.ytranklab.mock

import com.ytranklab.domain.YouTubeVideo
import java.nio.file.Path
import kotlin.io.path.readText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MockVideoDataSource(private val mockDirectory: Path) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun load(): MockVideoFile =
        json.decodeFromString(MockVideoFile.serializer(), mockDirectory.resolve("youtube-videos.json").readText())
}

@Serializable
data class MockVideoFile(
    val capturedAt: String,
    val videos: List<YouTubeVideo>,
)
