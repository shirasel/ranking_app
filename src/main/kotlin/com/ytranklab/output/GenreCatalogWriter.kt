package com.ytranklab.output

import com.ytranklab.config.GenreRule
import java.nio.file.Path

class GenreCatalogWriter(
    private val latestDirectory: Path,
    private val fileWriter: JsonFileWriter,
) {
    fun write(genreRules: List<GenreRule>) {
        val document = GenreCatalogDocument(
            genres = genreRules
                .filter { it.visible }
                .map { rule ->
                    GenreCatalogItem(
                        slug = rule.slug,
                        name = rule.name,
                        parent = rule.parent,
                    )
                },
        )
        fileWriter.write(
            latestDirectory.resolve("genre-catalog.json"),
            fileWriter.encode(GenreCatalogDocument.serializer(), document),
        )
    }
}
