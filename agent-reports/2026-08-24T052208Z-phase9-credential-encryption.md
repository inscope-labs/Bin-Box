# Agent Report — Phase 9: Credential Encryption at Rest

**Timestamp (UTC):** 2026-08-24T052208Z
**Agent:** Claude (architect/auditor)
**Scope:** new security/CredentialCrypto.kt; data/repository/{Key,Host}RepositoryImpl.kt; ui/viewmodel/BinBoxViewModel.kt (DI wiring); two test files

## What changed
Reconnaissance found `SecureStorageService` already existed — a real,
working AES-256-GCM AndroidKeyStore-backed encryption service with a
software fallback for test environments — but was **never called from
anywhere in the app**. `KeyEntity.privateKey` and
`HostEntity.password`/`keyPassphrase` were being persisted to the Room
SQLite database as plain text.

- Added `CredentialCrypto`: encrypt/decrypt boundary for individual
  fields, tagging ciphertext with an `ENC1:` prefix. Untagged (legacy)
  values pass through unchanged rather than being treated as corrupt —
  they're upgraded to ciphertext the next time the record is saved.
- `KeyRepositoryImpl`: SSH private keys encrypted before `insertKey`,
  decrypted after read. `generateRsaKeyPair()` still returns plaintext to
  the caller for immediate use; only the persisted copy is encrypted.
- `HostRepositoryImpl`: host `password` and `keyPassphrase` encrypted
  before insert/update, decrypted after read.
- `SecureStorageService` now constructed once in
  `BinBoxViewModel.createDependencyGraph` and threaded into both repos.
- Room schema untouched — columns stay TEXT; only their contents changed.
  No migration needed.

## Why
This is the concrete form of the upgrade plan's Phase 9 deliverable
("Encrypted credential storage... No plaintext credentials in logs") —
the crypto primitive already existed but wasn't wired to anything that
actually stores a credential.

## Files touched
- `security/CredentialCrypto.kt` (new)
- `data/repository/KeyRepositoryImpl.kt`
- `data/repository/HostRepositoryImpl.kt`
- `ui/viewmodel/BinBoxViewModel.kt` (DI wiring only)
- `test/.../domain/RepositoryAndUseCasesTest.kt` (constructor call site update)
- `test/.../core/DiagnosticsAndSecurityTest.kt` (+3 new tests for CredentialCrypto)

## Verification performed
- No Android SDK/Gradle toolchain reachable in this sandbox — same
  constraint as prior reports.
- Brace-balance check across all touched files.
- Confirmed no other call sites construct `HostRepositoryImpl`/
  `KeyRepositoryImpl` with the old 1-2-arg signature.
- Cross-checked against the pre-existing
  `testSecureStorageService_encryptDecryptAndClear` test, which already
  confirms `SecureStorageService` round-trips correctly under Robolectric
  in this repo — gives reasonable confidence the new
  `CredentialCrypto` tests will behave the same way.
- **Recommend running the actual test suite (`./gradlew testDebugUnitTest`)
  on next sync** — this is exactly the kind of change (constructor
  signature change touching DI wiring + two repos + two test files) worth
  a real build/test pass rather than trusting structural review alone.

## Coordination note
A concurrent AI Studio commit (`0cb8245`, "implement alternate buffer and
key support" — Phase 2 terminal work) landed on `main` between this
agent's previous push and this one. No file overlap; rebased cleanly, no
conflicts.

## Not done in this change (tracked, not silently dropped)
- A decrypt failure on a tagged-but-corrupted value (e.g. after key
  rotation) returns `null` rather than the raw ciphertext, which will
  currently surface as a missing password/passphrase in the UI rather
  than a clear "credential unreadable, please re-enter" state. Needs a
  UX pass, not addressed here.
- Host-key verification, keepalive, reconnection (SshTransport, Phase 5)
  — still open from the prior report.
- Biometric prompt wrapper, TLS certificate validation, session-token
  expiration, secure clipboard handling — remaining Phase 9 deliverables
  from the upgrade plan not yet started.

## Next
Continuing Phase 9: reliability (network-loss recovery, session
reconnection, process death recovery) is the next slice, unless
redirected toward the OCI provisioning package instead.
