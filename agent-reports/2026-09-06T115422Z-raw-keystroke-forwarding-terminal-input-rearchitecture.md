# Agent Task Report: Raw Keystroke Forwarding — Terminal Input Rearchitecture

- **Timestamp (UTC):** 2026-09-06T115422Z
- **Task:** Replace the client-buffered command-input model with Termux/xterm-style raw keystroke forwarding, to fix cursor blink, keyboard-covers-keys, unreliable history nav, dropped multi-line paste, no line-wrap, and keyboard-auto-dismiss-on-Enter.
- **Agent:** Claude (direct-commit mode per INC-002 — AI Studio unavailable).

---

## 1. Request Summary

Dev reported six terminal UX defects on-device:
1. Cursor visibility toggles inconsistently regardless of configured cursor style.
2. Full shell `PS1` (`ubuntu@bin-box-cbaf13de:~$ `) shown instead of a minimal prompt.
3. Arrow-key history navigation feels laggy/unreliable ("works sometimes").
4. Pasting multi-line text only displays the first line.
5. Long command lines shift horizontally instead of wrapping.
6. Soft keyboard dismisses itself immediately after Enter.

Dev directed that both open design decisions (prompt minimization approach; history ownership model) be resolved per Termux/industry-standard terminal behavior, not the originally-stated preference for client-owned history — see Root Cause Analysis §2.4 for why a client-owned history buffer is structurally incompatible with full-screen program support (vim/htop/less/tmux).

---

## 2. Root Cause Analysis

**2.1 — Architecture mismatch, not six independent bugs.** The prior `TerminalPromptRow` treated the command line as an editable `BasicTextField` value (`inputText`) held in Compose state, separate from the real PTY echo (`AnsiParser`/`sessionLines`). Two parallel representations of "what's typed" existed simultaneously, and every defect traces back to their drift or to constraints (`singleLine = true`, `ImeAction.Send`) imposed by treating the terminal as a text-editing form field instead of a terminal.

**2.2 — Backend was already correct; only the UI bypassed it.** `ShellSession.sendRawBytes`/`sendInput`/`sendSpecialKey`, `TerminalKeyTranslator` (full CSI/backspace/enter/ctrl-modifier byte mappings), and `PtyNative` (native fork/exec over a real PTY) already implement Termux-equivalent raw-forwarding primitives. The UI layer never called them for live typing — it only used them for the accessory-bar special keys, while routing all typed/pasted text through the client-side `inputText` value and `viewModel.sendCommand(text)` (whole-line dispatch on Enter only).

**2.3 — Per-defect mapping:**
- **(1) Cursor blink:** unconditional `LaunchedEffect(Unit) { delay(530); toggle }` applied regardless of `cursorStyle`, and never paused during typing.
- **(3) History lag:** no raw ANSI escapes were being sent server-side (verified — client-side history interception was already in place from a prior fix), but `LaunchedEffect(sessionLines.size, inputText)` re-triggered `animateScrollToItem` on every keystroke/history step, and `onPreviewKeyEvent(Key.DirectionUp/Down)` is inherently unreliable across soft-keyboard IME implementations that don't emit real `KeyEvent`s for on-screen arrows.
- **(4) Multi-line paste:** `onInputTextChange` had no newline handling; pasted `\n`-containing text landed in a `singleLine = true` field, which visually truncates at the first line break.
- **(5) No wrap:** `singleLine = true` was set explicitly — working as specified, just the wrong spec for a terminal.
- **(6) Keyboard dismiss:** `imeAction = ImeAction.Send` + `KeyboardActions(onSend/onDone/onGo/onNext)` — several IMEs auto-hide themselves after firing a "send/done" action independent of app focus state.
- **(2) Full PS1 shown:** not a bug — the displayed text is the shell's own real `PS1` echoed verbatim over the PTY. Confirmed via Termux research (see §2.4) that this is correct, expected terminal behavior.

**2.4 — Termux research finding (governs both open decisions).** Termux's `TerminalView` is a raw `View` (not a text-editing widget) driving a `TerminalEmulator` screen-buffer model; every keystroke, including arrows and Enter, is forwarded to the PTY immediately and uncomposed. There is no client-owned command buffer and no client-owned history for the live terminal — history is the shell's own `readline`, driven by raw `\x1b[A`/`\x1b[B` sequences it interprets itself. The one Termux feature resembling client-side history is a separate, optional 2024 toolbar add-on that types a line and sends it — it does not replace or reroute the real terminal's input path. **Decision, per Dev's direction to follow the Termux/industry-standard answer:** history stays shell-owned; the real `PS1` is shown as-is (no client-side prompt substitution) — a shorter prompt is a server-side `PS1` config change, not a BinBox UI feature. A client-owned command buffer was rejected outright: it is structurally incompatible with full-screen programs (vim/htop/less/tab-completion), which require every keystroke forwarded immediately in raw mode with zero client buffering.

**2.5 — Separate, deeper finding (out of scope for this task, tracked for urgent follow-up per Dev).** `AnsiParser` is a scrollback line-list (`getLines(): List<TerminalLine>`, lines committed/appended sequentially), not a true 2D-addressable screen grid, despite already containing alternate-screen-buffer scaffolding explicitly commented for vim/htop/nano support. Cursor-reposition-and-overwrite (what full-screen programs depend on continuously) is likely unreliable today, independent of this task's fixes. This is Phase 3.3 ("terminal emulator") of the RTX-100 plan and needs its own scoped rewrite — explicitly NOT attempted here to avoid conflating two different-sized problems.

