package com.inscopelabs.abx.binbox.terminal.engine

import androidx.compose.ui.graphics.Color
import com.inscopelabs.abx.binbox.terminal.model.*

class AnsiParser(
    private var theme: TerminalThemePreset,
    private val onBell: (() -> Unit)? = null,
    private val onTitleChange: ((String) -> Unit)? = null
) {
    private var currentStyle = TerminalStyle()
    
    // Primary buffer
    private val primaryBuffer = mutableListOf<TerminalLine>()
    // Alternate screen buffer (e.g. for vim, htop, nano)
    private val alternateBuffer = mutableListOf<TerminalLine>()
    private var isAlternateBufferActive = false

    private val currentBuffer: MutableList<TerminalLine>
        get() = if (isAlternateBufferActive) alternateBuffer else primaryBuffer

    private var currentLineSegments = mutableListOf<StyledSegment>()
    private var currentSegmentBuilder = StringBuilder()

    // Cursor position & visibility
    var cursorRow: Int = 0
        private set
    var cursorCol: Int = 0
        private set
    var isCursorVisible: Boolean = true
        private set
    var isBracketedPasteMode: Boolean = false
        private set

    // Saved cursor state (DECSC / DECRC)
    private var savedCursorRow = 0
    private var savedCursorCol = 0
    private var savedStyle = TerminalStyle()

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
                        // CR without LF: move cursor back to beginning of line (overwrites line)
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
                                // CSI sequence: parse till terminating character
                                var endIdx = i + 2
                                while (endIdx < len && !isCsiTerminator(input[endIdx])) {
                                    endIdx++
                                }

                                if (endIdx < len) {
                                    val csiCode = input.substring(i + 2, endIdx)
                                    val command = input[endIdx]
                                    handleCsi(csiCode, command)
                                    i = endIdx + 1
                                } else {
                                    i = len
                                }
                            }
                            ']' -> {
                                // OSC sequence: \u001b] ... \u0007 or \u001b\
                                var endIdx = i + 2
                                while (endIdx < len && input[endIdx] != '\u0007' && input[endIdx] != '\u001B') {
                                    endIdx++
                                }
                                if (endIdx < len && input[endIdx] == '\u001B' && endIdx + 1 < len && input[endIdx + 1] == '\\') {
                                    val oscContent = input.substring(i + 2, endIdx)
                                    handleOsc(oscContent)
                                    i = endIdx + 2
                                } else if (endIdx < len && input[endIdx] == '\u0007') {
                                    val oscContent = input.substring(i + 2, endIdx)
                                    handleOsc(oscContent)
                                    i = endIdx + 1
                                } else {
                                    i = len
                                }
                            }
                            '7' -> {
                                // Save cursor state (DECSC)
                                savedCursorRow = cursorRow
                                savedCursorCol = cursorCol
                                savedStyle = currentStyle
                                i += 2
                            }
                            '8' -> {
                                // Restore cursor state (DECRC)
                                cursorRow = savedCursorRow
                                cursorCol = savedCursorCol
                                currentStyle = savedStyle
                                i += 2
                            }
                            'c' -> {
                                // Full reset (RIS)
                                reset()
                                i += 2
                            }
                            else -> {
                                // Unrecognized single char escape
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

    private fun isCsiTerminator(c: Char): Boolean {
        return c in '@'..'~'
    }

    private fun handleOsc(content: String) {
        // OSC commands: "0;Title", "2;Title", etc.
        val parts = content.split(';', limit = 2)
        if (parts.size == 2) {
            val code = parts[0]
            val value = parts[1]
            if (code == "0" || code == "2") {
                onTitleChange?.invoke(value)
            }
        }
    }

    private fun handleCsi(paramsStr: String, command: Char) {
        when (command) {
            'm' -> {
                // Select Graphic Rendition (SGR)
                flushCurrentSegment()
                parseSgr(paramsStr)
            }
            'J' -> {
                // Erase in Display
                flushCurrentSegment()
                when (paramsStr) {
                    "2", "3" -> {
                        currentBuffer.clear()
                        currentLineSegments.clear()
                        currentSegmentBuilder.clear()
                        cursorRow = 0
                        cursorCol = 0
                    }
                    "1" -> {
                        // Clear from beginning to cursor
                        currentSegmentBuilder.clear()
                        currentLineSegments.clear()
                    }
                    "0", "" -> {
                        // Clear from cursor to end
                        currentSegmentBuilder.clear()
                    }
                }
            }
            'K' -> {
                // Erase in Line
                when (paramsStr) {
                    "2" -> {
                        currentSegmentBuilder.clear()
                        currentLineSegments.clear()
                        cursorCol = 0
                    }
                    "1" -> {
                        // Clear line left of cursor
                        currentSegmentBuilder.clear()
                    }
                    "0", "" -> {
                        // Clear line right of cursor
                        currentSegmentBuilder.clear()
                    }
                }
            }
            'H', 'f' -> {
                // Cursor Home / Position: CUP \e[row;colH (1-indexed)
                flushCurrentSegment()
                val parts = paramsStr.split(';').mapNotNull { it.toIntOrNull() }
                cursorRow = if (parts.isNotEmpty()) (parts[0] - 1).coerceAtLeast(0) else 0
                cursorCol = if (parts.size > 1) (parts[1] - 1).coerceAtLeast(0) else 0
            }
            'A' -> {
                // Cursor Up
                val count = paramsStr.toIntOrNull() ?: 1
                cursorRow = (cursorRow - count).coerceAtLeast(0)
            }
            'B' -> {
                // Cursor Down
                val count = paramsStr.toIntOrNull() ?: 1
                cursorRow += count
            }
            'C' -> {
                // Cursor Forward
                val count = paramsStr.toIntOrNull() ?: 1
                cursorCol += count
            }
            'D' -> {
                // Cursor Backward
                val count = paramsStr.toIntOrNull() ?: 1
                cursorCol = (cursorCol - count).coerceAtLeast(0)
            }
            'h', 'l' -> {
                val isEnable = (command == 'h')
                if (paramsStr.startsWith("?")) {
                    val mode = paramsStr.removePrefix("?").toIntOrNull()
                    when (mode) {
                        25 -> isCursorVisible = isEnable // DECTCEM Show/Hide Cursor
                        47, 1049 -> {
                            // Alternate screen buffer
                            if (isEnable && !isAlternateBufferActive) {
                                isAlternateBufferActive = true
                                alternateBuffer.clear()
                            } else if (!isEnable && isAlternateBufferActive) {
                                isAlternateBufferActive = false
                                alternateBuffer.clear()
                            }
                        }
                        2004 -> isBracketedPasteMode = isEnable // Bracketed paste mode
                    }
                }
            }
            's' -> {
                // Save cursor position
                savedCursorRow = cursorRow
                savedCursorCol = cursorCol
                savedStyle = currentStyle
            }
            'u' -> {
                // Restore cursor position
                cursorRow = savedCursorRow
                cursorCol = savedCursorCol
                currentStyle = savedStyle
            }
            else -> {
                // Other sequences flush segment safely
                flushCurrentSegment()
            }
        }
    }

    private fun parseSgr(paramsStr: String) {
        if (paramsStr.isEmpty()) {
            currentStyle = TerminalStyle()
            return
        }

        val tokens = paramsStr.split(';').mapNotNull { it.toIntOrNull() }
        if (tokens.isEmpty()) {
            currentStyle = TerminalStyle()
            return
        }

        var idx = 0
        while (idx < tokens.size) {
            val code = tokens[idx]
            when (code) {
                0 -> currentStyle = TerminalStyle()
                1 -> currentStyle = currentStyle.copy(isBold = true)
                2 -> currentStyle = currentStyle.copy(isDim = true)
                3 -> currentStyle = currentStyle.copy(isItalic = true)
                4 -> currentStyle = currentStyle.copy(isUnderline = true)
                7 -> currentStyle = currentStyle.copy(isInverted = true)
                9 -> currentStyle = currentStyle.copy(isStrikethrough = true)
                21 -> currentStyle = currentStyle.copy(isUnderline = true) // Double underline
                22 -> currentStyle = currentStyle.copy(isBold = false, isDim = false)
                23 -> currentStyle = currentStyle.copy(isItalic = false)
                24 -> currentStyle = currentStyle.copy(isUnderline = false)
                27 -> currentStyle = currentStyle.copy(isInverted = false)
                29 -> currentStyle = currentStyle.copy(isStrikethrough = false)

                // Standard Foreground (30-37)
                in 30..37 -> {
                    val colorIndex = code - 30
                    currentStyle = currentStyle.copy(foregroundColor = getPaletteColor(colorIndex))
                }
                // Extended Foreground (38;5;n or 38;2;r;g;b)
                38 -> {
                    if (idx + 2 < tokens.size && tokens[idx + 1] == 5) {
                        val color256 = tokens[idx + 2]
                        currentStyle = currentStyle.copy(foregroundColor = parse256Color(color256))
                        idx += 2
                    } else if (idx + 4 < tokens.size && tokens[idx + 1] == 2) {
                        val r = tokens[idx + 2].coerceIn(0, 255)
                        val g = tokens[idx + 3].coerceIn(0, 255)
                        val b = tokens[idx + 4].coerceIn(0, 255)
                        currentStyle = currentStyle.copy(foregroundColor = Color(r, g, b))
                        idx += 4
                    }
                }
                39 -> currentStyle = currentStyle.copy(foregroundColor = null)

                // Standard Background (40-47)
                in 40..47 -> {
                    val colorIndex = code - 40
                    currentStyle = currentStyle.copy(backgroundColor = getPaletteColor(colorIndex))
                }
                // Extended Background (48;5;n or 48;2;r;g;b)
                48 -> {
                    if (idx + 2 < tokens.size && tokens[idx + 1] == 5) {
                        val color256 = tokens[idx + 2]
                        currentStyle = currentStyle.copy(backgroundColor = parse256Color(color256))
                        idx += 2
                    } else if (idx + 4 < tokens.size && tokens[idx + 1] == 2) {
                        val r = tokens[idx + 2].coerceIn(0, 255)
                        val g = tokens[idx + 3].coerceIn(0, 255)
                        val b = tokens[idx + 4].coerceIn(0, 255)
                        currentStyle = currentStyle.copy(backgroundColor = Color(r, g, b))
                        idx += 4
                    }
                }
                49 -> currentStyle = currentStyle.copy(backgroundColor = null)

                // Bright Foreground (90-97)
                in 90..97 -> {
                    val colorIndex = (code - 90) + 8
                    currentStyle = currentStyle.copy(foregroundColor = getPaletteColor(colorIndex))
                }
                // Bright Background (100-107)
                in 100..107 -> {
                    val colorIndex = (code - 100) + 8
                    currentStyle = currentStyle.copy(backgroundColor = getPaletteColor(colorIndex))
                }
            }
            idx++
        }
    }

    private fun getPaletteColor(index: Int): Color {
        return if (index in theme.ansiPalette.indices) {
            theme.ansiPalette[index]
        } else {
            theme.foregroundColor
        }
    }

    private fun parse256Color(index: Int): Color {
        return when {
            index < 16 -> getPaletteColor(index)
            index in 16..231 -> {
                // 6x6x6 color cube
                val i = index - 16
                val r = (i / 36) * 51
                val g = ((i % 36) / 6) * 51
                val b = (i % 6) * 51
                Color(r, g, b)
            }
            index in 232..255 -> {
                // Grayscale ramp
                val gray = (index - 232) * 10 + 8
                Color(gray, gray, gray)
            }
            else -> theme.foregroundColor
        }
    }

    private fun flushCurrentSegment() {
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
