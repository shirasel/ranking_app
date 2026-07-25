package com.ytranklab.cli

interface Console {
    fun out(message: String)
    fun err(message: String)
}

class SystemConsole : Console {
    override fun out(message: String) {
        println(message)
    }

    override fun err(message: String) {
        System.err.println(message)
    }
}
