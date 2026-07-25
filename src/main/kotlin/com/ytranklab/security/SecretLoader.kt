package com.ytranklab.security

import java.nio.file.Path

class SecretLoader(
    projectRoot: Path,
    private val resolver: SecretResolver = SecretResolver(
        listOf(
            EnvironmentSecretSource(),
            DotEnvSecretSource(projectRoot.resolve(".env")),
        ),
    ),
) {
    fun load(): Secrets =
        Secrets(youtubeApiKey = resolver.resolve("YOUTUBE_API_KEY"))
}
