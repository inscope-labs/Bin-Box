package com.inscopelabs.abx.binbox.terminal.engine

import com.inscopelabs.abx.binbox.terminal.model.StyledSegment
import com.inscopelabs.abx.binbox.terminal.model.TerminalLine
import com.inscopelabs.abx.binbox.terminal.model.TerminalStyle

/**
 * A fixed-size, cursor-addressable character grid — the real screen model
 * full-screen terminal programs (vim, htop, less, tmux) require: every
 * character write happens at an explicit (row, col), and cursor-position
 * escape sequences move to and overwrite existing content in place, rather
 * than always appending a new line.
 *
 * This is deliberately separate from the primary buffer's scrollback-list
 * model in [AnsiParser] — canonical shell interaction (a growing history of
 * completed lines plus a live prompt line) is a genuinely different, and
 * already-correct, rendering shape for a mobile terminal. A fixed grid is
 * only needed for the alternate screen buffer that full-screen programs
 * switch into (DECSET ?47/?1049), which is what this class backs.
 */
class TerminalGrid(rows: Int, cols: Int) {
    var rows: Int = rows.coerceAtLeast(1)
        private set
    var cols: Int = cols.coerceAtLeast(1)
        private set

    private var cells: Array<Array<GridCell>> = Array(this.rows) { Array(this.cols) { GridCell.BLANK } }

    var cursorRow: Int = 0
        private set
    var cursorCol: Int = 0
        private set

    // Scroll region, 0-indexed inclusive bounds. Defaults to the full screen;
    // full-screen programs that split the screen (e.g. a status line) narrow
    // this via CSI 'r'.
    private var scrollTop: Int = 0
    private var scrollBottom: Int = this.rows - 1

    data class GridCell(val char: Char, val style: TerminalStyle) {
        companion object {
            val BLANK = GridCell(' ', TerminalStyle())
        }
    }

    /** Resizes the grid, preserving whatever content overlaps the old and new bounds. */
    fun resize(newRows: Int, newCols: Int) {
        val r = newRows.coerceAtLeast(1)
        val c = newCols.coerceAtLeast(1)
        if (r == rows && c == cols) return
        val oldCells = cells
        cells = Array(r) { row ->
            Array(c) { col ->
                oldCells.getOrNull(row)?.getOrNull(col) ?: GridCell.BLANK
            }
        }
        rows = r
        cols = c
        scrollTop = 0
        scrollBottom = rows - 1
        cursorRow = cursorRow.coerceIn(0, rows - 1)
        cursorCol = cursorCol.coerceIn(0, cols - 1)
    }

    fun setCursor(row: Int, col: Int) {
        cursorRow = row.coerceIn(0, rows - 1)
        cursorCol = col.coerceIn(0, cols - 1)
    }

    fun moveCursorBy(deltaRow: Int, deltaCol: Int) {
        setCursor(cursorRow + deltaRow, cursorCol + deltaCol)
    }

    fun setScrollRegion(top: Int, bottom: Int) {
        scrollTop = top.coerceIn(0, rows - 1)
        scrollBottom = bottom.coerceIn(scrollTop, rows - 1)
    }

    fun resetScrollRegion() {
        scrollTop = 0
        scrollBottom = rows - 1
    }

    /** Writes one character at the cursor, wrapping/scrolling as needed (xterm autowrap). */
    fun writeChar(char: Char, style: TerminalStyle) {
        if (cursorCol >= cols) {
            lineFeed()
            cursorCol = 0
        }
        cells[cursorRow][cursorCol] = GridCell(char, style)
        cursorCol++
    }

    fun carriageReturn() {
        cursorCol = 0
    }

    /** Moves down one row, scrolling the scroll region up if already at its bottom. */
    fun lineFeed() {
        if (cursorRow == scrollBottom) {
            scrollUp(1)
        } else if (cursorRow < rows - 1) {
            cursorRow++
        }
    }

    fun backspace() {
        if (cursorCol > 0) cursorCol--
    }

    fun scrollUp(n: Int) {
        repeat(n) {
            for (row in scrollTop until scrollBottom) {
                cells[row] = cells[row + 1]
            }
            cells[scrollBottom] = Array(cols) { GridCell.BLANK }
        }
    }

    fun scrollDown(n: Int) {
        repeat(n) {
            for (row in scrollBottom downTo scrollTop + 1) {
                cells[row] = cells[row - 1]
            }
            cells[scrollTop] = Array(cols) { GridCell.BLANK }
        }
    }

    fun eraseInLine(mode: Int) {
        when (mode) {
            0 -> for (c in cursorCol until cols) cells[cursorRow][c] = GridCell.BLANK
            1 -> for (c in 0..cursorCol.coerceAtMost(cols - 1)) cells[cursorRow][c] = GridCell.BLANK
            2 -> cells[cursorRow] = Array(cols) { GridCell.BLANK }
        }
    }

    fun eraseInDisplay(mode: Int) {
        when (mode) {
            0 -> {
                eraseInLine(0)
                for (r in (cursorRow + 1) until rows) cells[r] = Array(cols) { GridCell.BLANK }
            }
            1 -> {
                eraseInLine(1)
                for (r in 0 until cursorRow) cells[r] = Array(cols) { GridCell.BLANK }
            }
            2, 3 -> clear()
        }
    }

    fun clear() {
        cells = Array(rows) { Array(cols) { GridCell.BLANK } }
        cursorRow = 0
        cursorCol = 0
    }

    /**
     * Renders the fixed grid as one [TerminalLine] per row, for display —
     * always exactly [rows] lines, blank cells included, matching how a real
     * full-screen terminal always paints its entire viewport.
     */
    fun toTerminalLines(): List<TerminalLine> {
        return cells.map { row ->
            val segments = ArrayList<StyledSegment>()
            var runStart = 0
            for (i in 1..row.size) {
                if (i == row.size || row[i].style != row[i - 1].style) {
                    val text = buildString { for (j in runStart until i) append(row[j].char) }
                    segments.add(StyledSegment(text, row[runStart].style))
                    runStart = i
                }
            }
            TerminalLine(segments)
        }
    }
}
