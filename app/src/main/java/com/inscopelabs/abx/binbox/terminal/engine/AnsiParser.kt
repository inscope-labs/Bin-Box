package com.inscopelabs.abx.binbox.terminal.engine

import com.inscopelabs.abx.binbox.terminal.model.*

class AnsiParser(
    override var theme: TerminalThemePreset,
    private val onBell: (() -> Unit)? = null,
    private val onTitleChange: ((String) -> Unit)? = null
) : AnsiCsiTarget {
    override var currentStyle = TerminalStyle()
    
    // Primary buffer
    private val primaryBuffer = mutableListOf<TerminalLine>()
    // Alternate screen buffer (e.g. for vim, htop, nano)
    override val alternateBuffer = mutableListOf<TerminalLine>()
    override var isAlternateBufferActive = false

    override val currentBuffer: MutableList<TerminalLine>
        get() = if (isAlternateBufferActive) alternateBuffer else primaryBuffer

    override val currentLineSegments = mutableListOf<StyledSegment>()
    override val currentSegmentBuilder = StringBuilder()

    // Cursor position & visibility
    override var cursorRow: Int = 0
    override var cursorCol: Int = 0
    override var isCursorVisible: Boolean = true
    override var isBracketedPasteMode: Boolean = false

    // Saved cursor state (DECSC / DECRC)
    override var savedCursorRow = 0
    override var savedCursorCol = 0
    override var savedStyle = TerminalStyle()

    var maxScrollback: Int = 3000

    fun updateTheme(newTheme: TerminalThemePreset) {
        this.theme = newTheme
    }

    @Synchronized
    fun getLines(): List<TerminalLine> {
        val activeBuf = currentBuffer
        val result = ArrayList<TerminalLine>(activeBuf.size + 1)
        result.addAll(activeBuf)
        if (currentSegmentBuilder.isNotEmpty() || currentLineSegments.isNotEmpty()) {
            val pendingSegments = ArrayList(currentLineSegments)
            if (currentSegmentBuilder.isNotEmpty()) {
                pendingSegments.add(StyledSegment(currentSegmentBuilder.toString(), currentStyle))
            }
            result.add(TerminalLine(pendingSegments))
        }
        return result
    }

    @Synchronized
    fun clear() {
        primaryBuffer.clear()
        alternateBuffer.clear()
        currentLineSegments.clear()
        currentSegmentBuilder.clear()
        currentStyle = TerminalStyle()
        cursorRow = 0
        cursorCol = 0
    }

    @Synchronized
    fun reset() {
        clear()
        isAlternateBufferActive = false
        isCursorVisible = true
        isBracketedPasteMode = false
        savedCursorRow = 0
        savedCursorCol = 0
        savedStyle = TerminalStyle()
    }

    @Synchronized
    fun search(query: String, ignoreCase: Boolean = true): TerminalSearchResults {
        if (query.isBlank()) return TerminalSearchResults(query = query)
        val allLines = getLines()
        val matches = mutableListOf<SearchMatch>()

        for ((lineIdx, line) in allLines.withIndex()) {
            val lineText = line.rawText
            var startIndex = 0
            while (startIndex < lineText.length) {
                val matchIndex = lineText.indexOf(query, startIndex, ignoreCase = ignoreCase)
                if (matchIndex == -1) break
                val endOffset = matchIndex + query.length
                matches.add(
                    SearchMatch(
                        lineIndex = lineIdx,
                        startOffset = matchIndex,
                        endOffset = endOffset,
                        text = lineText.substring(matchIndex, endOffset)
                    )
                )
                startIndex = matchIndex + 1
            }
        }
        return TerminalSearchResults(
            query = query,
            matches = matches,
            currentMatchIndex = if (matches.isNotEmpty()) 0 else 0
        )
    }

    @Synchronized
    fun exportPlainText(): String {
        return getLines().joinToString("\n") { it.rawText }
    }

    @Synchronized
    fun feed(input: String) {
        var i = 0
        val len = input.length

        while (i < len) {
            val ch = input[i]

            when {
                // Bell character (\a)
                ch == '\u0007' -> {
                    onBell?.invoke()
                    i++
                }

                // Carriage Return (\r)
                ch == '\r' -> {
                    if (i + 1 < len && input[i + 1] == '\n') {
                        flushCurrentSegment()
                        commitCurrentLine()
                        i += 2
                    } else {
                        flushCurrentSegment()
                        currentLineSegments.clear()
                        currentSegmentBuilder.clear()
                        cursorCol = 0
                        i++
                    }
                }

                // Newline (\n)
                ch == '\n' -> {
                    flushCurrentSegment()
                    commitCurrentLine()
                    i++
                }

                // Backspace (\b)
                ch == '\b' -> {
                    if (currentSegmentBuilder.isNotEmpty()) {
                        currentSegmentBuilder.deleteCharAt(currentSegmentBuilder.length - 1)
                        if (cursorCol > 0) cursorCol--
                    } else if (currentLineSegments.isNotEmpty()) {
                        val last = currentLineSegments.removeAt(currentLineSegments.lastIndex)
                        if (last.text.length > 1) {
                            currentLineSegments.add(
                                last.copy(text = last.text.substring(0, last.text.length - 1))
                            )
                        }
                        if (cursorCol > 0) cursorCol--
                    }
                    i++
                }

                // Tab (\t)
                ch == '\t' -> {
                    val tabSpaces = 4 - (cursorCol % 4)
                    currentSegmentBuilder.append(" ".repeat(tabSpaces.coerceAtLeast(1)))
                    cursorCol += tabSpaces
                    i++
                }

                // ANSI Escape sequence start (\u001b)
                ch == '\u001B' -> {
                    if (i + 1 < len) {
                        val next = input[i + 1]
                        when (next) {
                            '[' -> {
                                var endIdx = i + 2
                                while (endIdx < len && !AnsiCsiHandler.isCsiTerminator(input[endIdx])) {
                                    endIdx++
                                }

                                if (endIdx < len) {
                                    val csiCode = input.substring(i + 2, endIdx)
                                    val command = input[endIdx]
                                    AnsiCsiHandler.handleCsi(this, csiCode, command)
                                    i = endIdx + 1
                                } else {
                                    i = len
                                }
                            }
                            ']' -> {
                                var endIdx = i + 2
                                while (endIdx < len && input[endIdx] != '\u0007' && input[endIdx] != '\u001B') {
                                    endIdx++
                                }
                                if (endIdx < len && input[endIdx] == '\u001B' && endIdx + 1 < len && input[endIdx + 1] == '\\') {
                                    handleOsc(input.substring(i + 2, endIdx))
                                    i = endIdx + 2
                                } else if (endIdx < len && input[endIdx] == '\u0007') {
                                    handleOsc(input.substring(i + 2, endIdx))
                                    i = endIdx + 1
                                } else {
                                    i = len
                                }
                            }
                            '7' -> {
                                savedCursorRow = cursorRow
                                savedCursorCol = cursorCol
                                savedStyle = currentStyle
                                i += 2
                            }
                            '8' -> {
                                cursorRow = savedCursorRow
                                cursorCol = savedCursorCol
                                currentStyle = savedStyle
                                i += 2
                            }
                            'c' -> {
                                reset()
                                i += 2
                            }
                            else -> {
                                i += 2
                            }
                        }
                    } else {
                        i++
                    }
                }

                else -> {
                    currentSegmentBuilder.append(ch)
                    cursorCol++
                    i++
                }
            }
        }

        // Limit scrollback buffer in non-alternate mode
        if (!isAlternateBufferActive && primaryBuffer.size > maxScrollback) {
            val toRemove = primaryBuffer.size - maxScrollback
            repeat(toRemove) {
                if (primaryBuffer.isNotEmpty()) primaryBuffer.removeAt(0)
            }
        }
    }

    private fun handleOsc(content: String) {
        val parts = content.split(';', limit = 2)
        if (parts.size == 2) {
            val code = parts[0]
            val value = parts[1]
            if (code == "0" || code == "2") {
                onTitleChange?.invoke(value)
            }
        }
    }

    override fun flushCurrentSegment() {
        if (currentSegmentBuilder.isNotEmpty()) {
            currentLineSegments.add(StyledSegment(currentSegmentBuilder.toString(), currentStyle))
            currentSegmentBuilder.clear()
        }
    }

    private fun commitCurrentLine() {
        currentBuffer.add(TerminalLine(ArrayList(currentLineSegments)))
        currentLineSegments.clear()
        cursorRow++
        cursorCol = 0
    }
}
