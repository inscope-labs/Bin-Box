package com.inscopelabs.abx.binbox.terminal.engine

import androidx.compose.ui.graphics.Color
import com.inscopelabs.abx.binbox.terminal.model.StyledSegment
import com.inscopelabs.abx.binbox.terminal.model.TerminalLine
import com.inscopelabs.abx.binbox.terminal.model.TerminalStyle
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemePreset

class AnsiParser(
    private var theme: TerminalThemePreset,
    private val onBell: (() -> Unit)? = null
) {
    private var currentStyle = TerminalStyle()
    private val buffer = mutableListOf<TerminalLine>()
    private var currentLineSegments = mutableListOf<StyledSegment>()
    private var currentSegmentBuilder = StringBuilder()

    var maxScrollback: Int = 3000

    fun updateTheme(newTheme: TerminalThemePreset) {
        this.theme = newTheme
    }

    @Synchronized
    fun getLines(): List<TerminalLine> {
        val result = ArrayList<TerminalLine>(buffer.size + 1)
        result.addAll(buffer)
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
        buffer.clear()
        currentLineSegments.clear()
        currentSegmentBuilder.clear()
        currentStyle = TerminalStyle()
    }

    @Synchronized
    fun feed(input: String) {
        var i = 0
        val len = input.length

        while (i < len) {
            val ch = input[i]

            when {
                // Bell character
                ch == '\u0007' -> {
                    onBell?.invoke()
                    i++
                }

                // Carriage Return
                ch == '\r' -> {
                    // Check if followed by newline
                    if (i + 1 < len && input[i + 1] == '\n') {
                        flushCurrentSegment()
                        commitCurrentLine()
                        i += 2
                    } else {
                        // CR without LF: overwrite from start of line
                        flushCurrentSegment()
                        currentLineSegments.clear()
                        currentSegmentBuilder.clear()
                        i++
                    }
                }

                // Newline
                ch == '\n' -> {
                    flushCurrentSegment()
                    commitCurrentLine()
                    i++
                }

                // Backspace
                ch == '\b' -> {
                    if (currentSegmentBuilder.isNotEmpty()) {
                        currentSegmentBuilder.deleteCharAt(currentSegmentBuilder.length - 1)
                    } else if (currentLineSegments.isNotEmpty()) {
                        val last = currentLineSegments.removeAt(currentLineSegments.lastIndex)
                        if (last.text.length > 1) {
                            currentLineSegments.add(
                                last.copy(text = last.text.substring(0, last.text.length - 1))
                            )
                        }
                    }
                    i++
                }

                // Tab
                ch == '\t' -> {
                    currentSegmentBuilder.append("    ")
                    i++
                }

                // ANSI Escape sequence start \u001b
                ch == '\u001B' -> {
                    if (i + 1 < len) {
                        val next = input[i + 1]
                        if (next == '[') {
                            // CSI sequence: parse till terminating letter
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
                                // Incomplete sequence at end of chunk, skip
                                i = len
                            }
                        } else if (next == ']') {
                            // OSC sequence: \u001b] ... \u0007 or \u001b\
                            var endIdx = i + 2
                            while (endIdx < len && input[endIdx] != '\u0007' && input[endIdx] != '\u001B') {
                                endIdx++
                            }
                            if (endIdx < len && input[endIdx] == '\u001B' && endIdx + 1 < len && input[endIdx + 1] == '\\') {
                                i = endIdx + 2
                            } else if (endIdx < len && input[endIdx] == '\u0007') {
                                i = endIdx + 1
                            } else {
                                i = len
                            }
                        } else {
                            // Single-character escape
                            i += 2
                        }
                    } else {
                        i++
                    }
                }

                else -> {
                    currentSegmentBuilder.append(ch)
                    i++
                }
            }
        }

        // Limit scrollback buffer
        if (buffer.size > maxScrollback) {
            val toRemove = buffer.size - maxScrollback
            repeat(toRemove) {
                if (buffer.isNotEmpty()) buffer.removeAt(0)
            }
        }
    }

    private fun isCsiTerminator(c: Char): Boolean {
        return c in '@'..'~'
    }

    private fun handleCsi(paramsStr: String, command: Char) {
        when (command) {
            'm' -> {
                // Select Graphic Rendition (SGR)
                flushCurrentSegment()
                parseSgr(paramsStr)
            }
            'J' -> {
                // Erase in Display: 2J or 3J clears screen/buffer
                if (paramsStr == "2" || paramsStr == "3" || paramsStr == "") {
                    buffer.clear()
                    currentLineSegments.clear()
                    currentSegmentBuilder.clear()
                }
            }
            'K' -> {
                // Erase in Line: 2K clears line
                if (paramsStr == "2" || paramsStr == "") {
                    currentSegmentBuilder.clear()
                    currentLineSegments.clear()
                }
            }
            'H', 'f' -> {
                // Cursor Home / Position: for now flush and allow text rendering
                flushCurrentSegment()
            }
            else -> {
                // Other cursor/mode controls (ignore or flush)
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
        buffer.add(TerminalLine(ArrayList(currentLineSegments)))
        currentLineSegments.clear()
    }
}
