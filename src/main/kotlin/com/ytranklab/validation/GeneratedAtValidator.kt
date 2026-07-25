package com.ytranklab.validation

import java.time.OffsetDateTime

class GeneratedAtValidator {
    fun validate(value: String, label: String, messages: ValidationMessages) {
        try {
            OffsetDateTime.parse(value)
        } catch (_: Exception) {
            messages.errors += "$label のgeneratedAtが日時として読めません。"
        }
    }
}
