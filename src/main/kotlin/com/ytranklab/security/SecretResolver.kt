package com.ytranklab.security

class SecretResolver(private val sources: List<SecretSource>) {
    fun resolve(name: String): String? =
        sources
            .asSequence()
            .mapNotNull { source -> source.load()[name]?.takeIf { it.isNotBlank() } }
            .firstOrNull()
}
