package com.ytranklab.security

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines

class DotEnvSecretSource(
    private val envFile: Path,
    private val parser: DotEnvParser = DotEnvParser(),
) : SecretSource {
    override fun load(): Map<String, String> {
        if (!envFile.exists()) return emptyMap()
        return parser.parse(envFile.readLines(Charsets.UTF_8))
            .filterValues { it.isNotBlank() }
    }
}
