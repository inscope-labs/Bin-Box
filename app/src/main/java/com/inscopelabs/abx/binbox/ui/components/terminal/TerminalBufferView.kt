package com.inscopelabs.abx.binbox.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onLaunchDemo: () -> Unit,
    onLaunchLocal: () -> Unit,
    onLaunchOci: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ImmersiveTerminalCardBg)
            .border(1.dp, ImmersiveBorderSubtle, RoundedCornerShape(24.dp))
            .padding(14.dp)
    ) {
        if (activeSession == null || sessionLines.isEmpty()) {
            // Empty Session State
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
                    text = "Bin Box Terminal",
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
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("terminal_lines_list"),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(sessionLines) { line ->
                        val annotatedString = renderLineAnnotatedString(line, currentTheme, searchQuery)
                        Text(
                            text = annotatedString,
                            fontSize = fontSizeSp.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = (fontSizeSp + 5).sp
                        )
                    }

                    // Active input / cursor line
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            if (cursorVisible) {
                                when (cursorStyle) {
                                    CursorStyle.BLOCK, CursorStyle.BLINKING_BLOCK -> {
                                        Box(
                                            modifier = Modifier
                                                .size(width = (fontSizeSp * 0.6).dp, height = fontSizeSp.dp)
                                                .background(ImmersivePrimary)
                                        )
                                    }
                                    CursorStyle.UNDERLINE -> {
                                        Box(
                                            modifier = Modifier
                                                .size(width = (fontSizeSp * 0.6).dp, height = 2.5.dp)
                                                .background(ImmersivePrimary)
                                        )
                                    }
                                    CursorStyle.BAR -> {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 2.5.dp, height = fontSizeSp.dp)
                                                .background(ImmersivePrimary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
