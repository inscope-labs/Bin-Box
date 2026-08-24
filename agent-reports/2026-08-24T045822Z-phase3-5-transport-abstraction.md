# Agent Report — Phase 3 & 5: Transport Abstraction

**Timestamp (UTC):** 2026-08-24T045822Z
**Agent:** Claude (architect/auditor)
**Scope:** new transport/ package; terminal/engine/ShellSession.kt

## What changed
- Added `transport/ITransport.kt`: the Phase 3 core deliverable. Interface +
  `TransportListener` contract. Reuses `terminal.model.SessionState`
  rather than introducing a parallel state model.
- Added `transport/SshTransport.kt`: Phase 5 SSH provider, backed by the
  existing (previously unused) `jsch` dependency. Extracted directly from
  the JSch logic that previously lived inline in `SshShellSession`.
- Refactored `SshShellSession` to delegate to `SshTransport` via
  `ITransport`; it no longer imports or references JSch types.

## Why
Reconnaissance before this work found `TerminalSessionManager` /
`ShellSession` already existed (session lifecycle), but with no transport
abstraction underneath — each `ShellSession` implementation embedded its own
connection logic (JSch, `ProcessBuilder`, raw `Socket`) directly. This is
the gap `DEVELOPMENT_PLAN.md` Step 1.4 (`ITransport`) called out as
undelivered. This change closes that gap for the SSH path end-to-end.

## Files touched
- `app/src/main/java/com/inscopelabs/abx/binbox/transport/ITransport.kt` (new)
- `app/src/main/java/com/inscopelabs/abx/binbox/transport/SshTransport.kt` (new)
- `app/src/main/java/com/inscopelabs/abx/binbox/terminal/engine/ShellSession.kt` (edited — SshShellSession only)

## Verification performed
- No Android SDK / Gradle toolchain available in this sandbox (network is
  restricted to a fixed allowlist that does not include Google's Maven
  repo), so a full `./gradlew build` could not be run here.
- Verified instead by: brace-balance check on all three touched files,
  audit of `SshShellSession`'s public constructor signature (unchanged —
  `TerminalSessionFactory` requires no edits), removal of now-dead JSch
  imports, and a repo-wide search confirming no test references
  `SshShellSession` internals (only a type check: `sshSession is
  SshShellSession`, which still holds).
- **Recommend running `./gradlew :app:compileDebugKotlin` on next sync** to
  catch anything a structural review can't (e.g. subtle type mismatches).

## Not done in this change (tracked, not silently dropped)
- Host-key verification, keepalive, reconnection — Phase 5 deliverables in
  the upgrade plan, not yet in `SshTransport`.
- `LocalShellSession` and `TelnetShellSession` still embed transport logic
  inline. Left untouched deliberately: Phase 4 (Termux) and Phase 7
  (WebSocket/backend) are delegated to the AI Studio agent. **When those
  phases are implemented, they should migrate onto `ITransport`**
  (`LocalProcessTransport`, `TcpTransport`) rather than continuing the old
  inline pattern — otherwise a second transport model re-emerges, which is
  the exact duplication problem flagged for `ShellHost`/`ShellTransport`
  vs `TerminalProvider` in the cross-plan addendum. Worth stating this
  explicitly in whatever task spec goes to AI Studio for Phase 4/7.
- `TerminalSessionFactory` was not modified — SSH construction already used
  named constructor args that remained compatible.

## Next
Phase 9 (Security & Reliability) is next per the agreed sequencing. It will
build on the existing `SecureStorageService`/`SshKeyManager` (already in
the repo) — no Keystore work has started yet.
