# Agent Task Report: Terminal Grid-Buffer Rewrite (Full-Screen Program Support)

- **Timestamp (UTC):** 2026-09-06T183433Z
- **Task:** Replace the append-only alternate-screen-buffer model with a real cursor-addressable 2D grid, so full-screen programs (vim, htop, less, tmux) render and behave correctly. Tracked since the raw-keystroke-forwarding report (2026-09-06T115422Z §2.5) as urgent follow-up.
- **Agent:** Claude (direct-commit mode per INC-002 — AI Studio unavailable). CI for the prior commit (`a127ed5`) passed before this task began.

---

## 1. Root Cause (confirmed via full read of `AnsiParser.feed()` and `AnsiCsiHandler`)

`cursorRow`/`cursorCol` were tracked as plain numbers with **zero connection to buffer content addressing**. Every character write unconditionally appended to `currentSegmentBuilder` (the single "currently building line"); CSI cursor-position commands (`H`/`f`/`A`/`B`/`C`/`D`) updated the tracked numbers but nothing ever read them back to decide *where* to write. `J`/`K` (erase) only ever touched `currentSegmentBuilder`/`currentLineSegments`, never an arbitrary buffer row. Concretely: `\x1b[5;10H` followed by `X` did not overwrite row 5 col 10 — it appended `X` to whatever line happened to be currently open, wherever that was. Full-screen programs were confirmed broken by design, not by an edge-case bug.

Also confirmed while investigating window sizing (needed for a grid to have real dimensions):
- A full multi-transport `resize(cols, rows)` pipeline already exists end-to-end (`BinBoxViewModel.resizeTerminal` → `TerminalSessionManager` → `ShellSession.resize` → `ITransport.resize`, implemented for SSH/Telnet/TCP/WebSocket transports) — **but nothing in the UI ever calls it.** No Compose layout measurement triggers a resize anywhere.
- `LocalProcessTransport.resize()` is a stub that only logs — it does not set the local PTY's real window size (`TIOCSWINSZ`). No native (`cpp`/JNI) source exists in this repo for `PtyNative` to call such an ioctl from; this would require new native code, not a Kotlin-only change.

Both of these are real, separate gaps from the grid-content bug and are **not addressed in this pass** — see §4.

---

## 2. Design

Scoped deliberately narrow: **only the alternate/full-screen buffer becomes a real grid.** The primary buffer's append-only scrollback-list model is untouched — canonical shell interaction (growing history + a live prompt line) is a different, and already-correct (per the 2026-09-06T115422Z fix, CI-verified), rendering shape for a mobile terminal. Rewriting that too was not needed and would have risked regressing a just-verified fix.

