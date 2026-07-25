package com.ytranklab.output

import com.ytranklab.domain.GenreRankingDocument
import com.ytranklab.domain.RankingDocument
import com.ytranklab.domain.RankingEntry
import com.ytranklab.domain.VideoDetailDocument
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.json.Json

class RankingJsonWriter(private val dataDirectory: Path) {
    private val latestDirectory = dataDirectory.resolve("latest")
    private val genreDirectory = latestDirectory.resolve("genres")
    private val videoDirectory = dataDirectory.resolve("videos")
    private val historyDirectory = dataDirectory.resolve("history")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun loadPreviousOverallRanks(): Map<String, Int> {
        val overallFile = latestDirectory.resolve("overall.json")
        if (!overallFile.exists()) return emptyMap()
        val document = json.decodeFromString(RankingDocument.serializer(), overallFile.readText())
        return document.ranking.associate { it.videoId to it.rank }
    }

    fun writeAll(
        overall: RankingDocument,
        genres: Map<String, GenreRankingDocument>,
        trending: RankingDocument,
        discovery: RankingDocument,
    ) {
        writeJson(latestDirectory.resolve("overall.json"), json.encodeToString(RankingDocument.serializer(), overall))
        writeJson(latestDirectory.resolve("trending.json"), json.encodeToString(RankingDocument.serializer(), trending))
        writeJson(latestDirectory.resolve("discovery.json"), json.encodeToString(RankingDocument.serializer(), discovery))
        writeHistory(overall)
        genres.forEach { (slug, document) ->
            writeJson(genreDirectory.resolve("$slug.json"), json.encodeToString(GenreRankingDocument.serializer(), document))
        }
    }

    fun writeVideoDetails(entries: List<RankingEntry>, generatedAt: String) {
        entries.forEach { entry ->
            val document = VideoDetailDocument(
                generatedAt = generatedAt,
                video = entry,
                scoreBreakdown = entry.scoreBreakdown,
                genres = entry.genres,
            )
            writeJson(videoDirectory.resolve("${entry.videoId}.json"), json.encodeToString(VideoDetailDocument.serializer(), document))
        }
    }

    private fun writeJson(path: Path, content: String) {
        path.parent?.createDirectories()
        val tmp = path.resolveSibling("${path.fileName}.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        java.nio.file.Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun writeHistory(overall: RankingDocument) {
        val generatedAt = OffsetDateTime.parse(overall.generatedAt)
        val historyFile = historyDirectory
            .resolve(generatedAt.format(DateTimeFormatter.ofPattern("yyyy")))
            .resolve(generatedAt.format(DateTimeFormatter.ofPattern("MM")))
            .resolve("${generatedAt.format(DateTimeFormatter.ofPattern("dd"))}.json")
        writeJson(historyFile, json.encodeToString(RankingDocument.serializer(), overall))
    }
}
