package com.ytranklab.history

import com.ytranklab.domain.RankingDocument
import com.ytranklab.output.JsonFileWriter
import java.nio.file.Path

class HistoryRankingRescoreWriter(
    private val fileWriter: JsonFileWriter = JsonFileWriter(),
) {
    fun write(file: Path, document: RankingDocument) {
        fileWriter.write(file, fileWriter.encode(RankingDocument.serializer(), document))
    }
}
