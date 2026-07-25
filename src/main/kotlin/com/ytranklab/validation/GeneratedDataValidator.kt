package com.ytranklab.validation

import com.ytranklab.domain.RankingDocument
import com.ytranklab.output.GenerationSummaryDocument
import java.nio.file.Path

class GeneratedDataValidator(
    private val dataDirectory: Path,
    private val reader: ValidationJsonReader = ValidationJsonReader(dataDirectory),
    private val generatedAtValidator: GeneratedAtValidator = GeneratedAtValidator(),
    private val rankingDocumentValidator: RankingDocumentValidator = RankingDocumentValidator(generatedAtValidator),
    private val summaryValidator: GenerationSummaryValidator = GenerationSummaryValidator(generatedAtValidator),
    private val videoDetailValidator: VideoDetailValidator = VideoDetailValidator(dataDirectory, reader),
    private val genreFilesValidator: GenreFilesValidator = GenreFilesValidator(
        dataDirectory = dataDirectory,
        latestDirectory = dataDirectory.resolve("latest"),
        reader = reader,
        generatedAtValidator = generatedAtValidator,
    ),
    private val secretScanner: PublicJsonSecretScanner = PublicJsonSecretScanner(dataDirectory),
    private val reportWriter: ValidationReportWriter = ValidationReportWriter(dataDirectory.resolve("latest")),
) {
    fun validate(): ValidationReport {
        val messages = ValidationMessages()

        val overall = reader.read("latest/overall.json", RankingDocument.serializer(), messages)
        val today = reader.read("latest/today.json", RankingDocument.serializer(), messages)
        val sevenDays = reader.read("latest/seven-days.json", RankingDocument.serializer(), messages)
        val trending = reader.read("latest/trending.json", RankingDocument.serializer(), messages)
        val discovery = reader.read("latest/discovery.json", RankingDocument.serializer(), messages)
        val summary = reader.read("latest/generation-summary.json", GenerationSummaryDocument.serializer(), messages)

        listOfNotNull(overall, today, sevenDays, trending, discovery).forEach { document ->
            rankingDocumentValidator.validate(document, messages)
        }

        if (overall != null && summary != null) {
            summaryValidator.validate(summary, overall, messages)
            videoDetailValidator.validate(overall, messages)
        }

        genreFilesValidator.validate(messages)
        secretScanner.scan(messages)

        return ValidationReport(
            dataGeneratedAt = summary?.generatedAt ?: overall?.generatedAt,
            errors = messages.errors,
            warnings = messages.warnings,
        )
    }

    fun writeReport(report: ValidationReport) {
        reportWriter.write(report)
    }
}
