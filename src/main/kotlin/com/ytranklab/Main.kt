package com.ytranklab

import com.ytranklab.bootstrap.CliApplicationFactory
import kotlin.io.path.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = CliApplicationFactory(Path(".")).create().execute(args)
    if (exitCode != 0) exitProcess(exitCode)
}
