package com.ytranklab.statistics

import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class AtomicStatisticsFileWriter {
    fun write(path: Path, content: String) {
        path.parent?.createDirectories()
        val tmp = path.resolveSibling("${path.fileName}.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        java.nio.file.Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING)
    }
}
