package com.inscopelabs.abx.binbox.ui.components.terminal

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

/**
 * A single zero-width placeholder character. The field's text always contains
 * exactly this sentinel (never the user's actual typed content) so that a
 * backspace on an "empty" field still produces an onValueChange callback we
 * can detect — Compose gives no callback for backspace against a truly empty
 * TextFieldValue.
 */
private const val SENTINEL = "\u200B"

/**
 * Raw terminal keystroke capture surface (Termux/xterm model).
 *
 * This field never visually displays what's typed and never accumulates a
 * client-side command string. Every insertion, deletion, or paste is diffed
 * against the sentinel-only baseline and forwarded immediately as raw bytes
 * to the PTY via [onInsert] / [onBackspace]. The real terminal echo (parsed
 * from the shell's own PTY output) is the single source of truth for what's
 * displayed — this field is purely an IME/hardware-keyboard input target.
 *
 * Arrow keys and Enter are intercepted at the key-event level and forwarded
 * as their real terminal byte sequences via [onArrowUp]/[onArrowDown]/
 * [onEnter] rather than being treated as text edits, so shell-side readline
 * (history, line editing) and full-screen programs keep working normally.
 */
@Composable
fun RawInputCaptureField(
    focusRequester: FocusRequester,
    onInsert: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onArrowUp: () -> Unit,
    onArrowDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(SENTINEL, selection = TextRange(SENTINEL.length)))
    }

    BasicTextField(
        value = fieldValue,
        onValueChange = { new ->
            // Invariant: fieldValue.text is always exactly SENTINEL right before
            // this callback runs (we reset it below on every prior call), so the
            // diff reduces to "what happened to the sentinel", not a general
            // old-vs-new text diff.
            when {
                new.text == SENTINEL -> {
                    // Selection/cursor-only change (or a no-op IME callback); nothing to forward.
                }
                new.text.isEmpty() -> {
                    // The sentinel was deleted with nothing replacing it -> backspace.
                    onBackspace()
                }
                new.text.startsWith(SENTINEL) -> {
                    // Normal case: sentinel intact, new content appended after it.
                    onInsert(new.text.removePrefix(SENTINEL))
                }
                else -> {
                    // IME replaced the sentinel itself (autocorrect/voice input).
                    // Best-effort: forward whatever text resulted, sentinel included
                    // characters stripped out since they carry no real content.
                    onInsert(new.text.replace(SENTINEL, ""))
                }
            }
            // Always snap back to the sentinel-only baseline so the field
            // never grows and never has anything visible to render.
            fieldValue = TextFieldValue(SENTINEL, selection = TextRange(SENTINEL.length))
        },
        modifier = modifier
            .size(1.dp)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.key) {
                    Key.Enter, Key.NumPadEnter -> {
                        onEnter()
                        true
                    }
                    Key.DirectionUp -> {
                        onArrowUp()
                        true
                    }
                    Key.DirectionDown -> {
                        onArrowDown()
                        true
                    }
                    else -> false
                }
            },
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.None,
            autoCorrect = false
        )
        // Deliberately no KeyboardActions / ImeAction.Send binding: several IMEs
        // auto-dismiss the soft keyboard after firing a "send/done" action
        // regardless of app focus state. Enter is handled purely via the raw
        // KeyEvent path above, so the keyboard stays open until the user
        // explicitly dismisses it — matching Termux/industry-standard terminals.
    )
}
