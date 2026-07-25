package com.ytranklab.validation

import com.ytranklab.domain.GenreRankingDocument
import com.ytranklab.domain.RankingDocument
import com.ytranklab.domain.VideoDetailDocument
import com.ytranklab.output.GenerationSummaryDocument
import java.nio.file.Path
import java.time.OffsetDateTime
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.io.path.writeText
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GeneratedDataValidator(private val dataDirectory: Path) {
    private val latestDirectory = dataDirectory.resolve("latest")
    private val videoDirectory = dataDirectory.resolve("videos")
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val reportJson = Json {
        prettyPrint = true
    }

    fun validate(): ValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val overall = readJson("latest/overall.json", RankingDocument.serializer(), errors)
        val today = readJson("latest/today.json", RankingDocument.serializer(), errors)
        val sevenDays = readJson("latest/seven-days.json", RankingDocument.serializer(), errors)
        val trending = readJson("latest/trending.json", RankingDocument.serializer(), errors)
        val discovery = readJson("latest/discovery.json", RankingDocument.serializer(), errors)
        val summary = readJson("latest/generation-summary.json", GenerationSummaryDocument.serializer(), errors)

        listOfNotNull(overall, today, sevenDays, trending, discovery).forEach { document ->
            validateRankingDocument(document, errors)
        }

        if (overall != null && summary != null) {
            validateSummary(summary, overall, errors, warnings)
            validateVideoDetails(overall, errors)
        }

        validateGenreFiles(errors, warnings)
        scanPublicJsonForSecretLikeText(errors)

        return ValidationReport(
            dataGeneratedAt = summary?.generatedAt ?: overall?.generatedAt,
            errors = errors,
            warnings = warnings,
        )
    }

    fun writeReport(report: ValidationReport) {
        val reportFile = latestDirectory.resolve("validation-report.json")
        val document = ValidationReportDocument(
            generatedAt = report.dataGeneratedAt ?: OffsetDateTime.now().toString(),
            status = if (report.isSuccess) "passed" else "failed",
            errorCount = report.errors.size,
            warningCount = report.warnings.size,
            errors = report.errors,
            warnings = report.warnings,
        )
        reportFile.parent.createDirectories()
        reportFile.writeText(reportJson.encodeToString(document))
    }

    private fun <T> readJson(
        relativePath: String,
        deserializer: kotlinx.serialization.DeserializationStrategy<T>,
        errors: MutableList<String>,
    ): T? {
        val file = dataDirectory.resolve(relativePath)
        if (!file.exists()) {
            errors += "$relativePath がありません。"
            return null
        }

        return try {
            json.decodeFromString(deserializer, file.readText())
        } catch (_: SerializationException) {
            errors += "$relativePath のJSON構造が不正です。"
            null
        } catch (_: IllegalArgumentException) {
            errors += "$relativePath を読み込めません。"
            null
        }
    }

    private fun validateRankingDocument(document: RankingDocument, errors: MutableList<String>) {
        validateGeneratedAt(document.generatedAt, "ranking:${document.period}", errors)
        val seenIds = mutableSetOf<String>()
        document.ranking.forEachIndexed { index, entry ->
            val expectedRank = index + 1
            if (entry.rank != expectedRank) {
                errors += "${document.period} の順位が連番ではありません。"
                return@forEachIndexed
            }
            if (!seenIds.add(entry.videoId)) {
                errors += "${document.period} に重複した動画IDがあります。"
            }
            if (entry.videoId.isBlank()) errors += "${document.period} に空の動画IDがあります。"
            if (entry.title.isBlank()) errors += "${document.period} に空のタイトルがあります。"
            if (entry.normalizedScore !in 0.0..100.0) {
                errors += "${document.period} に0から100の範囲外スコアがあります。"
            }
        }
    }

    private fun validateSummary(
        summary: GenerationSummaryDocument,
        overall: RankingDocument,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        validateGeneratedAt(summary.generatedAt, "generation-summary", errors)
        if (summary.rankingVideos != overall.ranking.size) {
            errors += "generation-summary のランキング反映数と overall.json の件数が一致しません。"
        }
        if (summary.inputVideos < summary.rankingVideos) {
            errors += "generation-summary の入力動画数がランキング反映数より少ないです。"
        }
        if (summary.collection.publicVideos < summary.rankingVideos) {
            errors += "generation-summary の公開動画数がランキング反映数より少ないです。"
        }
        if (summary.collection.estimatedQuotaUnits >= 8_000) {
            warnings += "推定YouTube API使用量が高めです。"
        }
    }

    private fun validateVideoDetails(overall: RankingDocument, errors: MutableList<String>) {
        overall.ranking.forEach { entry ->
            val detailPath = videoDirectory.resolve("${entry.videoId}.json")
            val detail = readJson("videos/${entry.videoId}.json", VideoDetailDocument.serializer(), errors)
            if (!detailPath.exists()) return@forEach
            if (detail != null && detail.video.videoId != entry.videoId) {
                errors += "videos/${entry.videoId}.json の動画IDがランキングと一致しません。"
            }
        }
    }

    private fun validateGenreFiles(errors: MutableList<String>, warnings: MutableList<String>) {
        val genreDirectory = latestDirectory.resolve("genres")
        if (!genreDirectory.exists()) {
            errors += "latest/genres ディレクトリがありません。"
            return
        }

        val genreFiles = genreDirectory.listDirectoryEntries("*.json")
        if (genreFiles.isEmpty()) {
            warnings += "ジャンルJSONがありません。"
            return
        }

        genreFiles.forEach { file ->
            val relativePath = dataDirectory.resolve("latest").relativize(file)
            val document = readJson("latest/$relativePath", GenreRankingDocument.serializer(), errors) ?: return@forEach
            validateGeneratedAt(document.generatedAt, "genre:${document.genre.slug}", errors)
            if (document.totalVideos < document.ranking.size) {
                errors += "${file.name} のtotalVideosがランキング件数より少ないです。"
            }
        }
    }

    private fun validateGeneratedAt(value: String, label: String, errors: MutableList<String>) {
        try {
            OffsetDateTime.parse(value)
        } catch (_: Exception) {
            errors += "$label のgeneratedAtが日時として読めません。"
        }
    }

    private fun scanPublicJsonForSecretLikeText(errors: MutableList<String>) {
        if (!dataDirectory.exists()) return
        val secretPatterns = listOf(
            Regex("YOUTUBE_API_KEY", RegexOption.IGNORE_CASE),
            Regex("AIza[0-9A-Za-z_-]{20,}"),
        )

        dataDirectory.walk()
            .filter { it.isRegularFile() && it.name.endsWith(".json") }
            .forEach { file ->
                val content = file.readText()
                if (secretPatterns.any { it.containsMatchIn(content) }) {
                    errors += "${file.relativeTo(dataDirectory)} にSecretらしき文字列があります。"
                }
            }
    }
}

data class ValidationReport(
    val dataGeneratedAt: String? = null,
    val errors: List<String>,
    val warnings: List<String>,
) {
    val isSuccess: Boolean = errors.isEmpty()
}

@Serializable
data class ValidationReportDocument(
    val generatedAt: String,
    val status: String,
    val errorCount: Int,
    val warningCount: Int,
    val errors: List<String>,
    val warnings: List<String>,
)
