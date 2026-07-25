package com.ytranklab.output

import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

class JsonFileWriter(
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    },
) {
    fun <T> encode(serializer: SerializationStrategy<T>, value: T): String =
        json.encodeToString(serializer, value)

    fun write(path: Path, content: String) {
        path.parent?.createDirectories()
        val tmp = path.resolveSibling("${path.fileName}.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        java.nio.file.Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING)
    }
}
