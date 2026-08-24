package com.inscopelabs.abx.binbox.terminal.engine

object TerminalKeyTranslator {

    fun translateSpecialKey(key: TerminalKey): ByteArray {
        val sequence = when (key) {
            TerminalKey.CTRL_C -> "\u0003"
            TerminalKey.CTRL_D -> "\u0004"
            TerminalKey.CTRL_Z -> "\u001A"
            TerminalKey.CTRL_L -> "\u000C"
            TerminalKey.CTRL_A -> "\u0001"
            TerminalKey.CTRL_E -> "\u0005"
            TerminalKey.ARROW_UP -> "\u001B[A"
            TerminalKey.ARROW_DOWN -> "\u001B[B"
            TerminalKey.ARROW_LEFT -> "\u001B[D"
            TerminalKey.ARROW_RIGHT -> "\u001B[C"
            TerminalKey.TAB -> "\t"
            TerminalKey.ESC -> "\u001B"
            TerminalKey.PAGE_UP -> "\u001B[5~"
            TerminalKey.PAGE_DOWN -> "\u001B[6~"
            TerminalKey.HOME -> "\u001B[H"
            TerminalKey.END -> "\u001B[F"
        }
        return sequence.toByteArray(Charsets.UTF_8)
    }

    fun translateFunctionKey(fIndex: Int): ByteArray {
        val sequence = when (fIndex) {
            1 -> "\u001BOP"
            2 -> "\u001BOQ"
            3 -> "\u001BOR"
            4 -> "\u001BOS"
            5 -> "\u001B[15~"
            6 -> "\u001B[17~"
            7 -> "\u001B[18~"
            8 -> "\u001B[19~"
            9 -> "\u001B[20~"
            10 -> "\u001B[21~"
            11 -> "\u001B[23~"
            12 -> "\u001B[24~"
            else -> ""
        }
        return sequence.toByteArray(Charsets.UTF_8)
    }

    fun translateInputWithModifiers(
        input: String,
        ctrlActive: Boolean,
        altActive: Boolean,
        bracketedPaste: Boolean = false
    ): ByteArray {
        if (bracketedPaste && input.length > 1) {
            return "\u001B[200~$input\u001B[201~".toByteArray(Charsets.UTF_8)
        }

        if (input.isEmpty()) return ByteArray(0)

        val result = StringBuilder()
        for (ch in input) {
            when {
                ctrlActive -> {
                    val code = when (ch.uppercaseChar()) {
                        in 'A'..'Z' -> (ch.uppercaseChar() - 'A' + 1).toChar().toString()
                        '[' -> "\u001B"
                        '\\' -> "\u001C"
                        ']' -> "\u001D"
                        '^' -> "\u001E"
                        '_' -> "\u001F"
                        ' ' -> "\u0000"
                        else -> ch.toString()
                    }
                    if (altActive) {
                        result.append("\u001B").append(code)
                    } else {
                        result.append(code)
                    }
                }
                altActive -> {
                    result.append("\u001B").append(ch)
                }
                else -> {
                    result.append(ch)
                }
            }
        }
        return result.toString().toByteArray(Charsets.UTF_8)
    }

    fun translateEnterKey(crlf: Boolean = false): ByteArray {
        return (if (crlf) "\r\n" else "\r").toByteArray(Charsets.UTF_8)
    }

    fun translateBackspaceKey(): ByteArray {
        return "\u007F".toByteArray(Charsets.UTF_8)
    }
}
