# Agent Report — Phase 5/9: SSH Host-Key Verification

**Timestamp (UTC):** 2026-08-24T101910Z
**Agent:** Claude (architect/auditor)
**Scope:** new security/HostKeyStore.kt; data/entity, data/dao, data/database (Room v1->v2); transport/SshTransport.kt; terminal/engine/{ShellSession,TerminalSessionFactory}.kt; ui/viewmodel/BinBoxViewModel.kt; one test file

## What changed
`SshTransport` hardcoded `StrictHostKeyChecking=no` since it was first
extracted (Phase 3/5 report already flagged this as the highest-priority
open item). Every SSH connection accepted whatever host key the server
presented, with zero verification — a real MITM exposure, not a missing
nicety.

- Added `KnownHostKeyEntity`/`KnownHostKeyDao` (Room schema v1 -> v2,
  `fallbackToDestructiveMigration()` — acceptable pre-release, no shipped
  users, schema wasn't being exported/tracked anyway). Only a SHA-256
  fingerprint is stored, not the raw key.
- Added `HostKeyStore`: implements JSch's `HostKeyRepository` as
  trust-on-first-use. First contact with a host:port is trusted and
  remembered; a later connection presenting a **different** key is
  rejected outright (`HostKeyRepository.CHANGED`), not merely warned
  about.
- `SshTransport` now takes an optional `HostKeyRepository`. Supplied ->
  `StrictHostKeyChecking=yes`, so JSch hard-fails a changed key. Absent ->
  old permissive behavior kept, but now logs loudly rather than silently.
- Threaded `hostKeyStore` through `SshShellSession` ->
  `TerminalSessionFactory` -> `BinBoxViewModel`'s DI graph, where a real
  `HostKeyStore` backed by the app database is always supplied in
  production.
- 3 new Robolectric tests: TOFU accept-then-match, reject-on-mismatch,
  bracketed non-default-port host string parsing.

## Files touched
- `security/HostKeyStore.kt` (new)
- `data/entity/Entities.kt`, `data/dao/Daos.kt`, `data/database/AppDatabase.kt`
- `transport/SshTransport.kt`
- `terminal/engine/ShellSession.kt`, `terminal/engine/TerminalSessionFactory.kt`
- `ui/viewmodel/BinBoxViewModel.kt` (DI wiring only)
- `test/.../core/DiagnosticsAndSecurityTest.kt` (+3 tests)

## Verification performed
- No Android SDK/Gradle toolchain in this sandbox — same constraint as
  every prior report.
- Brace-balance check across all 9 touched/added files.
- Full call-site audit: every existing `TerminalSessionFactory`/
  `SshShellSession` construction (production and test) uses named
  arguments, so the new defaulted parameters (`hostKeyStore` /
  `hostKeyRepository`) don't break any of them.
- Cross-referenced JSch's `HostKeyRepository`/`HostKey` API (interface
  constants `OK`/`NOT_INCLUDED`/`CHANGED`, `HostKey.getType()`/
  `getHost()`/`getFingerPrint(JSch)`, `Session.setHostKeyRepository()`)
  against the already-working JSch usage elsewhere in `SshTransport` for
  signature consistency.
- **Recommend, on next sync: run the test suite, then a real SSH
  connection test against a known host** to confirm the live handshake
  path (not just the repository logic in isolation) behaves as expected —
  this is exactly the kind of change where a structural review alone
  isn't sufficient confidence.

## Known limitations (flagged, not hidden)
- `HostKeyStore.getHostKey()` / `getHostKey(host, type)` return empty —
  only fingerprints are persisted, so there's nothing to reconstruct a
  full `HostKey` from. Nothing in this codebase currently calls either
  method.
- A host-key mismatch is a hard failure with no way to distinguish
  "attack" from "you rebuilt the VM and it legitimately has a new key."
  There is no UI path yet to review/clear a changed key — the connection
  will simply keep failing until someone deletes the row directly (or a
  future UI feature is built for it). Flagged as the natural next Phase 9
  UX item.
- Keepalive and reconnection (Phase 5 deliverables) are still open from
  the prior report — unaffected by this change.

## Coordination note
Push initially failed with a GitHub auth error ("Invalid username or
token"); retried and succeeded immediately, confirmed transient by an API
check returning HTTP 200 with the same token in between. No remote
changes had landed in the meantime — no rebase was needed this time.

## Next
Reconnaissance suggests keepalive/reconnection (SshTransport) or the
security-item backlog (TLS cert validation, secure clipboard handling)
as the next Phase 9 slice, pending direction.
