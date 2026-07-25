package com.ytranklab.security

interface SecretSource {
    fun load(): Map<String, String>
}