- **New `TerminalGrid.kt`**: fixed `rows × cols` cell array, own cursor (`setCursor`/`moveCursorBy`), scroll-region-aware `scrollUp`/`scrollDown`/`lineFeed`, `eraseInLine`/`eraseInDisplay`, `resize` (preserves overlapping content), `toTerminalLines()` (run-length-coalesces same-style runs into `StyledSegment`s, matching the primary buffer's existing structure so the renderer needs no new data shape).
- **`AnsiParser`**: `alternateBuffer: MutableList<TerminalLine>` replaced with `alternateGrid: TerminalGrid`. `cursorRow`/`cursorCol` became proxying properties — when the alternate buffer is active, get/set transparently target the grid's cursor instead of the primary buffer's tracked position. This means `AnsiCsiHandler`'s existing cursor-movement code (`H`/`f`/`A`/`B`/`C`/`D`) needed **zero changes** to correctly address whichever buffer is live. `feed()`'s CR/LF/backspace/tab/character-write branches now check `isAlternateBufferActive` and route to the grid; the primary-mode branches are byte-for-byte identical to before.
- **`AnsiCsiHandler`**: `J`/`K` branch on alternate-mode to call the grid's erase methods instead of touching `currentSegmentBuilder`. Alternate-buffer toggle (`?47`/`?1049`) now calls new `enterAlternateBuffer()`/`exitAlternateBuffer()` hooks on the parser instead of directly clearing a list. Added `r` (DECSTBM scroll region — needed for split-screen full-screen apps) and `S`/`T` (explicit scroll up/down) handling, alternate-mode only.
- **`ShellSession`**: added `isAlternateScreenActive` (same pattern as the existing `hasPendingLine`), so the UI can tell full-screen mode apart from canonical shell mode. `resize()` now also calls `ansiParser.updateSize(rows, cols)` and re-emits `_lines`.
- **`TerminalBufferView.kt`**: added a third rendering branch (alongside "no session" and "canonical shell") for `activeSession.isAlternateScreenActive`. Every grid row now renders uniformly with no special last-row treatment — the previous "last line is the interactive prompt" logic would be actively wrong here, since a full-screen program's cursor can be anywhere on screen. An invisible `RawInputCaptureField` is kept alive via an overlay so keystrokes still reach the shell while a full-screen program is running.
- **`TerminalScreen.kt`**: one-line guard so BinBox's own history browser doesn't record keystrokes typed while a full-screen program (e.g. vim) has focus — those aren't shell commands.

**Not touched:** primary-buffer append-only logic, `TerminalPromptRow`, `RawInputCaptureField`, `TerminalQuickKeysBar` — none needed changes for this task.

---

## 3. Compliance and Audit

### Prior Logging Gaps
- PRIOR LOGGING GAPS FOUND: none (`issues/pending/` clean).

### Single-Responsibility File Discipline & Line Thresholds
- `TerminalGrid.kt`: logic file, new, 176 lines (threshold: 500) — COMPLIANT.
- `AnsiParser.kt`: logic file, 372 lines (threshold: 500) — COMPLIANT.
- `AnsiCsiHandler.kt`: logic file, 164 lines (threshold: 500) — COMPLIANT.
- `ShellSession.kt`: logic file, 302 lines (threshold: 500) — COMPLIANT.
- `TerminalBufferView.kt`: UI file, 391 lines (threshold: 1000) — COMPLIANT.

### Version Increment
- **versionCode/debugCode incremented 38 → 39 per explicit developer direction** ("Don't forget to increment the version code when completed"). Note: `version.properties` had independently been bumped 37 → 38 directly on `main` (commit `3e9a993`, author `inscope-labs`) for the prior raw-keystroke-forwarding task while this one was in progress. Rebasing onto that picked up the 38 baseline, so this task's completion gets its own increment (39) on top rather than colliding with it. `version.properties` is a Protected Path per AGENTS.md §5; this edit was made under explicit developer authorization for this specific task, not unilaterally. `versionName` left at `0.1.0`, matching every prior bump in this repo's history (checked via `git log -p -- version.properties`) — only `versionCode`/`debugCode` have ever incremented pre-1.0.

---

## 4. Explicitly NOT Done This Pass (tracked, not silently skipped)

1. **No dynamic terminal-size measurement.** The alternate grid defaults to a fixed 80×24 and stays there — nothing in the Compose UI measures the real viewport and calls the already-existing `resizeTerminal` pipeline. Full-screen programs will lay themselves out for 80×24 regardless of actual device/font size.
2. **No native `TIOCSWINSZ` for local sessions.** Even if (1) were wired, `LocalProcessTransport.resize()` doesn't tell the local shell its real window size — this needs new native code (none exists in-repo for `PtyNative` to extend), which is a materially different kind of task (NDK/JNI, not Kotlin) and wasn't attempted blind.
3. **No visible cursor indicator positioned at the full-screen program's true on-screen location.** Content now renders at the correct grid position (the actual bug), but the cursor-block/underline/bar overlay used in canonical mode isn't drawn anywhere in full-screen mode yet — it would need per-row layout offset plumbing the current `LazyColumn` doesn't expose. The program is fully interactive (keystrokes reach it, content updates correctly) but has no visible blinking cursor of its own.
4. **CSI coverage is a working subset, not full VT100/xterm fidelity** — cursor addressing, character overwrite, erase, scroll regions, and basic scroll up/down are implemented; less common sequences (mouse reporting, DEC private modes beyond 25/47/1049/2004, character-set designation, etc.) fall through to the existing harmless no-op default.

---

## 5. Verification and Commands

- **No Android SDK/Gradle toolchain available in this agent's sandbox**, same limitation as the prior report — this has not been compiled or unit-tested by the agent that wrote it.
- Verification performed: full re-read of every changed file; confirmed the existing `TerminalEngineTest.kt` suite only exercises primary-buffer behavior (plain text, SGR/color parsing, clear-screen, bell, scrollback limit) — none of which was touched, so that suite should remain green; grep-swept for dangling references to the removed `alternateBuffer` symbol (none found); confirmed `SshShellSession`/`LocalShellSession`/`TelnetShellSession` all extend `TransportShellSession` and inherit the fix automatically; confirmed `SandboxDemoShellSession` is unaffected (uses the interface's safe default `isAlternateScreenActive = false`).
- **Action required before trusting full-screen apps on-device:** run CI, then a real device pass opening `vim`, `htop`, and `less` on a local session specifically (SSH/Telnet sessions inherit the same fix but weren't the original complaint). Expect correct-but-fixed-size (80×24) rendering with a working screen but no visible cursor indicator, per §4.
