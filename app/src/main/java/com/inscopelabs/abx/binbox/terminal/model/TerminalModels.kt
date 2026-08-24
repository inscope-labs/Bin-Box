package com.inscopelabs.abx.binbox.terminal.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.inscopelabs.abx.binbox.ui.theme.*

data class TerminalStyle(
    val foregroundColor: Color? = null,
    val backgroundColor: Color? = null,
    val isBold: Boolean = false,
    val isDim: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isInverted: Boolean = false
) {
    fun toSpanStyle(theme: TerminalThemePreset): SpanStyle {
        var fg = foregroundColor ?: theme.foregroundColor
        var bg = backgroundColor

        if (isInverted) {
            val temp = fg
            fg = bg ?: theme.backgroundColor
            bg = temp
        }

        if (isDim) {
            fg = fg.copy(alpha = 0.6f)
        }

        return SpanStyle(
            color = fg,
            background = bg ?: Color.Transparent,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = when {
                isUnderline && isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                isUnderline -> TextDecoration.Underline
                isStrikethrough -> TextDecoration.LineThrough
                else -> TextDecoration.None
            }
        )
    }
}

data class StyledSegment(
    val text: String,
    val style: TerminalStyle
)

data class TerminalLine(
    val segments: List<StyledSegment> = emptyList()
) {
    val rawText: String by lazy {
        segments.joinToString("") { it.text }
    }
}

enum class CursorStyle(val label: String) {
    BLOCK("Block"),
    UNDERLINE("Underline"),
    BAR("Vertical Bar"),
    BLINKING_BLOCK("Blinking Block")
}

enum class ProtocolType(val label: String, val defaultPort: Int) {
    SSH("SSH (Secure Shell)", 22),
    LOCAL_SHELL("Local Shell (Device)", 0),
    DEMO_HOST("Sandbox Linux Host (Demo)", 22),
    TELNET("Telnet (Raw TCP)", 23),
    CUSTOM_SOCKET("Custom Socket / Raw", 8080)
}

enum class AuthType(val label: String) {
    PASSWORD("Password"),
    PRIVATE_KEY("SSH Keypair"),
    PASSWORDLESS("Passwordless / Interactive")
}

sealed interface SessionState {
    object Disconnected : SessionState
    object Connecting : SessionState
    object Connected : SessionState
    data class Error(val message: String) : SessionState
}

data class TerminalThemePreset(
    val id: String,
    val name: String,
    val backgroundColor: Color,
    val foregroundColor: Color,
    val cursorColor: Color,
    val selectionColor: Color,
    val ansiPalette: List<Color>
) {
    val black: Color get() = ansiPalette.getOrElse(0) { Color.Black }
    val red: Color get() = ansiPalette.getOrElse(1) { Color.Red }
    val green: Color get() = ansiPalette.getOrElse(2) { Color.Green }
    val yellow: Color get() = ansiPalette.getOrElse(3) { Color.Yellow }
    val blue: Color get() = ansiPalette.getOrElse(4) { Color.Blue }
    val magenta: Color get() = ansiPalette.getOrElse(5) { Color.Magenta }
    val cyan: Color get() = ansiPalette.getOrElse(6) { Color.Cyan }
    val white: Color get() = ansiPalette.getOrElse(7) { Color.White }
}

object TerminalThemes {
    val MonokaiPro = TerminalThemePreset(
        id = "monokai_pro",
        name = "Monokai Pro",
        backgroundColor = Color(0xFF272822),
        foregroundColor = Color(0xFFF8F8F2),
        cursorColor = Color(0xFFFFCC66),
        selectionColor = Color(0x6649483E),
        ansiPalette = listOf(
            Color(0xFF272822), Color(0xFFFF6188), Color(0xFFA9DC76), Color(0xFFFFD866),
            Color(0xFF78DCE8), Color(0xFFAB9DF2), Color(0xFF78DCE8), Color(0xFFFCFCFA),
            Color(0xFF727072), Color(0xFFFF6188), Color(0xFFA9DC76), Color(0xFFFFD866),
            Color(0xFF78DCE8), Color(0xFFAB9DF2), Color(0xFF78DCE8), Color(0xFFFCFCFA)
        )
    )

