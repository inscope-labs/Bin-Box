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
            termType = "xterm-256color"
        )

        val ZSH = ShellProfile(
            id = "zsh",
            name = "Z Shell",
            shellPath = "/bin/zsh",
            termType = "xterm-256color"
        )

        val SH = ShellProfile(
            id = "sh",
            name = "POSIX Shell",
            shellPath = "/bin/sh",
            termType = "xterm-256color"
        )
    }
}
