package com.ytranklab.validation

import kotlinx.serialization.Serializable

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

class ValidationMessages {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()
}
