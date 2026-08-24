package com.inscopelabs.abx.binbox.domain.model

/**
 * Shell configuration descriptor specifying shell binary, term type, environment, and startup actions.
 */
data class ShellProfile(
    val id: String = "default",
    val name: String = "Default Shell",
    val shellPath: String = "/bin/bash",
    val termType: String = "xterm-256color",
    val envVars: Map<String, String> = emptyMap(),
    val startupCommands: List<String> = emptyList(),
    val initialDirectory: String? = null,
    val encoding: String = "UTF-8"
) {
    companion object {
        val DEFAULT = ShellProfile()

        val BASH = ShellProfile(
            id = "bash",
            name = "GNU Bash",
            shellPath = "/bin/bash",
            termType = "xterm-256color",
            envVars = mapOf("SHELL" to "/bin/bash", "TERM" to "xterm-256color")
        )

        val ZSH = ShellProfile(
            id = "zsh",
            name = "Z Shell (zsh)",
            shellPath = "/bin/zsh",
            termType = "xterm-256color",
            envVars = mapOf("SHELL" to "/bin/zsh", "TERM" to "xterm-256color")
        )

        val FISH = ShellProfile(
            id = "fish",
            name = "Fish Shell",
            shellPath = "/usr/bin/fish",
            termType = "xterm-256color",
            envVars = mapOf("SHELL" to "/usr/bin/fish", "TERM" to "xterm-256color")
        )

        val SH = ShellProfile(
            id = "sh",
            name = "POSIX Shell (sh)",
            shellPath = "/bin/sh",
            termType = "xterm-256color",
            envVars = mapOf("SHELL" to "/bin/sh", "TERM" to "xterm-256color")
        )

        val PYTHON = ShellProfile(
            id = "python",
            name = "Python 3 REPL",
            shellPath = "/usr/bin/python3",
            termType = "xterm-256color",
            envVars = mapOf("PYTHONUNBUFFERED" to "1")
        )

        val ALL_PRESETS = listOf(DEFAULT, BASH, ZSH, FISH, SH, PYTHON)

        fun getProfileById(id: String): ShellProfile {
            return ALL_PRESETS.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}
