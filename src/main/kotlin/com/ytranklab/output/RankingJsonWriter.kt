package com.ytranklab.output

import com.ytranklab.collection.CollectionReport
import com.ytranklab.config.GenreRule
import com.ytranklab.domain.GenreRankingDocument
import com.ytranklab.domain.RankingDocument
import com.ytranklab.domain.RankingEntry
import java.nio.file.Path

class RankingJsonWriter(
    dataDirectory: Path,
    private val previousRankingReader: PreviousRankingReader = PreviousRankingReader(dataDirectory.resolve("latest")),
    private val latestRankingWriter: LatestRankingWriter = LatestRankingWriter(
        latestDirectory = dataDirectory.resolve("latest"),
        genreDirectory = dataDirectory.resolve("latest").resolve("genres"),
        fileWriter = JsonFileWriter(),
    ),
    private val periodRankingWriter: PeriodRankingWriter = PeriodRankingWriter(
        latestDirectory = dataDirectory.resolve("latest"),
        statisticsReader = VideoStatisticsHistoryReader(dataDirectory.resolve("statistics").resolve("videos")),
        deltaFactory = StatisticDeltaRankingEntryFactory(),
        fileWriter = JsonFileWriter(),
    ),
    private val historyRankingWriter: HistoryRankingWriter = HistoryRankingWriter(
        historyDirectory = dataDirectory.resolve("history"),
        latestDirectory = dataDirectory.resolve("latest"),
        fileWriter = JsonFileWriter(),
    ),
    private val videoDetailWriter: VideoDetailWriter = VideoDetailWriter(
        videoDirectory = dataDirectory.resolve("videos"),
        videoRankingHistoryDirectory = dataDirectory.resolve("rankings").resolve("videos"),
        fileWriter = JsonFileWriter(),
    ),
    private val generationSummaryWriter: GenerationSummaryWriter = GenerationSummaryWriter(
        latestDirectory = dataDirectory.resolve("latest"),
        fileWriter = JsonFileWriter(),
    ),
    private val genreCatalogWriter: GenreCatalogWriter = GenreCatalogWriter(
        latestDirectory = dataDirectory.resolve("latest"),
        fileWriter = JsonFileWriter(),
    ),
) {
    fun loadPreviousOverallRanks(): Map<String, Int> =
        previousRankingReader.loadPreviousOverallRanks()

    fun writeAll(
        overall: RankingDocument,
        genres: Map<String, GenreRankingDocument>,
        trending: RankingDocument,
        discovery: RankingDocument,
    ) {
        latestRankingWriter.write(overall, genres, trending, discovery)
        periodRankingWriter.writeToday(overall)
        periodRankingWriter.writeSevenDays(overall)
        historyRankingWriter.writeHistory(overall)
        historyRankingWriter.writeHistoryIndex()
    }

    fun writeVideoDetails(entries: List<RankingEntry>, generatedAt: String) {
        videoDetailWriter.write(entries, generatedAt)
    }

    fun writeGenreCatalog(genreRules: List<GenreRule>) {
        genreCatalogWriter.write(genreRules)
    }

    fun writeGenerationSummary(
        generatedAt: String,
        inputVideos: Int,
        rankingVideos: Int,
        genreRankings: Int,
        collectionReport: CollectionReport,
        historyDeleted: Int,
        videoDetailsDeleted: Int,
    ) {
        generationSummaryWriter.write(
            generatedAt = generatedAt,
            inputVideos = inputVideos,
            rankingVideos = rankingVideos,
            genreRankings = genreRankings,
            collectionReport = collectionReport,
            historyDeleted = historyDeleted,
            videoDetailsDeleted = videoDetailsDeleted,
        )
    }
}
