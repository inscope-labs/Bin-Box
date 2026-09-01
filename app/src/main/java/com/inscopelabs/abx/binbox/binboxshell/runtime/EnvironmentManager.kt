package com.inscopelabs.abx.binbox.binboxshell.runtime

import android.content.Context
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import java.io.File

/**
 * Prepares the POSIX environment variables and initial configuration scripts for shell sessions.
 */
class EnvironmentManager(
    private val context: Context,
    private val runtimePaths: RuntimePaths
) {
    fun setupInitialEnvironment() {
        try {
            val profileFile = File(runtimePaths.homeDir, ".profile")
            if (!profileFile.exists()) {
                context.assets.open("shell/config/profile.sh").use { input ->
                    profileFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                BinBoxLogger.d(TAG, "Copied default profile.sh to ${profileFile.absolutePath}")
            }
        } catch (e: Exception) {
            BinBoxLogger.w(TAG, "Failed to copy default profile.sh: ${e.message}")
        }
    }

    fun buildEnvironment(customEnv: Map<String, String>? = null): Map<String, String> {
        val env = mutableMapOf<String, String>()

        val binPath = runtimePaths.binDir.absolutePath
        val nativeLibPath = runtimePaths.nativeLibDir?.absolutePath

        val pathSegments = mutableListOf(binPath)
        nativeLibPath?.let { pathSegments.add(it) }
        pathSegments.addAll(listOf("/system/bin", "/system/xbin", "/vendor/bin", "/apex/com.android.runtime/bin"))

        env["PATH"] = pathSegments.filter { it.isNotBlank() }.joinToString(":")
        env["HOME"] = runtimePaths.homeDir.absolutePath
        env["TMPDIR"] = runtimePaths.tmpDir.absolutePath
        env["SHELL"] = "/system/bin/sh"
        env["TERM"] = "xterm-256color"
        env["COLORTERM"] = "truecolor"
        env["LANG"] = "en_US.UTF-8"
        env["USER"] = "binbox"
        env["BINBOX_SANDBOX"] = "1"

        nativeLibPath?.let {
            env["LD_LIBRARY_PATH"] = it
        }

        customEnv?.let {
            env.putAll(it)
        }

        BinBoxLogger.d(TAG, "Built local shell environment with ${env.size} variables")
        return env
    }

    fun getMotdBanner(): String {
        return try {
            context.assets.open("shell/config/motd.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "BinBox Local Shell Environment\r\n"
        }
    }

    companion object {
        private const val TAG = "EnvironmentManager"
    }
}
