package com.inscopelabs.abx.binbox.terminal.engine

import androidx.compose.ui.graphics.Color
import com.inscopelabs.abx.binbox.terminal.model.TerminalStyle
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemePreset

object AnsiSgrParser {

    fun parse(paramsStr: String, initialStyle: TerminalStyle, theme: TerminalThemePreset): TerminalStyle {
        if (paramsStr.isEmpty()) {
            return TerminalStyle()
        }

        val tokens = paramsStr.split(';').mapNotNull { it.toIntOrNull() }
        if (tokens.isEmpty()) {
            return TerminalStyle()
        }

        var style = initialStyle
        var idx = 0
        while (idx < tokens.size) {
            val code = tokens[idx]
            when (code) {
                0 -> style = TerminalStyle()
                1 -> style = style.copy(isBold = true)
                2 -> style = style.copy(isDim = true)
                3 -> style = style.copy(isItalic = true)
                4 -> style = style.copy(isUnderline = true)
                7 -> style = style.copy(isInverted = true)
                9 -> style = style.copy(isStrikethrough = true)
                21 -> style = style.copy(isUnderline = true) // Double underline
                22 -> style = style.copy(isBold = false, isDim = false)
                23 -> style = style.copy(isItalic = false)
                24 -> style = style.copy(isUnderline = false)
                27 -> style = style.copy(isInverted = false)
                29 -> style = style.copy(isStrikethrough = false)

                // Standard Foreground (30-37)
                in 30..37 -> {
                    val colorIndex = code - 30
                    style = style.copy(foregroundColor = getPaletteColor(colorIndex, theme))
                }
                // Extended Foreground (38;5;n or 38;2;r;g;b)
                38 -> {
                    if (idx + 2 < tokens.size && tokens[idx + 1] == 5) {
                        val color256 = tokens[idx + 2]
                        style = style.copy(foregroundColor = parse256Color(color256, theme))
                        idx += 2
                    } else if (idx + 4 < tokens.size && tokens[idx + 1] == 2) {
                        val r = tokens[idx + 2].coerceIn(0, 255)
                        val g = tokens[idx + 3].coerceIn(0, 255)
                        val b = tokens[idx + 4].coerceIn(0, 255)
                        style = style.copy(foregroundColor = Color(r, g, b))
                        idx += 4
                    }
                }
                39 -> style = style.copy(foregroundColor = null)

                // Standard Background (40-47)
                in 40..47 -> {
                    val colorIndex = code - 40
                    style = style.copy(backgroundColor = getPaletteColor(colorIndex, theme))
                }
                // Extended Background (48;5;n or 48;2;r;g;b)
                48 -> {
                    if (idx + 2 < tokens.size && tokens[idx + 1] == 5) {
                        val color256 = tokens[idx + 2]
                        style = style.copy(backgroundColor = parse256Color(color256, theme))
                        idx += 2
                    } else if (idx + 4 < tokens.size && tokens[idx + 1] == 2) {
                        val r = tokens[idx + 2].coerceIn(0, 255)
                        val g = tokens[idx + 3].coerceIn(0, 255)
                        val b = tokens[idx + 4].coerceIn(0, 255)
                        style = style.copy(backgroundColor = Color(r, g, b))
                        idx += 4
                    }
                }
                49 -> style = style.copy(backgroundColor = null)

                // Bright Foreground (90-97)
                in 90..97 -> {
                    val colorIndex = (code - 90) + 8
                    style = style.copy(foregroundColor = getPaletteColor(colorIndex, theme))
                }
                // Bright Background (100-107)
                in 100..107 -> {
                    val colorIndex = (code - 100) + 8
                    style = style.copy(backgroundColor = getPaletteColor(colorIndex, theme))
                }
            }
            idx++
        }
        return style
    }

    fun getPaletteColor(index: Int, theme: TerminalThemePreset): Color {
        return if (index in theme.ansiPalette.indices) {
            theme.ansiPalette[index]
        } else {
            theme.foregroundColor
        }
    }

    fun parse256Color(index: Int, theme: TerminalThemePreset): Color {
        return when {
            index < 16 -> getPaletteColor(index, theme)
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
}
