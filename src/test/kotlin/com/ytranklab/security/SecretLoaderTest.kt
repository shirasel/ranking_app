package com.ytranklab.security

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class SecretLoaderTest {
    @Test
    fun loadsYoutubeApiKeyFromDotEnvWithoutLoggingIt() {
        val directory = Files.createTempDirectory("yt-rank-lab-secret-test")
        directory.resolve(".env").writeText(
            """
            # local secret
            YOUTUBE_API_KEY="local-test-key"
            """.trimIndent(),
            Charsets.UTF_8,
        )

        val secrets = SecretLoader(directory).load()

        assertEquals("local-test-key", secrets.youtubeApiKey)
    }
}