    val Dracula = TerminalThemePreset(
        id = "dracula",
        name = "Dracula",
        backgroundColor = Color(0xFF282A36),
        foregroundColor = Color(0xFFF8F8F2),
        cursorColor = Color(0xFFBD93F9),
        selectionColor = Color(0x6644475A),
        ansiPalette = listOf(
            Color(0xFF21222C), Color(0xFFFF5555), Color(0xFF50FA7B), Color(0xFFF1FA8C),
            Color(0xFFBD93F9), Color(0xFFFF79C6), Color(0xFF8BE9FD), Color(0xFFF8F8F2),
            Color(0xFF6272A4), Color(0xFFFF6E6E), Color(0xFF69FF94), Color(0xFFFFFFA5),
            Color(0xFFD6ACFF), Color(0xFFFF92DF), Color(0xFFA4FFFF), Color(0xFFFFFFFF)
        )
    )

    val Nord = TerminalThemePreset(
        id = "nord",
        name = "Nord Arctic",
        backgroundColor = Color(0xFF2E3440),
        foregroundColor = Color(0xFFD8DEE9),
        cursorColor = Color(0xFF88C0D0),
        selectionColor = Color(0x66434C5E),
        ansiPalette = listOf(
            Color(0xFF3B4252), Color(0xFFBF616A), Color(0xFFA3BE8C), Color(0xFFEBCB8B),
            Color(0xFF81A1C1), Color(0xFFB48EAD), Color(0xFF88C0D0), Color(0xFFE5E9F0),
            Color(0xFF4C566A), Color(0xFFD08770), Color(0xFFA3BE8C), Color(0xFFEBCB8B),
            Color(0xFF5E81AC), Color(0xFFB48EAD), Color(0xFF8FBCBB), Color(0xFFECEFF4)
        )
    )

    val Cyberpunk = TerminalThemePreset(
        id = "cyberpunk",
        name = "Cyberpunk Neon",
        backgroundColor = Color(0xFF0A0A12),
        foregroundColor = Color(0xFF00F0FF),
        cursorColor = Color(0xFFFF0055),
        selectionColor = Color(0x662A0845),
        ansiPalette = listOf(
            Color(0xFF101018), Color(0xFFFF0055), Color(0xFF00FF66), Color(0xFFFFE600),
            Color(0xFF00F0FF), Color(0xFFFF00A0), Color(0xFF00FFFF), Color(0xFFEEEEEE),
            Color(0xFF303040), Color(0xFFFF2277), Color(0xFF33FF88), Color(0xFFFFF033),
            Color(0xFF33F3FF), Color(0xFFFF33B3), Color(0xFF33FFFF), Color(0xFFFFFFFF)
        )
    )

    val MatrixGreen = TerminalThemePreset(
        id = "matrix",
        name = "Matrix Green",
        backgroundColor = Color(0xFF030A04),
        foregroundColor = Color(0xFF00FF66),
        cursorColor = Color(0xFF00FF66),
        selectionColor = Color(0x66003311),
        ansiPalette = listOf(
            Color(0xFF001100), Color(0xFF008800), Color(0xFF00FF66), Color(0xFF33CC33),
            Color(0xFF00AA44), Color(0xFF00DD55), Color(0xFF00EE77), Color(0xFF00FF88),
            Color(0xFF004400), Color(0xFF00AA22), Color(0xFF00FF77), Color(0xFF66FF66),
            Color(0xFF00CC55), Color(0xFF22FF88), Color(0xFF44FFAA), Color(0xFF88FFCC)
        )
    )
    val Matrix = MatrixGreen

