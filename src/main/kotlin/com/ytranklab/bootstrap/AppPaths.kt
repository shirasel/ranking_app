package com.ytranklab.bootstrap

import java.nio.file.Path

class AppPaths(private val projectRoot: Path) {
    val configDirectory: Path = projectRoot.resolve("config")
    val mockDirectory: Path = projectRoot.resolve("mock")
    val dataDirectory: Path = projectRoot.resolve("docs/data")
    val latestDataDirectory: Path = dataDirectory.resolve("latest")
    val statisticsFile: Path = dataDirectory.resolve("statistics/latest.json")
    val fallbackStatisticsFile: Path = projectRoot.resolve("mock/previous-statistics.json")
}
