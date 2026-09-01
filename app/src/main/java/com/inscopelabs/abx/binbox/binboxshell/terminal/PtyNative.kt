package com.inscopelabs.abx.binbox.binboxshell.terminal

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import java.io.FileDescriptor

/**
 * Native PTY (Pseudo-Terminal) interface with fallback detection.
 */
object PtyNative {
    private const val TAG = "PtyNative"
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("pty")
            isLoaded = true
            BinBoxLogger.i(TAG, "Native libpty loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            isLoaded = false
            BinBoxLogger.d(TAG, "Native libpty not present, standard process fallback enabled: ${e.message}")
        } catch (e: Exception) {
            isLoaded = false
            BinBoxLogger.w(TAG, "Unexpected error loading libpty: ${e.message}")
        }
    }

    val isAvailable: Boolean
        get() = isLoaded

    external fun nativeCreatePty(cols: Int, rows: Int): IntArray?
    external fun nativeForkAndExec(
        cmd: Array<String>,
        environment: Array<String>,
        workingDir: String,
        slaveFd: Int
    ): Int
    external fun nativeSetPtyWindowSize(masterFd: Int, cols: Int, rows: Int, widthPx: Int, heightPx: Int): Int
    external fun nativeCloseFd(fd: Int): Int

    fun setPtySize(masterFd: Int, cols: Int, rows: Int, widthPx: Int = 0, heightPx: Int = 0): Boolean {
        if (!isLoaded) return false
        return try {
            nativeSetPtyWindowSize(masterFd, cols, rows, widthPx, heightPx) == 0
        } catch (e: Exception) {
            BinBoxLogger.w(TAG, "Failed to set PTY window size: ${e.message}")
            false
        }
    }
}
