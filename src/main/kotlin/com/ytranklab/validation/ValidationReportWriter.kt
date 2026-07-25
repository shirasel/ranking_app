package com.ytranklab.validation

import java.nio.file.Path
import java.time.OffsetDateTime
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ValidationReportWriter(
    private val latestDirectory: Path,
    private val json: Json = Json { prettyPrint = true },
) {
    fun write(report: ValidationReport) {
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
        reportFile.writeText(json.encodeToString(document))
    }
}
