package com.ytranklab.history

import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

class RetentionFileWalker {
    fun walkFiles(directory: Path): Sequence<Path> = sequence {
        if (!directory.exists()) return@sequence
        directory.listDirectoryEntries().forEach { child ->
            if (child.isRegularFile()) {
                yield(child)
            } else {
                yieldAll(walkFiles(child))
            }
        }
    }

    fun pruneEmptyDirectories(directory: Path) {
        if (!directory.exists() || directory.isRegularFile()) return
        directory.listDirectoryEntries().forEach { child -> pruneEmptyDirectories(child) }
        if (directory.listDirectoryEntries().isEmpty()) {
            directory.deleteIfExists()
        }
    }
}
