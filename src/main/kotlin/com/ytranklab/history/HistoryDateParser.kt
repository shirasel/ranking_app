package com.ytranklab.history

import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.name

class HistoryDateParser {
    fun parse(file: Path): LocalDate? {
        val day = file.name.removeSuffix(".json")
        val month = file.parent?.name ?: return null
        val year = file.parent?.parent?.name ?: return null
        return runCatching { LocalDate.parse("$year-$month-$day") }.getOrNull()
    }
}
