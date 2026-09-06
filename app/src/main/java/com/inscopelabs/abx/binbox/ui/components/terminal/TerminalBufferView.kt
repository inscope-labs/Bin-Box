package com.inscopelabs.abx.binbox.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.oci.wizard.OciFreeTierPromoCard
import com.inscopelabs.abx.binbox.terminal.engine.ShellSession
import com.inscopelabs.abx.binbox.terminal.model.CursorStyle
import com.inscopelabs.abx.binbox.terminal.model.TerminalLine
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemePreset
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun TerminalBufferView(
    activeSession: ShellSession?,
    sessionLines: List<TerminalLine>,
    listState: LazyListState,
    currentTheme: TerminalThemePreset,
    fontSizeSp: Int,
    cursorStyle: CursorStyle,
    cursorVisible: Boolean,
    searchQuery: String,
    inputFocusRequester: FocusRequester,
    onLaunchDemo: () -> Unit,
    onLaunchLocal: () -> Unit,
    onLaunchOci: () -> Unit,
    onRawInsert: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onArrowUp: () -> Unit,
    onArrowDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Square-cornered terminal background container per industry terminal specifications
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ImmersiveTerminalCardBg)
            .border(1.dp, ImmersiveBorderSubtle)
            .padding(12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                inputFocusRequester.requestFocus()
            }
    ) {
        if (activeSession == null) {
            // Empty / Welcome Session State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ImmersiveComponent)
                        .border(1.dp, ImmersiveBorderSubtle, RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = ImmersivePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "BinBox Terminal",
                    color = ImmersiveTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Connect to remote host shells via SSH, Telnet, or launch the local Android shell.",
                    color = ImmersiveTextSecondary,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onLaunchDemo,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImmersivePrimary,
                            contentColor = ImmersiveOnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Cloud, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Launch Demo Host", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onLaunchLocal,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersiveTextPrimary)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, null, tint = ImmersivePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Local Shell")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                OciFreeTierPromoCard(
                    onLaunchWizard = onLaunchOci,
                    modifier = Modifier.fillMaxWidth(0.95f)
                )
            }
        } else {
            // The last line is either mid-write (hasPendingLine) or the shell's
            // freshly-reprinted prompt — either way it's the one line that's still
            // "live" and gets the cursor + keystroke capture attached to it. Its
            // text is the real PTY echo, already including anything typed so far,
            // so there is no separate client-side prompt/input value to reconcile.
            val hasActiveLine = activeSession.hasPendingLine || sessionLines.isNotEmpty()
            val completedLines = if (hasActiveLine && sessionLines.isNotEmpty()) {
                sessionLines.dropLast(1)
            } else {
                sessionLines
            }
            val activeLineAnnotated: AnnotatedString = if (sessionLines.isNotEmpty()) {
                renderLineAnnotatedString(sessionLines.last(), currentTheme, searchQuery)
            } else {
                AnnotatedString("")
            }

            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("terminal_lines_list")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            inputFocusRequester.requestFocus()
                        },
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(completedLines) { line ->
                        val annotatedString = renderLineAnnotatedString(line, currentTheme, searchQuery)
                        Text(
                            text = annotatedString,
                            fontSize = fontSizeSp.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = (fontSizeSp + 5).sp
                        )
                    }

                    // Active/live line: real shell echo (prompt + anything typed so
                    // far) plus an inline cursor and the invisible keystroke capture
                    // field. Nothing here is client-composed text.
                    item {
                        TerminalPromptRow(
                            activeLine = activeLineAnnotated,
                            inputFocusRequester = inputFocusRequester,
                            fontSizeSp = fontSizeSp,
                            currentTheme = currentTheme,
                            cursorStyle = cursorStyle,
                            cursorVisible = cursorVisible,
                            onRawInsert = onRawInsert,
                            onBackspace = onBackspace,
                            onEnter = onEnter,
                            onArrowUp = onArrowUp,
                            onArrowDown = onArrowDown
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalPromptRow(
    activeLine: AnnotatedString,
    inputFocusRequester: FocusRequester,
    fontSizeSp: Int,
    currentTheme: TerminalThemePreset,
    cursorStyle: CursorStyle,
    cursorVisible: Boolean,
    onRawInsert: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onArrowUp: () -> Unit,
    onArrowDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Word-wrapping (not single-line/horizontal-scroll) so long lines behave
    // like every other terminal instead of shifting sideways off-screen.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                inputFocusRequester.requestFocus()
            },
        verticalAlignment = Alignment.Top
    ) {
        // The real shell echo (prompt + anything typed, already merged by the
        // PTY) rendered with full ANSI styling. This is the only source of
        // truth for what's on screen — there is no separate input value.
        Text(
            text = activeLine,
            fontSize = fontSizeSp.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = (fontSizeSp + 5).sp,
            color = currentTheme.foregroundColor,
            softWrap = true,
            modifier = Modifier.weight(1f)
        )

        // Cursor is drawn at the true end of the real echoed line — no longer
        // a position independently guessed by a separate input field. (Precise
        // mid-line placement after cursor-left navigation needs the full 2D
        // screen-grid model tracked separately; this anchors correctly for the
        // overwhelmingly common case of typing forward.)
        if (cursorVisible) {
            when (cursorStyle) {
                CursorStyle.BLOCK, CursorStyle.BLINKING_BLOCK -> {
                    Box(
                        modifier = Modifier
                            .size(
                                width = (fontSizeSp * 0.58).dp,
                                height = (fontSizeSp * 1.15).dp
                            )
                            .background(ImmersivePrimary)
                            .testTag("terminal_cursor_block")
                    )
                }
                CursorStyle.UNDERLINE -> {
                    Box(
                        modifier = Modifier
                            .size(
                                width = (fontSizeSp * 0.58).dp,
                                height = 2.5.dp
                            )
                            .background(ImmersivePrimary)
                            .testTag("terminal_cursor_underline")
                    )
                }
                CursorStyle.BAR -> {
                    Box(
                        modifier = Modifier
                            .size(width = 2.dp, height = (fontSizeSp * 1.15).dp)
                            .background(ImmersivePrimary)
                            .testTag("terminal_cursor_bar")
                    )
                }
            }
        }

        // Invisible keystroke capture surface — see RawInputCaptureField for
        // why this never displays or accumulates text itself.
        RawInputCaptureField(
            focusRequester = inputFocusRequester,
            onInsert = onRawInsert,
            onBackspace = onBackspace,
            onEnter = onEnter,
            onArrowUp = onArrowUp,
            onArrowDown = onArrowDown
        )
    }
}

fun renderLineAnnotatedString(
    line: TerminalLine,
    theme: TerminalThemePreset,
    searchQuery: String
): AnnotatedString {
    return buildAnnotatedString {
        line.segments.forEach { segment ->
            val spanStyle = segment.style.toSpanStyle(theme)
            pushStyle(spanStyle)
            append(segment.text)
            pop()
        }

        // Highlight search results if present
        if (searchQuery.isNotBlank()) {
            val fullText = line.rawText
            var searchIdx = fullText.indexOf(searchQuery, ignoreCase = true)
            while (searchIdx >= 0) {
                addStyle(
                    style = SpanStyle(
                        background = ImmersiveStatusAmber.copy(alpha = 0.5f),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    ),
                    start = searchIdx,
                    end = searchIdx + searchQuery.length
                )
                searchIdx = fullText.indexOf(searchQuery, searchIdx + searchQuery.length, ignoreCase = true)
            }
        }
    }
}
