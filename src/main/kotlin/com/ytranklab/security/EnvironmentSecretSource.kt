package com.ytranklab.security

class EnvironmentSecretSource(
    private val environment: Map<String, String> = System.getenv(),
) : SecretSource {
    override fun load(): Map<String, String> =
        environment
            .mapValues { it.value.trim() }
            .filterValues { it.isNotBlank() }
}
