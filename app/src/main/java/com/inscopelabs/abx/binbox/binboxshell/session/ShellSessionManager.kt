package com.inscopelabs.abx.binbox.binboxshell.session

import com.inscopelabs.abx.binbox.binboxshell.runtime.EnvironmentManager
import com.inscopelabs.abx.binbox.binboxshell.runtime.RuntimePaths
import com.inscopelabs.abx.binbox.binboxshell.security.ShellSecurity
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import java.io.File

/**
 * Orchestrates subprocess creation and execution environment for local shell sessions.
 */
class ShellSessionManager(
    private val runtimePaths: RuntimePaths,
    private val environmentManager: EnvironmentManager,
    private val shellSecurity: ShellSecurity
) {

    fun spawnProcess(
        command: List<String>? = null,
        workingDir: File? = null,
        environment: Map<String, String>? = null
    ): Process {
        val targetWorkingDir = workingDir?.takeIf { it.exists() && it.isDirectory }
            ?: runtimePaths.homeDir

        val rawCmd = command?.takeIf { it.isNotEmpty() } ?: listOf("/system/bin/sh", "-l")
        val sanitizedCmd = shellSecurity.sanitizeCommand(rawCmd)

        BinBoxLogger.i(TAG, "Spawning local shell process: ${sanitizedCmd.joinToString(" ")} in ${targetWorkingDir.absolutePath}")

        val pb = ProcessBuilder(sanitizedCmd)
        pb.directory(targetWorkingDir)

        val fullEnv = environmentManager.buildEnvironment(environment)
        val sanitizedEnv = shellSecurity.sanitizeEnvironment(fullEnv)

        val pbEnv = pb.environment()
        pbEnv.clear()
        pbEnv.putAll(sanitizedEnv)

        pb.redirectErrorStream(true)

        val process = pb.start()
        BinBoxLogger.d(TAG, "Process started successfully")
        return process
    }

    companion object {
        private const val TAG = "ShellSessionManager"
    }
}
