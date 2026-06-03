package com.vextiq.optimizer

/**
 * One place to run an external command and get back its exit code + output.
 *
 * Replaces the near-identical `runCommand` + `CmdResult` blocks that had been
 * copy-pasted into every optimizer. Always closes the stream (`use`) so process
 * handles don't leak, and merges stderr so a failure's message is captured.
 */
object ProcessRunner {

    data class Result(val exitCode: Int, val output: String) {
        val ok: Boolean get() = exitCode == 0
    }

    fun run(cmd: List<String>): Result {
        val process = ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exit = process.waitFor()
        return Result(exit, output)
    }

    fun run(vararg args: String): Result = run(args.toList())

    /** Convenience for `reg` invocations: `reg("add", path, "/v", ...)`. */
    fun reg(vararg args: String): Result = run(listOf("reg") + args.toList())
}
