# Agent Task Report: Terminal Prompt Alignment, Visibility, Enter Key & History Navigation Fixes

- **Timestamp (UTC):** 2026-09-03T174549Z
- **Task:** Fix terminal prompt spacing, prompt visibility persistence, Enter key registration, and history navigation behavior.

---

## 1. Request Summary
The user noted 4 specific discrepancies from the initial Command Input Capsule shell prompt integration:
1. **Prompt & Input Spacing**: An extraneous gap/number of spaces existed between the shell prompt text and the on-screen input/cursor position.
2. **Prompt Visibility**: The actual prompt was not visible at all times during interaction.
3. **Enter Key Registration**: Pressing enter on the software/hardware keyboard did not register or advance to a new prompt line.
4. **History Navigation & Prompt Anchor Disappearance**: Using up/down arrows left previous history commands on separate lines on the screen, and the normal prompt anchor (`ubuntu@bin-box-cbaf13de:~$ `) disappeared.

---

## 2. Root Cause Analysis
1. **Spacing Gap**: In `TerminalPromptRow`, the shell prompt string from `AnsiParser` often retained trailing spaces from pty outputs, while prompt generation heuristics also appended extra space, producing a double or triple space gap before the input field.
2. **Prompt Visibility**: When lines were received or buffer states shifted, `hasPendingLine` was false for completed buffers or during rapid output updates, dropping the prompt anchor unless dynamically preserved as `lastKnownPrompt`.
3. **Enter Key Registration**: `BasicTextField` with single-line configuration only listened to `ImeAction.Send` and had a guard `if (inputText.isNotBlank())`. Blank enters (the most common terminal action to advance a prompt) were dropped. Additionally, hardware and software keyboard `Key.Enter` / `Key.NumPadEnter` key events and multiple IME actions (`onDone`, `onGo`, `onNext`) were not captured.
4. **History Navigation & Anchor Disappearance**:
   - Up/down arrow key presses were previously transmitting raw ANSI escape sequences (`\u001b[A` / `\u001b[B`) directly to the shell backend while `inputText` remained disconnected from the terminal history buffer. The shell echoed output without ANSI CSI `K` (erase line) properly retaining the prompt in the line builder, causing the line builder to erase the entire line and leave duplicate command remnants.
   - History navigation needed to manage `inputText` directly via the client-side `viewModel.history`, cycling previous commands directly into `inputText` without injecting duplicate prompt lines or wiping the persistent prompt anchor.

---

## 3. Changes Implemented

### A. Spacing Normalization & Prompt Persistence (`TerminalBufferView.kt`)
- Added `normalizePrompt(annotated: AnnotatedString): AnnotatedString`:
  - Trims trailing whitespace from the prompt text while preserving all ANSI SpanStyles (colors, weights, backgrounds).
  - Enforces exactly a single space before the input field cursor according to standard terminal specifications (xterm/Termux).
- Added `var lastKnownPrompt by remember(activeSession?.id)`:
  - Dynamically captures the real shell prompt from the session line whenever active.
  - Keeps the prompt anchor (`ubuntu@bin-box-cbaf13de:~$ `) visible at all times, even between command executions or history cycling.
- Slices `sessionLines` appropriately so completed output lines are separated cleanly from the active input line.

### B. Enter Key Handling & Blank Line Advancement (`TerminalBufferView.kt`, `TerminalScreen.kt`, `TerminalQuickKeysBar.kt`)
- Updated `TerminalPromptRow`:
  - Added `.onPreviewKeyEvent` to intercept `Key.Enter` and `Key.NumPadEnter` directly on physical/software keyboards.
  - Implemented unified `dispatchEnter` handler:
    - If `inputText` is non-empty: dispatches command via `onSendCommand(inputText)` and clears input.
    - If `inputText` is blank: dispatches a newline command (`""` / `\n`) to advance the terminal to a fresh prompt line just like native shells.
  - Expanded `keyboardActions` to handle `onSend`, `onDone`, `onGo`, and `onNext`.
  - Updated Quick Keys accessory `ENTER` button to trigger the same clean enter dispatch.

### C. Command History Navigation with Up/Down Arrows (`TerminalScreen.kt`, `TerminalBufferView.kt`, `TerminalQuickKeysBar.kt`)
- Wired `viewModel.history` into `TerminalScreen`:
  - Added `historyIndex` and `uncommittedInput` tracking.
  - Implemented `onHistoryUp`: Cycles backwards through history, loading previously executed commands directly into `inputText` without echoing new lines onto the screen.
  - Implemented `onHistoryDown`: Cycles forward through history, returning to `uncommittedInput` when reaching the bottom.
  - Passed `onHistoryUp` and `onHistoryDown` to:
    - `TerminalBufferView`'s `TerminalPromptRow` via `.onPreviewKeyEvent` (`Key.DirectionUp` / `Key.DirectionDown`).
    - `TerminalQuickKeysBar`'s `▲` and `▼` accessory buttons.
  - Automatically resets history tracking on Enter, command dispatch, and Ctrl+C (`^C`).

### D. ANSI CSI Line Erase Fix (`AnsiCsiHandler.kt`)
- Fixed CSI `K` (`\u001b[K`) handling:
  - Erase in Line (`0K` / `K`): Erases from cursor column forward rather than clearing the entire line builder.
  - Preserves the prompt text preceding the cursor when applications or shells issue line clearing sequences.

---

## 4. Compliance and Audit

### Prior Logging Gaps
- PRIOR LOGGING GAPS FOUND: none (`issues/pending/` is clean).

### Single-Responsibility File Discipline & Line Thresholds
- `TerminalBufferView.kt`: UI file, 403 lines (UI threshold: 1000 lines) — COMPLIANT.
- `TerminalScreen.kt`: UI file, 300 lines (UI threshold: 1000 lines) — COMPLIANT.
- `TerminalQuickKeysBar.kt`: UI file, 240 lines (UI threshold: 1000 lines) — COMPLIANT.
- `AnsiCsiHandler.kt`: Logic file, 136 lines (Logic threshold: 500 lines) — COMPLIANT.

### Version Increment Probability Assessment
- **Probability score:** 90 / 100 (Core terminal UX bug fixes for prompt alignment, enter key, and history navigation).
- **Action:** `version.properties` is a Protected Path per AGENTS.md §5 and `Hosted_Local_Shell-Addendum-Roles.md` §3 (AI Studio agent must never edit `version.properties`). No edits made to `version.properties`.

---

## 5. Verification and Commands
- Ran `compile_applet`: **BUILD SUCCESSFUL**.
- Ran `gradle :app:testDebugUnitTest`: **BUILD SUCCESSFUL** (All unit and Robolectric tests passing, 0 failures).