---

## 3. Changes Implemented

### A. New raw keystroke capture surface (`RawInputCaptureField.kt`, new file)
- Invisible `BasicTextField` holding a permanent zero-width-space sentinel value, never displaying or accumulating real text.
- `onValueChange` diffs against the sentinel invariant (reset every callback) to classify each edit as insert / backspace / IME-replace, forwarding immediately via callbacks — no client-side text buffer at any point.
- Arrow keys and Enter intercepted via `onPreviewKeyEvent` and forwarded as real terminal key events; printable input/paste falls through to `onValueChange` and is forwarded as raw bytes, embedded newlines included (a multi-line paste executes each line in sequence, same as any terminal — this is what fixes defect 4).
- `imeAction = ImeAction.None`, no `KeyboardActions` binding — nothing in this field can trigger a keyboard-dismissing IME action (fixes defect 6).

### B. `TerminalBufferView.kt`
- Removed `normalizePrompt()` and `lastKnownPrompt` — both existed only to reconcile the client input value against the real prompt text; that duplication no longer exists.
- `TerminalPromptRow` now renders the real echoed last line directly (`renderLineAnnotatedString(sessionLines.last(), ...)`) with `softWrap = true` (fixes defect 5) and an inline cursor anchored to the end of that real text — no longer a position independently guessed by a separate field.
- Cursor now renders for all styles including `BAR` (previously handled only via `cursorBrush` on the now-removed input field).
- Signature changed: `inputText`/`onInputTextChange`/`onSendCommand`/`onHistoryUp`/`onHistoryDown` replaced with `onRawInsert`/`onBackspace`/`onEnter`/`onArrowUp`/`onArrowDown`.

### C. `TerminalScreen.kt`
- Removed `inputText`, `historyIndex`, `uncommittedInput` state and the client-side `onHistoryUp`/`onHistoryDown` recall logic entirely.
- Added `typedLineShadow` — tracks the current line **only** so BinBox's own history browser (`historyUseCases.recordHistory`) still has something to record; it has no effect on display or on what's sent to the shell.
- `sendRaw`/`sendBackspace`/`sendEnter` now call `viewModel.sendRawInput`/existing raw-forwarding APIs directly per keystroke; arrows call `viewModel.sendSpecialKey(TerminalKey.ARROW_UP/DOWN)` unconditionally (fixes defect 3 — no client recall path to branch on, and no per-step scroll-animation trigger since auto-scroll now keys on `sessionLines` as a whole rather than `inputText`).
- Cursor blink now gates on `cursorStyle == BLINKING_BLOCK` only and restarts (holding solid for 600ms) on typing activity (fixes defect 1).

### D. `TerminalQuickKeysBar.kt`
- Removed `onSendCommand`/`onSendEnter`/`onHistoryUp`/`onHistoryDown` params. ENTER now always sends `"\r"` via `onSendRawInput`; arrows always call `onSendSpecialKey(ARROW_UP/DOWN)`. The "LOGS" quick action sends its command text + `"\r"` via the same raw path instead of the removed command-buffer dispatch.

**Not changed:** `BinBoxViewModel.sendCommand`, `ShellSession`, `PtyNative`, `TerminalKeyTranslator`, `AnsiParser`/`AnsiCsiHandler` — all already correct or out of scope (§2.5).

---

## 4. Compliance and Audit

### Prior Logging Gaps
- PRIOR LOGGING GAPS FOUND: none (`issues/pending/` does not exist / is clean).

### Single-Responsibility File Discipline & Line Thresholds
- `TerminalScreen.kt`: UI file, 290 lines (threshold: 1000) — COMPLIANT.
- `TerminalBufferView.kt`: UI file, 342 lines (threshold: 1000) — COMPLIANT.
- `TerminalQuickKeysBar.kt`: UI file, 221 lines (threshold: 1000) — COMPLIANT.
- `RawInputCaptureField.kt`: UI file, new, 128 lines (threshold: 1000) — COMPLIANT.

### Version Increment Probability Assessment
- **Probability score:** 85 / 100 (terminal input model change — no protected-path files touched).
- **Action:** `version.properties` is a Protected Path per AGENTS.md §5. No edits made to `version.properties`.

---

## 5. Verification and Commands

- **No Android SDK / Gradle toolchain is available in this agent's sandbox** (network egress is allow-listed to source/package registries only; no `google`/Android SDK repos reachable). Unlike prior Studio-agent reports, **this change has not been compiled or unit-tested by the agent that authored it.**
- Verification performed instead: manual full re-read of all four changed/new files after editing; grep-based sweep confirming no dangling references to removed symbols (`inputText`, `historyIndex`, `onHistoryUp`/`onHistoryDown`, `onSendCommand`, `onSendEnter`, `normalizePrompt`, `lastKnownPrompt`) anywhere in `app/src/main/java/`; confirmed no test files couple to the changed composable signatures.
- **Action required before this is trusted on-device:** run `gradle :app:testDebugUnitTest` (or equivalent CI) and a real device/emulator pass covering: typing, backspace, Enter, arrow-key history recall, multi-line paste, and keyboard persistence after Enter. Recommend this happens via CI-on-push or a manual build, since local compiler verification could not be performed here.