    val AmberCrt = TerminalThemePreset(
        id = "amber_crt",
        name = "Amber CRT Vintage",
        backgroundColor = Color(0xFF120D02),
        foregroundColor = Color(0xFFFFB000),
        cursorColor = Color(0xFFFFCC00),
        selectionColor = Color(0x663D2600),
        ansiPalette = listOf(
            Color(0xFF1F1600), Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFFBBF24),
            Color(0xFFFCD34D), Color(0xFFB45309), Color(0xFFFDE68A), Color(0xFFFEF3C7),
            Color(0xFF451A03), Color(0xFFB45309), Color(0xFFD97706), Color(0xFFF59E0B),
            Color(0xFFFBBF24), Color(0xFFFCD34D), Color(0xFFFDE68A), Color(0xFFFFFBEB)
        )
    )

    val SolarizedDark = TerminalThemePreset(
        id = "solarized_dark",
        name = "Solarized Dark",
        backgroundColor = Color(0xFF002B36),
        foregroundColor = Color(0xFF839496),
        cursorColor = Color(0xFF268BD2),
        selectionColor = Color(0x66073642),
        ansiPalette = listOf(
            Color(0xFF073642), Color(0xFFDC322F), Color(0xFF859900), Color(0xFFB58900),
            Color(0xFF268BD2), Color(0xFFD33682), Color(0xFF2AA198), Color(0xFFEEE8D5),
            Color(0xFF586E75), Color(0xFFCB4B16), Color(0xFF586E75), Color(0xFF657B83),
            Color(0xFF839496), Color(0xFF6C71C4), Color(0xFF93A1A1), Color(0xFFFDF6E3)
        )
    )

    val OneDark = TerminalThemePreset(
        id = "one_dark",
        name = "One Dark Pro",
        backgroundColor = Color(0xFF21252B),
        foregroundColor = Color(0xFFABB2BF),
        cursorColor = Color(0xFF528BFF),
        selectionColor = Color(0x663E4451),
        ansiPalette = listOf(
            Color(0xFF282C34), Color(0xFFE06C75), Color(0xFF98C379), Color(0xFFE5C07B),
            Color(0xFF61AFEF), Color(0xFFC678DD), Color(0xFF56B6C2), Color(0xFFABB2BF),
            Color(0xFF5C6370), Color(0xFFBE5046), Color(0xFF98C379), Color(0xFFE5C07B),
            Color(0xFF61AFEF), Color(0xFFC678DD), Color(0xFF56B6C2), Color(0xFFFFFFFF)
        )
    )

    val TokyoNight = TerminalThemePreset(
        id = "tokyo_night",
        name = "Tokyo Night",
        backgroundColor = Color(0xFF1A1B26),
        foregroundColor = Color(0xFFA9B1D6),
        cursorColor = Color(0xFFC0CAF5),
        selectionColor = Color(0x66283457),
        ansiPalette = listOf(
            Color(0xFF15161E), Color(0xFFF7768E), Color(0xFF9ECE6A), Color(0xFFE0AF68),
            Color(0xFF7AA2F7), Color(0xFFBB9AF7), Color(0xFF7DCFFF), Color(0xFFA9B1D6),
            Color(0xFF414868), Color(0xFFF7768E), Color(0xFF9ECE6A), Color(0xFFE0AF68),
            Color(0xFF7AA2F7), Color(0xFFBB9AF7), Color(0xFF7DCFFF), Color(0xFFC0CAF5)
        )
    )

    val GruvboxDark = TerminalThemePreset(
        id = "gruvbox",
        name = "Gruvbox Dark",
        backgroundColor = Color(0xFF282828),
        foregroundColor = Color(0xFFEBDBB2),
        cursorColor = Color(0xFFFE8019),
        selectionColor = Color(0x66504945),
        ansiPalette = listOf(
            Color(0xFF1D2021), Color(0xFFCC241D), Color(0xFF98971A), Color(0xFFD79921),
            Color(0xFF458588), Color(0xFFB16286), Color(0xFF689D6A), Color(0xFFA89984),
            Color(0xFF928374), Color(0xFFFB4934), Color(0xFFB8BB26), Color(0xFFFABD2F),
            Color(0xFF83A598), Color(0xFFD3869B), Color(0xFF8EC07C), Color(0xFFEBDBB2)
        )
    )
    val Gruvbox = GruvboxDark

    val SolarizedLight = TerminalThemePreset(
        id = "solarized_light",
        name = "Solarized Light",
        backgroundColor = Color(0xFFFDF6E3),
        foregroundColor = Color(0xFF657B83),
        cursorColor = Color(0xFF268BD2),
        selectionColor = Color(0x33073642),
        ansiPalette = listOf(
            Color(0xFFEEE8D5), Color(0xFFDC322F), Color(0xFF859900), Color(0xFFB58900),
            Color(0xFF268BD2), Color(0xFFD33682), Color(0xFF2AA198), Color(0xFF073642),
            Color(0xFF93A1A1), Color(0xFFCB4B16), Color(0xFF586E75), Color(0xFF657B83),
            Color(0xFF839496), Color(0xFF6C71C4), Color(0xFF93A1A1), Color(0xFF002B36)
        )
    )

    val PureOled = TerminalThemePreset(
        id = "pure_oled",
        name = "Pure OLED (Black)",
        backgroundColor = Color(0xFF000000),
        foregroundColor = Color(0xFFEEEEEE),
        cursorColor = Color(0xFF00F0FF),
        selectionColor = Color(0x66333333),
        ansiPalette = listOf(
            Color(0xFF000000), Color(0xFFFF5555), Color(0xFF55FF55), Color(0xFFFFFF55),
            Color(0xFF5555FF), Color(0xFFFF55FF), Color(0xFF55FFFF), Color(0xFFFFFFFF),
            Color(0xFF555555), Color(0xFFFF7777), Color(0xFF77FF77), Color(0xFFFFFF77),
            Color(0xFF7777FF), Color(0xFFFF77FF), Color(0xFF77FFFF), Color(0xFFFFFFFF)
        )
    )

    val AllThemes = listOf(
        MonokaiPro, Dracula, Nord, Cyberpunk, MatrixGreen, AmberCrt, SolarizedDark, SolarizedLight, OneDark, TokyoNight, GruvboxDark, PureOled
    )

    fun getThemeById(id: String): TerminalThemePreset {
        return AllThemes.find { it.id == id } ?: MonokaiPro
    }
}

data class CursorPosition(
    val row: Int = 0,
    val col: Int = 0,
    val visible: Boolean = true,
    val style: CursorStyle = CursorStyle.BLOCK
)

data class TerminalDimension(
    val cols: Int = 80,
    val rows: Int = 24,
    val widthPx: Int = 0,
    val heightPx: Int = 0
)

data class SearchMatch(
    val lineIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val text: String
)

data class TerminalSearchResults(
    val query: String = "",
    val matches: List<SearchMatch> = emptyList(),
    val currentMatchIndex: Int = 0
) {
    val totalMatches: Int get() = matches.size
    val currentMatch: SearchMatch? get() = matches.getOrNull(currentMatchIndex)
}

enum class TerminalFontFamily(val label: String, val fontFamily: FontFamily) {
    MONOSPACE("Monospace (System Default)", FontFamily.Monospace),
    DEFAULT("Sans Serif", FontFamily.Default),
    SERIF("Serif Display", FontFamily.Serif)
}

data class TerminalFontConfig(
    val fontFamily: TerminalFontFamily = TerminalFontFamily.MONOSPACE,
    val fontSizeSp: Float = 12f,
    val lineHeightSp: Float = 16f,
    val letterSpacingSp: Float = 0f
)
