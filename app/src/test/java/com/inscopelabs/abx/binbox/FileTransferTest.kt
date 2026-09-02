package com.inscopelabs.abx.binbox

import android.util.Base64
import com.inscopelabs.abx.binbox.terminal.engine.ShellSession
import com.inscopelabs.abx.binbox.terminal.engine.TerminalKey
import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.inscopelabs.abx.binbox.terminal.model.TerminalLine
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemePreset
import com.inscopelabs.abx.binbox.terminal.transfer.FileTransferEngine
import com.inscopelabs.abx.binbox.terminal.transfer.TarStreamPacker
import com.inscopelabs.abx.binbox.terminal.transfer.TransferProgress
import com.inscopelabs.abx.binbox.terminal.transfer.TransferStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream

@RunWith(RobolectricTestRunner::class)
class FileTransferTest {

    private class MockShellSession : ShellSession {
        override val id: String = "test-session-123"
        override var title: String = "Test Shell"
        override val hostLabel: String = "Ubuntu VM"
        override val state: StateFlow<SessionState> = MutableStateFlow(SessionState.Connected)
        override val lines: StateFlow<List<TerminalLine>> = MutableStateFlow(emptyList())
        override val rawLogText: String = ""

        val rawBytesSent = mutableListOf<ByteArray>()

        override fun start() {}
        override fun sendInput(text: String) {}
        override fun sendSpecialKey(key: TerminalKey) {}
        override fun sendRawBytes(bytes: ByteArray) {
            rawBytesSent.add(bytes)
        }
        override fun clear() {}
        override fun disconnect() {}
        override fun updateTheme(theme: TerminalThemePreset) {}
    }

    @Test
    fun tarStreamPacker_createsValidGzipTarballForSingleFile() {
        val fileContent = "Hello from BinBox Android file transfer!".toByteArray(Charsets.UTF_8)
        val entry = TarStreamPacker.TarEntry(
            relativePath = "test.txt",
            isDirectory = false,
            sizeBytes = fileContent.size.toLong(),
            inputStreamProvider = { ByteArrayInputStream(fileContent) }
        )

        val tarGz = TarStreamPacker.packToTarGz(listOf(entry))
        assertTrue(tarGz.isNotEmpty())

        // Decompress GZIP to verify tar structure
        val gzipIn = GZIPInputStream(ByteArrayInputStream(tarGz))
        val tarBytes = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (true) {
            val r = gzipIn.read(buffer)
            if (r <= 0) break
            tarBytes.write(buffer, 0, r)
        }
        val uncompressedTar = tarBytes.toByteArray()

        // POSIX ustar block size is multiple of 512
        assertEquals(0, uncompressedTar.size % 512)
        assertTrue(uncompressedTar.size >= 512 * 3) // header + 1 block content + 2 EOF blocks

        // Inspect header filename
        val headerName = String(uncompressedTar, 0, 100, Charsets.UTF_8).trimEnd('\u0000')
        assertEquals("test.txt", headerName)

        // Inspect ustar magic
        val magic = String(uncompressedTar, 257, 5, Charsets.US_ASCII)
        assertEquals("ustar", magic)
    }

    @Test
    fun tarStreamPacker_createsValidTarballForDirectoriesAndFiles() {
        val entryDir = TarStreamPacker.TarEntry(
            relativePath = "my_project",
            isDirectory = true,
            sizeBytes = 0L
        )

        val file1Content = "print('hello world')\n".toByteArray(Charsets.UTF_8)
        val entryFile1 = TarStreamPacker.TarEntry(
            relativePath = "my_project/main.py",
            isDirectory = false,
            sizeBytes = file1Content.size.toLong(),
            inputStreamProvider = { ByteArrayInputStream(file1Content) }
        )

        val file2Content = "requirements\n".toByteArray(Charsets.UTF_8)
        val entryFile2 = TarStreamPacker.TarEntry(
            relativePath = "my_project/req.txt",
            isDirectory = false,
            sizeBytes = file2Content.size.toLong(),
            inputStreamProvider = { ByteArrayInputStream(file2Content) }
        )

        val tarGz = TarStreamPacker.packToTarGz(listOf(entryDir, entryFile1, entryFile2))
        assertTrue(tarGz.isNotEmpty())

        // Decompress and verify
        val gzipIn = GZIPInputStream(ByteArrayInputStream(tarGz))
        val tarBytes = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (true) {
            val r = gzipIn.read(buffer)
            if (r <= 0) break
            tarBytes.write(buffer, 0, r)
        }
        val uncompressed = tarBytes.toByteArray()
        assertTrue(uncompressed.isNotEmpty())
        assertEquals(0, uncompressed.size % 512)
    }

    @Test
    fun transferProgress_formattingHelperTests() {
        assertEquals("0 B", TransferProgress.formatBytes(0L))
        assertEquals("500 B", TransferProgress.formatBytes(500L))
        assertEquals("1.5 KB", TransferProgress.formatBytes(1536L))
        assertEquals("2 MB", TransferProgress.formatBytes(2097152L))

        val progress = TransferProgress(
            status = TransferStatus.STREAMING,
            progress = 0.75f,
            transferredBytes = 1536L,
            totalBytes = 2048L
        )
        assertEquals(75, progress.percentageInt)
        assertEquals("1.5 KB", progress.formattedTransferred)
        assertEquals("2 KB", progress.formattedTotal)
    }
}
