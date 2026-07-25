package com.ytranklab.validation

import com.ytranklab.domain.RankingDocument
import com.ytranklab.domain.VideoDetailDocument
import java.nio.file.Path
import kotlin.io.path.exists

class VideoDetailValidator(
    private val dataDirectory: Path,
    private val reader: ValidationJsonReader,
) {
    private val videoDirectory = dataDirectory.resolve("videos")

    fun validate(overall: RankingDocument, messages: ValidationMessages) {
        overall.ranking.forEach { entry ->
            val detailPath = videoDirectory.resolve("${entry.videoId}.json")
            val detail = reader.read("videos/${entry.videoId}.json", VideoDetailDocument.serializer(), messages)
            if (!detailPath.exists()) return@forEach
            if (detail != null && detail.video.videoId != entry.videoId) {
                messages.errors += "videos/${entry.videoId}.json の動画IDがランキングと一致しません。"
            }
        }
    }
}
