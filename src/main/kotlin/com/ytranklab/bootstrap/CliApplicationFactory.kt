package com.ytranklab.bootstrap

import com.ytranklab.app.RankingApplication
import com.ytranklab.cli.CommandLineRankingApp
import com.ytranklab.cli.SystemConsole
import com.ytranklab.config.AppConfigLoader
import com.ytranklab.history.HistoryRankingRescorer
import com.ytranklab.security.SecretLoader
import com.ytranklab.validation.GeneratedDataValidator
import java.nio.file.Path

class CliApplicationFactory(private val projectRoot: Path) {
    fun create(): CommandLineRankingApp {
        val paths = AppPaths(projectRoot)
        val configLoader = AppConfigLoader(paths.configDirectory)
        return CommandLineRankingApp(
            rankingApplication = RankingApplication(RankingApplicationDependencies.fromProjectRoot(projectRoot)),
            secretLoader = SecretLoader(projectRoot),
            validator = GeneratedDataValidator(paths.dataDirectory),
            historyRescorer = HistoryRankingRescorer(paths.dataDirectory, configLoader.loadRankingConfig()),
            paths = paths,
            console = SystemConsole(),
        )
    }
}
