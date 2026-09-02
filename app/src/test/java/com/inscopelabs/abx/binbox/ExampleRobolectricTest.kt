package com.inscopelabs.abx.binbox

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.abx.binbox.terminal.engine.AnsiParser
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BinBox", appName)
    }

    @Test
    fun `ansi parser correctly processes plain text`() {
        var bellTriggered = false
        val parser = AnsiParser(
            theme = TerminalThemes.MonokaiPro,
            onBell = { bellTriggered = true }
        )

        parser.feed("Hello BinBox Terminal\n")
        val lines = parser.getLines()
        assertEquals(1, lines.size)
        assertEquals("Hello BinBox Terminal", lines[0].rawText)
    }

    @Test
    fun `ansi parser handles colors and styles`() {
        val parser = AnsiParser(theme = TerminalThemes.MonokaiPro)
        // Red text (ESC[31m), Bold (ESC[1m), Reset (ESC[0m)
        parser.feed("\u001B[1;31m[ERROR]\u001B[0m Connection established\n")
        val lines = parser.getLines()
        assertEquals(1, lines.size)
        assertEquals("[ERROR] Connection established", lines[0].rawText)
        assertTrue(lines[0].segments.isNotEmpty())
        assertTrue(lines[0].segments[0].style.isBold)
    }

    @Test
    fun `ansi parser handles backspaces and carriage returns`() {
        val parser = AnsiParser(theme = TerminalThemes.MonokaiPro)
        parser.feed("Progress: 50%\rProgress: 100%\n")
        val lines = parser.getLines()
        assertEquals(1, lines.size)
        assertEquals("Progress: 100%", lines[0].rawText)
    }
}
