package com.inscopelabs.abx.binbox.terminal.transfer

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.GZIPOutputStream

/**
 * Packs files and directories into a standard POSIX ustar tar.gz archive stream.
 * Zero external library dependencies, compliant with POSIX.1-1988 ustar specification.
 */
object TarStreamPacker {
    private const val TAG = "TarStreamPacker"
    private const val BLOCK_SIZE = 512

    data class TarEntry(
        val relativePath: String,
        val isDirectory: Boolean,
        val sizeBytes: Long,
        val modificationTimeSec: Long = System.currentTimeMillis() / 1000L,
        val inputStreamProvider: (() -> InputStream)? = null
    )

    /**
     * Packages a collection of entries into a gzip-compressed tar byte array.
     */
    fun packToTarGz(entries: List<TarEntry>): ByteArray {
        BinBoxLogger.d(TAG, "Packing ${entries.size} entries into tar.gz payload")
        val byteOut = ByteArrayOutputStream()
        GZIPOutputStream(byteOut).use { gzipOut ->
            for (entry in entries) {
                writeTarHeader(gzipOut, entry)
                if (!entry.isDirectory && entry.inputStreamProvider != null) {
                    writeFileContent(gzipOut, entry)
                }
            }
            // End of archive: two 512-byte zero blocks (1024 bytes)
            gzipOut.write(ByteArray(BLOCK_SIZE * 2))
            gzipOut.flush()
        }
        val result = byteOut.toByteArray()
        BinBoxLogger.i(TAG, "Completed tar.gz packaging: ${result.size} compressed bytes for ${entries.size} entries")
        return result
    }

    private fun writeTarHeader(out: java.io.OutputStream, entry: TarEntry) {
        val header = ByteArray(BLOCK_SIZE)
        var name = entry.relativePath.replace('\\', '/')
        if (entry.isDirectory && !name.endsWith('/')) {
            name += "/"
        }

        // 1. File name (0..99)
        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
        val copyLen = minOf(nameBytes.size, 100)
        System.arraycopy(nameBytes, 0, header, 0, copyLen)

        // 2. File mode (100..107) -> "0000755\0" for dir, "0000644\0" for file
        val modeStr = if (entry.isDirectory) "0000755\u0000" else "0000644\u0000"
        putOctalString(header, 100, modeStr)

        // 3. UID (108..115) -> "0000000\0"
        putOctalString(header, 108, "0000000\u0000")

        // 4. GID (116..123) -> "0000000\0"
        putOctalString(header, 116, "0000000\u0000")

        // 5. Size (124..135) -> 11 octal digits + space
        val sizeVal = if (entry.isDirectory) 0L else entry.sizeBytes
        val sizeStr = String.format(Locale.US, "%011o ", sizeVal)
        putOctalString(header, 124, sizeStr)

        // 6. Mtime (136..147) -> 11 octal digits + space
        val mtimeStr = String.format(Locale.US, "%011o ", entry.modificationTimeSec)
        putOctalString(header, 136, mtimeStr)

        // 7. Typeflag (156) -> '5' for directory, '0' for regular file
        header[156] = if (entry.isDirectory) '5'.code.toByte() else '0'.code.toByte()

        // 8. Magic (257..262) -> "ustar\0"
        val magicBytes = "ustar\u0000".toByteArray(StandardCharsets.US_ASCII)
        System.arraycopy(magicBytes, 0, header, 257, magicBytes.size)

        // 9. Version (263..264) -> "00"
        header[263] = '0'.code.toByte()
        header[264] = '0'.code.toByte()

        // 10. Uname & Gname (265..328)
        val unameBytes = "binbox\u0000".toByteArray(StandardCharsets.US_ASCII)
        System.arraycopy(unameBytes, 0, header, 265, unameBytes.size)
        System.arraycopy(unameBytes, 0, header, 297, unameBytes.size)

        // 11. Calculate and write Header Checksum (148..155)
        // Checksum field is treated as 8 spaces (0x20) during calculation
        for (i in 148..155) {
            header[i] = ' '.code.toByte()
        }
        var chksum = 0L
        for (b in header) {
            chksum += (b.toInt() and 0xFF)
        }
        val chksumStr = String.format(Locale.US, "%06o\u0000 ", chksum)
        val chksumBytes = chksumStr.toByteArray(StandardCharsets.US_ASCII)
        System.arraycopy(chksumBytes, 0, header, 148, minOf(chksumBytes.size, 8))

        out.write(header)
    }

    private fun writeFileContent(out: java.io.OutputStream, entry: TarEntry) {
        val stream = entry.inputStreamProvider?.invoke() ?: return
        var totalRead = 0L
        val buffer = ByteArray(4096)
        stream.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                out.write(buffer, 0, read)
                totalRead += read
            }
        }
        // Pad to 512-byte block boundary
        val remainder = (totalRead % BLOCK_SIZE).toInt()
        if (remainder > 0) {
            val padLen = BLOCK_SIZE - remainder
            out.write(ByteArray(padLen))
        }
    }

    private fun putOctalString(header: ByteArray, offset: Int, value: String) {
        val bytes = value.toByteArray(StandardCharsets.US_ASCII)
        System.arraycopy(bytes, 0, header, offset, bytes.size)
    }
}
