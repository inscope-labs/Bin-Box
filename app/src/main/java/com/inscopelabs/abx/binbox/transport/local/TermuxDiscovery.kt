package com.inscopelabs.abx.binbox.transport.local

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import java.io.File

/**
 * Discovery utility for local Android and Termux shells (Phase 4 — Local Android/Termux Shell Provider).
 *
 * Checks for Termux userland environment, installed binaries, system shells,
 * and builds appropriate environment configurations for POSIX execution on Android.
 */
object TermuxDiscovery {

    const val TERMUX_PACKAGE_NAME = "com.termux"
    const val TERMUX_FILES_DIR = "/data/data/com.termux/files"
    const val TERMUX_PREFIX = "$TERMUX_FILES_DIR/usr"
    const val TERMUX_HOME = "$TERMUX_FILES_DIR/home"
    const val TERMUX_BIN = "$TERMUX_PREFIX/bin"
    const val TERMUX_LIB = "$TERMUX_PREFIX/lib"
    const val TERMUX_TMP = "$TERMUX_PREFIX/tmp"

    private val CANDIDATE_SHELLS = listOf(
        "$TERMUX_BIN/bash",
        "$TERMUX_BIN/zsh",
        "$TERMUX_BIN/sh",
        "$TERMUX_BIN/login",
        "/system/bin/sh",
        "/system/xbin/sh",
        "/bin/sh"
    )

    data class LocalShellEnvironment(
        val shellPath: String,
        val isTermux: Boolean,
        val workingDir: File?,
        val environmentVariables: Map<String, String>,
        val availableShells: List<String>
    )

    /**
     * Checks whether the Termux files directory or binaries are accessible.
     */
    fun isTermuxAvailable(): Boolean {
        return try {
            val termuxUsr = File(TERMUX_PREFIX)
            val termuxBash = File("$TERMUX_BIN/bash")
            (termuxUsr.exists() && termuxUsr.isDirectory) || termuxBash.exists()
        } catch (e: Throwable) {
            BinBoxLogger.d("TermuxDiscovery", "Termux availability check failed: ${e.message}")
            false
        }
    }

    /**
     * Finds all executable shells present on the device.
     */
    fun discoverAvailableShells(): List<String> {
        val found = mutableListOf<String>()
        for (path in CANDIDATE_SHELLS) {
            try {
                val file = File(path)
                if (file.exists() && (file.canExecute() || path.startsWith("/system/bin"))) {
                    found.add(path)
                }
            } catch (e: Throwable) {
                // Ignore permission or security exceptions
            }
        }
        if (found.isEmpty()) {
            found.add("/system/bin/sh")
        }
        return found.distinct()
    }

    /**
     * Builds default environment variables for Termux.
     */
    fun buildTermuxEnvironment(): Map<String, String> {
        val env = mutableMapOf<String, String>()
        env["TERM"] = "xterm-256color"
        env["COLORTERM"] = "truecolor"
        env["PREFIX"] = TERMUX_PREFIX
        env["HOME"] = TERMUX_HOME
        env["PATH"] = "$TERMUX_BIN:$TERMUX_BIN/applets:/system/bin:/system/xbin"
        env["LD_LIBRARY_PATH"] = TERMUX_LIB
        env["TMPDIR"] = TERMUX_TMP
        env["LANG"] = "en_US.UTF-8"
        env["PS1"] = "\\u@\\h:\\w\\$ "
        return env
    }

    /**
     * Builds default environment variables for Android system shell.
     */
    fun buildSystemEnvironment(): Map<String, String> {
        val env = mutableMapOf<String, String>()
        env["TERM"] = "xterm-256color"
        env["COLORTERM"] = "truecolor"
        env["PATH"] = "/system/bin:/system/xbin:/sbin:/vendor/bin"
        env["LANG"] = "en_US.UTF-8"
        env["PS1"] = "\\u@localhost:\\w\\$ "
        return env
    }

    /**
     * Detects the best available local shell environment (preferring Termux if installed,
     * falling back to /system/bin/sh).
     */
    fun detectBestShell(preferTermux: Boolean = true): LocalShellEnvironment {
        val available = discoverAvailableShells()
        val termuxAvailable = isTermuxAvailable()

        return if (preferTermux && termuxAvailable) {
            val termuxShell = available.firstOrNull { it.startsWith(TERMUX_PREFIX) }
                ?: "$TERMUX_BIN/bash"
            val home = File(TERMUX_HOME)
            val workDir = if (home.exists() && home.isDirectory) home else null
            LocalShellEnvironment(
                shellPath = termuxShell,
                isTermux = true,
                workingDir = workDir,
                environmentVariables = buildTermuxEnvironment(),
                availableShells = available
            )
        } else {
            val systemShell = available.firstOrNull { it.startsWith("/system") || it == "/bin/sh" }
                ?: "/system/bin/sh"
            LocalShellEnvironment(
                shellPath = systemShell,
                isTermux = false,
                workingDir = null,
                environmentVariables = buildSystemEnvironment(),
                availableShells = available
            )
        }
    }
}
