package com.inscopelabs.abx.binbox.terminal.engine

import com.inscopelabs.abx.binbox.terminal.model.StyledSegment
import com.inscopelabs.abx.binbox.terminal.model.TerminalLine
import com.inscopelabs.abx.binbox.terminal.model.TerminalStyle
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemePreset

interface AnsiCsiTarget {
    var cursorRow: Int
    var cursorCol: Int
    var isCursorVisible: Boolean
    var isBracketedPasteMode: Boolean
    var isAlternateBufferActive: Boolean
    var currentStyle: TerminalStyle
    val currentBuffer: MutableList<TerminalLine>
    val currentLineSegments: MutableList<StyledSegment>
    val currentSegmentBuilder: java.lang.StringBuilder
    val alternateBuffer: MutableList<TerminalLine>
    var savedCursorRow: Int
    var savedCursorCol: Int
    var savedStyle: TerminalStyle
    val theme: TerminalThemePreset

    fun flushCurrentSegment()
}

class AnsiCsiHandler {
    companion object {
        fun isCsiTerminator(c: Char): Boolean {
            return c in '@'..'~'
        }

        fun handleCsi(target: AnsiCsiTarget, paramsStr: String, command: Char) {
            when (command) {
                'm' -> {
                    target.flushCurrentSegment()
                    target.currentStyle = AnsiSgrParser.parse(paramsStr, target.currentStyle, target.theme)
                }
                'J' -> {
                    target.flushCurrentSegment()
                    when (paramsStr) {
                        "2", "3" -> {
                            target.currentBuffer.clear()
                            target.currentLineSegments.clear()
                            target.currentSegmentBuilder.clear()
                            target.cursorRow = 0
                            target.cursorCol = 0
                        }
                        "1" -> {
                            target.currentSegmentBuilder.clear()
                            target.currentLineSegments.clear()
                        }
                        "0", "" -> {
                            target.currentSegmentBuilder.clear()
                        }
                    }
                }
                'K' -> {
                    when (paramsStr) {
                        "2" -> {
                            target.currentSegmentBuilder.clear()
                            target.currentLineSegments.clear()
                            target.cursorCol = 0
                        }
                        "1" -> {
                            target.currentSegmentBuilder.clear()
                        }
                        "0", "" -> {
                            target.currentSegmentBuilder.clear()
                        }
                    }
                }
                'H', 'f' -> {
                    target.flushCurrentSegment()
                    val parts = paramsStr.split(';').mapNotNull { it.toIntOrNull() }
                    target.cursorRow = if (parts.isNotEmpty()) (parts[0] - 1).coerceAtLeast(0) else 0
                    target.cursorCol = if (parts.size > 1) (parts[1] - 1).coerceAtLeast(0) else 0
                }
                'A' -> {
                    val count = paramsStr.toIntOrNull() ?: 1
                    target.cursorRow = (target.cursorRow - count).coerceAtLeast(0)
                }
                'B' -> {
                    val count = paramsStr.toIntOrNull() ?: 1
                    target.cursorRow += count
                }
                'C' -> {
                    val count = paramsStr.toIntOrNull() ?: 1
                    target.cursorCol += count
                }
                'D' -> {
                    val count = paramsStr.toIntOrNull() ?: 1
                    target.cursorCol = (target.cursorCol - count).coerceAtLeast(0)
                }
                'h', 'l' -> {
                    val isEnable = (command == 'h')
                    if (paramsStr.startsWith("?")) {
                        val mode = paramsStr.removePrefix("?").toIntOrNull()
                        when (mode) {
                            25 -> target.isCursorVisible = isEnable
                            47, 1049 -> {
                                if (isEnable && !target.isAlternateBufferActive) {
                                    target.isAlternateBufferActive = true
                                    target.alternateBuffer.clear()
                                } else if (!isEnable && target.isAlternateBufferActive) {
                                    target.isAlternateBufferActive = false
                                    target.alternateBuffer.clear()
                                }
                            }
                            2004 -> target.isBracketedPasteMode = isEnable
                        }
                    }
                }
                's' -> {
                    target.savedCursorRow = target.cursorRow
                    target.savedCursorCol = target.cursorCol
                    target.savedStyle = target.currentStyle
                }
                'u' -> {
                    target.cursorRow = target.savedCursorRow
                    target.cursorCol = target.savedCursorCol
                    target.currentStyle = target.savedStyle
                }
                else -> {
                    target.flushCurrentSegment()
                }
            }
        }
    }
}
