package com.ytranklab.validation

import com.ytranklab.domain.GenreRankingDocument
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class GenreFilesValidator(
    private val dataDirectory: Path,
    private val latestDirectory: Path,
    private val reader: ValidationJsonReader,
    private val generatedAtValidator: GeneratedAtValidator,
) {
    fun validate(messages: ValidationMessages) {
        val genreDirectory = latestDirectory.resolve("genres")
        if (!genreDirectory.exists()) {
            messages.errors += "latest/genres ディレクトリがありません。"
            return
        }

        val genreFiles = genreDirectory.listDirectoryEntries("*.json")
        if (genreFiles.isEmpty()) {
            messages.warnings += "ジャンルJSONがありません。"
            return
        }

        genreFiles.forEach { file ->
            val relativePath = latestDirectory.relativize(file)
            val document = reader.read("latest/$relativePath", GenreRankingDocument.serializer(), messages) ?: return@forEach
            generatedAtValidator.validate(document.generatedAt, "genre:${document.genre.slug}", messages)
            if (document.totalVideos < document.ranking.size) {
                messages.errors += "${file.name} のtotalVideosがランキング件数より少ないです。"
            }
        }
    }
}
