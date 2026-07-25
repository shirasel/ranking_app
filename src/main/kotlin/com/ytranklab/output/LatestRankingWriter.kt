package com.ytranklab.output

import com.ytranklab.domain.GenreRankingDocument
import com.ytranklab.domain.RankingDocument
import java.nio.file.Path

class LatestRankingWriter(
    private val latestDirectory: Path,
    private val genreDirectory: Path,
    private val fileWriter: JsonFileWriter,
) {
    fun write(
        overall: RankingDocument,
        genres: Map<String, GenreRankingDocument>,
        trending: RankingDocument,
        discovery: RankingDocument,
    ) {
        fileWriter.write(
            latestDirectory.resolve("overall.json"),
            fileWriter.encode(RankingDocument.serializer(), overall),
        )
        fileWriter.write(
            latestDirectory.resolve("trending.json"),
            fileWriter.encode(RankingDocument.serializer(), trending),
        )
        fileWriter.write(
            latestDirectory.resolve("discovery.json"),
            fileWriter.encode(RankingDocument.serializer(), discovery),
        )
        genres.forEach { (slug, document) ->
            fileWriter.write(
                genreDirectory.resolve("$slug.json"),
                fileWriter.encode(GenreRankingDocument.serializer(), document),
            )
        }
    }
}
