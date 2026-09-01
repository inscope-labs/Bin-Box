package com.inscopelabs.abx.binbox.binboxshell.runtime

import android.content.Context
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import java.io.File

/**
 * Manages sandbox filesystem directory paths for the BinBox local shell environment.
 */
class RuntimePaths(private val context: Context) {

    val rootDir: File = File(context.filesDir, "shell")
    val homeDir: File = File(rootDir, "home")
    val tmpDir: File = File(rootDir, "tmp")
    val binDir: File = File(rootDir, "bin")
    val etcDir: File = File(rootDir, "etc")
    val cacheDir: File = File(context.cacheDir, "shell")

    val nativeLibDir: File? = context.applicationInfo.nativeLibraryDir?.let { File(it) }

    init {
        ensureDirectories()
    }

    fun ensureDirectories() {
        listOf(rootDir, homeDir, tmpDir, binDir, etcDir, cacheDir).forEach { dir ->
            if (!dir.exists()) {
                val created = dir.mkdirs()
                BinBoxLogger.d(TAG, "Created directory ${dir.absolutePath}: $created")
            }
        }
    }

    fun getEtcFile(fileName: String): File = File(etcDir, fileName)

    fun getBinFile(binaryName: String): File = File(binDir, binaryName)

    companion object {
        private const val TAG = "RuntimePaths"
    }
}
