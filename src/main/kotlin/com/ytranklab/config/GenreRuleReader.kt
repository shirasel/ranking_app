package com.ytranklab.config

import java.nio.file.Path

class GenreRuleReader(
    private val configDirectory: Path,
    private val yamlReader: YamlConfigReader,
) {
    fun read(): List<GenreRule> {
        val root = yamlReader.readMap(configDirectory.resolve("genres.yml"))
        return root.list("genres").map { item ->
            val map = item.asConfigMap()
            val keywords = map.map("keywords")
            GenreRule(
                slug = map.string("slug"),
                name = map.string("name"),
                parent = map.optionalString("parent"),
                visible = map.boolean("visible", true),
                titleKeywords = keywords.map("title").stringDoubleMap(),
                descriptionKeywords = keywords.map("description").stringDoubleMap(),
                youtubeCategoryIds = map.list("youtubeCategoryIds").map { it.toString() },
            )
        }
    }
}
