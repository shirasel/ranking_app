package com.ytranklab.cli

import com.ytranklab.app.AppPaths
import com.ytranklab.app.RankingApplication
import com.ytranklab.app.RankingApplicationDependencies
import com.ytranklab.security.SecretLoader
import com.ytranklab.validation.GeneratedDataValidator
import java.nio.file.Path

class CliApplicationFactory(private val projectRoot: Path) {
    fun create(): CommandLineRankingApp {
        val paths = AppPaths(projectRoot)
        return CommandLineRankingApp(
            rankingApplication = RankingApplication(RankingApplicationDependencies.fromProjectRoot(projectRoot)),
            secretLoader = SecretLoader(projectRoot),
            validator = GeneratedDataValidator(paths.dataDirectory),
            paths = paths,
            console = SystemConsole(),
        )
    }
}
