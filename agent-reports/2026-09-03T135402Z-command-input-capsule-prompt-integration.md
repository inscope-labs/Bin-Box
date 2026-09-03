# Agent Task Report: Command Input Capsule Shell Prompt Integration

- **Timestamp (UTC):** 2026-09-03T135402Z
- **Task:** Integrate Command Input Capsule directly into the shell terminal prompt with a visible cursor, remove history navigation (leveraging existing directional accessory keys), convert terminal background from beveled/rounded to square corners, and adhere to terminal industry standards.

---

## 1. Request Summary
The user requested:
1. **Command Input Integration**: Integrate the Command Input Capsule directly into the shell terminal prompt — with a visible cursor. It should behave like any normal shell prompt after integration.
2. **Remove History Navigation**: Do not carry over history navigation chips/buttons to the prompt line, as directional UI buttons (`▲` and `▼`) in the quick keys bar already provide that functionality.
3. **Square Corners**: Convert the terminal background from its beveled/rounded container to square corners (`RectangleShape`).
4. **Industry Standards & Best Practices**: Follow common terminal conventions (Termux, JuiceSSH, X11 xterm, VT100), including tap-to-focus on the terminal buffer, IME Send action dispatch, full ANSI color/formatting preservation on prompt lines, custom cursor styles (block, underline, bar) with blinking support, and an ENTER key in the accessory bar.

---

## 2. Changes Implemented

### A. Terminal Buffer View (`TerminalBufferView.kt`)
- **Square Corners**: Replaced `RoundedCornerShape(24.dp)` container and border with clean square corners (`RectangleShape`), border `ImmersiveBorderSubtle`, and `12.dp` padding.
- **Integrated Terminal Prompt (`TerminalPromptRow`)**:
  - Implemented logic to detect active pending prompt lines from the shell session (`activeSession.hasPendingLine` or prompt suffix heuristics `$` / `#` / `>`).
  - Separates completed output lines from the active prompt line.
  - Displays the prompt text using `renderLineAnnotatedString()`, preserving full ANSI styling (colors, bold, host/user labels).
  - Integrates `BasicTextField` on the same line with `weight(1f)` using monospace typography matching terminal font size and theme colors.
  - Implements visible custom cursor via `decorationBox`:
    - `BLOCK` / `BLINKING_BLOCK`: Solid rectangular cursor block `█` (`width = fontSize * 0.58`, `height = fontSize * 1.15`) in `ImmersivePrimary`.
    - `UNDERLINE`: Horizontal underline cursor (`2.5.dp`).
    - `BAR`: Vertical bar cursor (`2.dp`).
  - Configured `KeyboardActions(onSend = ...)` to dispatch commands and reset the input field.
  - Added tap-to-focus on the terminal container and lazy list, instantly opening the soft keyboard and focusing the input field when tapping anywhere in the terminal view.
  - Added process logging via `BinBoxLogger.d` on prompt command dispatch.

### B. Terminal Screen Layout (`TerminalScreen.kt`)
- Removed obsolete `TerminalInputCapsule` component from below the quick keys bar.
- Wired `inputText`, `onInputTextChange`, `onSendCommand`, and `inputFocusRequester` directly into `TerminalBufferView`.
- Updated list auto-scroll `LaunchedEffect` to observe both `sessionLines.size` and `inputText`, scrolling to the prompt line so the cursor and input text remain in focus above the soft keyboard.
- Cleaned up unused `history` state observation from `TerminalScreen`.
- Added `TerminalKey` import for handling special key actions.

### C. Quick Keys Accessory Bar (`TerminalQuickKeysBar.kt`)
- Added optional `onSendEnter` callback and added an `ENTER` key (`AccessoryKeyButton`) directly alongside `TAB` and `ESC`, enabling one-touch command execution without requiring the virtual keyboard.
- Confirmed directional arrow buttons (`▲` and `▼`) provide history navigation.

### D. Session & Parsing Engine (`AnsiParser.kt`, `ShellSession.kt`, `SandboxDemoShellSession.kt`)
- Added `@Synchronized fun hasPendingLine(): Boolean` to `AnsiParser` to accurately detect whether an incomplete line (prompt) is currently awaiting input.
- Added `val hasPendingLine: Boolean get() = false` to `ShellSession` interface, overridden in `TransportShellSession` and `SandboxDemoShellSession`.

### E. Clean-Up
- Deleted obsolete `/app/src/main/java/com/inscopelabs/abx/binbox/ui/components/terminal/TerminalInputCapsule.kt`.

---

## 3. Compliance and Audit

### Prior Logging Gaps
- PRIOR LOGGING GAPS FOUND: none (`issues/pending/` is clean).

### Single-Responsibility File Discipline & Line Thresholds
- `TerminalBufferView.kt`: UI file, 268 lines (UI threshold: 1000 lines) — COMPLIANT.
- `TerminalScreen.kt`: UI file, 247 lines (UI threshold: 1000 lines) — COMPLIANT.
- `TerminalQuickKeysBar.kt`: UI file, 222 lines (UI threshold: 1000 lines) — COMPLIANT.
- `AnsiParser.kt`: Logic file, 283 lines (Logic threshold: 500 lines) — COMPLIANT.
- `ShellSession.kt`: Logic file, 297 lines (Logic threshold: 500 lines) — COMPLIANT.
- `SandboxDemoShellSession.kt`: Logic file, 318 lines (Logic threshold: 500 lines) — COMPLIANT.

### Version Increment Probability Assessment
- **Probability score:** 85 / 100 (Core terminal UX and shell interaction overhaul).
- **Action:** `version.properties` is a Protected Path per AGENTS.md §5 and `Hosted_Local_Shell-Addendum-Roles.md` §3 (AI Studio agent must never edit `version.properties`). No edits made to `version.properties`.

---

## 4. Verification and Commands
- Ran `compile_applet`: **BUILD SUCCESSFUL**
- Ran `gradle :app:testDebugUnitTest`: **BUILD SUCCESSFUL** (51 actionable tasks, 0 failures, all unit and Robolectric tests passing).
