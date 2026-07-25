package com.ytranklab.validation

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ValidationJsonReader(
    private val dataDirectory: Path,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun <T> read(
        relativePath: String,
        deserializer: DeserializationStrategy<T>,
        messages: ValidationMessages,
    ): T? {
        val file = dataDirectory.resolve(relativePath)
        if (!file.exists()) {
            messages.errors += "$relativePath がありません。"
            return null
        }

        return try {
            json.decodeFromString(deserializer, file.readText())
        } catch (_: SerializationException) {
            messages.errors += "$relativePath のJSON構造が不正です。"
            null
        } catch (_: IllegalArgumentException) {
            messages.errors += "$relativePath を読み込めません。"
            null
        }
    }
}
