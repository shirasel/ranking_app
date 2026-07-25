package com.ytranklab.config

import java.nio.file.Path
import kotlin.io.path.inputStream
import org.yaml.snakeyaml.Yaml

class YamlConfigReader(private val yaml: Yaml = Yaml()) {
    fun readMap(path: Path): ConfigMap =
        ConfigMap(path.inputStream().use { yaml.load<Any?>(it).asMap() })
}

class ConfigMap(private val values: Map<String, Any?>) {
    fun map(key: String): ConfigMap = ConfigMap(values[key].asMap())

    fun list(key: String): List<Any?> = values[key] as? List<Any?> ?: emptyList()

    fun string(key: String): String = values[key]?.toString().orEmpty()

    fun optionalString(key: String): String? = values[key]?.toString()?.takeIf { it.isNotBlank() }

    fun int(key: String, default: Int): Int = (values[key] as? Number)?.toInt() ?: default

    fun long(key: String, default: Long): Long = (values[key] as? Number)?.toLong() ?: default

    fun double(key: String, default: Double): Double = (values[key] as? Number)?.toDouble() ?: default

    fun boolean(key: String, default: Boolean): Boolean = values[key] as? Boolean ?: default

    fun stringDoubleMap(): Map<String, Double> =
        values.entries.associate { it.key to ((it.value as? Number)?.toDouble() ?: 0.0) }
}

fun Any?.asConfigMap(): ConfigMap = ConfigMap(asMap())

private fun Any?.asMap(): Map<String, Any?> {
    if (this !is Map<*, *>) return emptyMap()
    return entries
        .filter { it.key != null }
        .associate { it.key.toString() to it.value }
}
