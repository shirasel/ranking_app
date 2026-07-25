package com.ytranklab.config

import java.nio.file.Path

class AppConfigLoader(
    configDirectory: Path,
    yamlReader: YamlConfigReader = YamlConfigReader(),
) {
    private val rankingConfigReader = RankingConfigReader(configDirectory, yamlReader)
    private val genreRuleReader = GenreRuleReader(configDirectory, yamlReader)
    private val sourceConfigReader = SourceConfigReader(configDirectory, yamlReader)

    fun loadRankingConfig(): RankingConfig = rankingConfigReader.read()

    fun loadGenreRules(): List<GenreRule> = genreRuleReader.read()

    fun loadSourceConfig(): SourceConfig = sourceConfigReader.read()
}
