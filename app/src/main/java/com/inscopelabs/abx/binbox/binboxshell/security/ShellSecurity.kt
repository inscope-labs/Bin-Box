package com.inscopelabs.abx.binbox.binboxshell.security

import com.inscopelabs.abx.binbox.binboxshell.runtime.RuntimePaths
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import java.io.File

/**
 * Validates command inputs, environment variables, and execution safety for the local shell sandbox.
 */
class ShellSecurity(private val runtimePaths: RuntimePaths) {

    private val disallowedEnvPrefixes = listOf(
        "LD_AUDIT",
        "DYLD_"
    )

    fun sanitizeEnvironment(env: Map<String, String>): Map<String, String> {
        val sanitized = mutableMapOf<String, String>()
        for ((key, value) in env) {
            if (disallowedEnvPrefixes.any { key.startsWith(it, ignoreCase = true) }) {
                BinBoxLogger.w(TAG, "Stripped restricted environment variable: $key")
                continue
            }
            if (key.equals("LD_PRELOAD", ignoreCase = true)) {
                // Only allow LD_PRELOAD if it resolves within app's nativeLibraryDir or sandbox
                val allowed = runtimePaths.nativeLibDir?.let { value.startsWith(it.absolutePath) } ?: false
                if (!allowed) {
                    BinBoxLogger.w(TAG, "Rejected unsafe LD_PRELOAD path: $value")
                    continue
                }
            }
            sanitized[key] = value
        }
        return sanitized
    }

    fun isPathWithinSandbox(file: File): Boolean {
        val path = file.canonicalPath
        return path.startsWith(runtimePaths.rootDir.canonicalPath)
    }

    fun sanitizeCommand(command: List<String>): List<String> {
        return command.filter { it.isNotBlank() }
    }

    companion object {
        private const val TAG = "ShellSecurity"
    }
}
