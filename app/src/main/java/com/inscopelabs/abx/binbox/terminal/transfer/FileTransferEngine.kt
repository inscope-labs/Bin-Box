package com.inscopelabs.abx.binbox.terminal.transfer

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Base64
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.terminal.engine.ShellSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.InputStream
import java.text.DecimalFormat

enum class TransferStatus {
    IDLE,
    SCANNING,
    PACKING,
    STREAMING,
    COMPLETED,
    ERROR,
    CANCELLED
}

data class TransferProgress(
    val status: TransferStatus = TransferStatus.IDLE,
    val progress: Float = 0f,
    val transferredBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val totalFiles: Int = 0,
    val currentItemName: String = "",
    val errorMessage: String? = null
) {
    val formattedTransferred: String get() = formatBytes(transferredBytes)
    val formattedTotal: String get() = formatBytes(totalBytes)
    val percentageInt: Int get() = (progress * 100f).toInt().coerceIn(0, 100)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
        }
    }
}

class FileTransferEngine(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val TAG = "FileTransferEngine"

    private val _progress = MutableStateFlow(TransferProgress())
    val progress: StateFlow<TransferProgress> = _progress.asStateFlow()

    private var activeJob: Job? = null

    fun reset() {
        BinBoxLogger.d(TAG, "Resetting file transfer progress state")
        activeJob?.cancel()
        _progress.value = TransferProgress()
    }

    fun cancel() {
        BinBoxLogger.w(TAG, "Cancelling active file transfer")
        activeJob?.cancel()
        _progress.value = _progress.value.copy(
            status = TransferStatus.CANCELLED,
            errorMessage = "Transfer cancelled by user"
        )
    }

    /**
     * Inspects and transfers a single file or an entire directory tree into the active shell session's current directory.
     */
    fun startTransfer(
        session: ShellSession,
        uri: Uri,
        isDirectory: Boolean,
        chunkSize: Int = 2048,
        chunkDelayMs: Long = 15L
    ) {
        BinBoxLogger.i(TAG, "Initiating file transfer: uri=$uri, isDirectory=$isDirectory, session=${session.id}")
        activeJob?.cancel()

        activeJob = scope.launch {
            try {
                _progress.value = TransferProgress(
                    status = TransferStatus.SCANNING,
                    currentItemName = "Scanning files..."
                )

                val entries = if (isDirectory) {
                    scanDirectoryTree(context.contentResolver, uri)
                } else {
                    scanSingleFile(context.contentResolver, uri)
                }

                if (entries.isEmpty()) {
                    throw IllegalStateException("No files selected or unable to read target")
                }

                val totalRawBytes = entries.sumOf { if (it.isDirectory) 0L else it.sizeBytes }
                val totalFileCount = entries.count { !it.isDirectory }
                val rootLabel = entries.firstOrNull()?.relativePath?.trimEnd('/') ?: "Payload"

                _progress.value = TransferProgress(
                    status = TransferStatus.PACKING,
                    totalFiles = totalFileCount,
                    totalBytes = totalRawBytes,
                    currentItemName = "Packaging $rootLabel..."
                )

                // Package to .tar.gz payload
                val tarGzBytes = TarStreamPacker.packToTarGz(entries)
                val base64Text = Base64.encodeToString(tarGzBytes, Base64.NO_WRAP)
                val totalBase64Len = base64Text.length.toLong()

                BinBoxLogger.d(TAG, "Generated compressed payload size: ${tarGzBytes.size} bytes (Base64 string length: $totalBase64Len)")

                _progress.value = TransferProgress(
                    status = TransferStatus.STREAMING,
                    progress = 0f,
                    transferredBytes = 0L,
                    totalBytes = totalBase64Len,
                    totalFiles = totalFileCount,
                    currentItemName = rootLabel
                )

                // 1. Send shell unpack command to active session
                // We execute base64 decoding piped directly into tar extraction
                val unpackCommand = "base64 -d | tar -xzf -\n"
                session.sendRawBytes(unpackCommand.toByteArray(Charsets.UTF_8))
                delay(100)

                // 2. Stream base64 chunks
                var offset = 0
                while (offset < base64Text.length) {
                    ensureActive()
                    val end = minOf(offset + chunkSize, base64Text.length)
                    val chunk = base64Text.substring(offset, end)
                    session.sendRawBytes(chunk.toByteArray(Charsets.UTF_8))

                    offset = end
                    val frac = (offset.toFloat() / totalBase64Len.toFloat()).coerceIn(0f, 1f)
                    _progress.value = _progress.value.copy(
                        progress = frac,
                        transferredBytes = offset.toLong()
                    )

                    if (chunkDelayMs > 0) {
                        delay(chunkDelayMs)
                    }
                }

                // 3. Send newline and EOF (ASCII EOT = 0x04) to complete remote stream
                session.sendRawBytes("\n".toByteArray(Charsets.UTF_8))
                delay(50)
                session.sendRawBytes(byteArrayOf(4)) // Ctrl+D / EOF
                delay(200)

                BinBoxLogger.i(TAG, "Successfully transferred $totalFileCount files ($totalBase64Len Base64 bytes) to shell session")
                _progress.value = TransferProgress(
                    status = TransferStatus.COMPLETED,
                    progress = 1f,
                    transferredBytes = totalBase64Len,
                    totalBytes = totalBase64Len,
                    totalFiles = totalFileCount,
                    currentItemName = rootLabel
                )
            } catch (e: CancellationException) {
                BinBoxLogger.w(TAG, "Transfer was cancelled")
                _progress.value = _progress.value.copy(
                    status = TransferStatus.CANCELLED,
                    errorMessage = "Transfer cancelled"
                )
            } catch (e: Throwable) {
                BinBoxLogger.e(TAG, "Transfer failed: ${e.message}", e)
                _progress.value = _progress.value.copy(
                    status = TransferStatus.ERROR,
                    errorMessage = e.message ?: "Transfer encountered an error"
                )
            }
        }
    }

    private fun scanSingleFile(contentResolver: ContentResolver, uri: Uri): List<TarStreamPacker.TarEntry> {
        var displayName = "uploaded_file"
        var size = 0L

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx != -1) displayName = cursor.getString(nameIdx) ?: displayName
                    if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                }
            }
        } catch (e: Throwable) {
            BinBoxLogger.w(TAG, "Failed querying file metadata: ${e.message}")
        }

        if (size <= 0L) {
            // Read bytes to discover length if size was unrecorded by provider
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    var count = 0L
                    val buf = ByteArray(4096)
                    while (true) {
                        val r = stream.read(buf)
                        if (r <= 0) break
                        count += r
                    }
                    size = count
                }
            } catch (_: Throwable) {}
        }

        return listOf(
            TarStreamPacker.TarEntry(
                relativePath = displayName,
                isDirectory = false,
                sizeBytes = size,
                inputStreamProvider = { contentResolver.openInputStream(uri) ?: throw IllegalStateException("Cannot open input stream for $uri") }
            )
        )
    }

    private fun scanDirectoryTree(contentResolver: ContentResolver, treeUri: Uri): List<TarStreamPacker.TarEntry> {
        val entries = mutableListOf<TarStreamPacker.TarEntry>()
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)

        // Query root display name
        var rootDirName = "archive"
        try {
            val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
            contentResolver.query(rootDocUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    rootDirName = cursor.getString(0) ?: rootDirName
                }
            }
        } catch (e: Throwable) {
            BinBoxLogger.w(TAG, "Failed resolving tree root name: ${e.message}")
        }

        entries.add(TarStreamPacker.TarEntry(relativePath = rootDirName, isDirectory = true, sizeBytes = 0L))
        recurseTree(contentResolver, treeUri, rootDocId, rootDirName, entries)
        return entries
    }

    private fun recurseTree(
        contentResolver: ContentResolver,
        treeUri: Uri,
        parentDocId: String,
        currentPath: String,
        outList: MutableList<TarStreamPacker.TarEntry>
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        try {
            contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol)
                    val name = cursor.getString(nameCol) ?: "unnamed"
                    val mime = cursor.getString(mimeCol)
                    val isDir = (mime == DocumentsContract.Document.MIME_TYPE_DIR)
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L

                    val relPath = "$currentPath/$name"
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

                    if (isDir) {
                        outList.add(TarStreamPacker.TarEntry(relativePath = relPath, isDirectory = true, sizeBytes = 0L))
                        recurseTree(contentResolver, treeUri, docId, relPath, outList)
                    } else {
                        outList.add(
                            TarStreamPacker.TarEntry(
                                relativePath = relPath,
                                isDirectory = false,
                                sizeBytes = size,
                                inputStreamProvider = { contentResolver.openInputStream(docUri) ?: throw IllegalStateException("Cannot read child $docUri") }
                            )
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            BinBoxLogger.w(TAG, "Error listing tree children for $parentDocId: ${e.message}")
        }
    }
}
