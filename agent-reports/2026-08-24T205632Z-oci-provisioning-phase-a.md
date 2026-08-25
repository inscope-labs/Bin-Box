# OCI Provisioning Package — Phase A (identity, auth, state machine)

**Agent:** Claude
**Date:** 2026-08-24
**Scope note:** This is a partial implementation of the OCI onboarding &
shell provisioning package described in `oci-provision.md`. It covers the
layers that don't depend on knowing OCI's exact REST wire formats. It does
NOT cover the OCI Compute/Network/Identity API client — see "Deferred" below.

## What was built

`com.inscopelabs.abx.binbox.oci/`

- `identity/` — `OciAccount`, `OciFingerprint`, `OciCredentials`,
  `OciCredentialsStore`, `OciKeyManager`
- `auth/` — `OciRequestSigner`, `OciSignatureHeaders`
- `provisioning/` — `OciProvisioningState` (§31 state machine),
  `OciErrorCategory`/`OciProvisioningError` (§33), `OciProvisioningContext`,
  `OciProvisioningSession`, `OciProvisioningRepository`,
  `OciSshKeyProvisioner` (§20 — VM SSH key generation, added in a follow-up
  pass; reuses the existing `IKeyRepository`/`KeyRepositoryImpl` rather than
  a new store, since that's already encrypted-at-rest from Phase 9)
- `terminal/` — `ShellHost` (interface), `OciShellHost`
- `wizard/` — `OciOnboardingStage` (§9), `OciOnboardingViewModel` — real
  through `CONNECTION_VERIFICATION`, plus real (order-independent)
  `GenerateVmSshKey` handling; everything else stubbed

Also: added `AppError.AuthError.OciAuthenticationFailed` variant to the
existing error taxonomy in `core/error/AppError.kt` (small, additive change
to a non-protected file).

## Design decisions worth flagging

1. **OCI signing key never leaves AndroidKeyStore.** Unlike
   `SshKeyManager`, which must export raw PEM for JSch, `OciKeyManager`
   generates the RSA-2048 signing key directly in AndroidKeyStore and only
   ever exposes the public key. `OciRequestSigner` signs via a
   `Signature` handle against the Keystore entry — private key bytes never
   exist in app memory. This is a deliberate improvement over mirroring
   SshKeyManager's export pattern, not an oversight.

2. **No `ShellTransport`/`DirectSshTransport`/`AbxRelayTransport` layer.**
   The doc (§28-30) proposes a new transport abstraction. Bin-Box already
   has one — `ITransport`/`TerminalSessionFactory`, built in Phase 3/5/9 —
   and it already consumes `ConnectionProfile` via `IHostRepository`.
   Building a second transport layer here would duplicate rather than
   complete that path, which is exactly the drift the standing addendum
   flagged (reconciling this package's ShellHost/ShellTransport stack
   against the upgrade plan's TerminalProvider/TerminalSession stack).
   `OciShellHost.toConnectionProfile()` bridges directly into the existing
   stack instead. **This is an architectural call, not just an
   implementation detail — please confirm or override.**
   `AbxRelayTransport` (§30) has no equivalent in this repo at all (no ABX
   relay/gateway concept exists yet), so relay-mode connections aren't
   stubbed against a transport that doesn't exist — left absent, not faked.

3. **`OciCredentialsStore` is SharedPreferences, not a new Room table.**
   Single active OCI profile, not a relational record — didn't seem to
   warrant a schema migration yet. §7's "support multiple future OCI
   profiles" is an explicit follow-up, not silently dropped.

4. **`OciProvisioningSession.sshKeyAlias` holds two different kinds of
   reference depending on which key it's pointing at, by necessity.** The
   OCI API signing key (§8) never needs raw bytes and lives in
   AndroidKeyStore, referenced by string alias. The VM SSH key (§20) must
   hand JSch raw PEM, so it's stored the same way every other SSH host key
   in this app is — an encrypted `IKeyRepository` row, referenced by `Long`
   id. `sshKeyAlias` stores that id stringified. Documented inline in
   `OciSshKeyProvisioner`, but worth a second look before this gets
   consumed elsewhere — the field name implies one storage scheme when two
   exist.

## Correction from the initial pass

The first version of `OciOnboardingViewModel`'s class kdoc referenced a
function, `advanceContextDiscovery`, that was never actually written, and
described `verifyConnection()`'s not-yet-built path as throwing
`NotImplementedError` when it actually reports the gap through UI state.
Caught and fixed on a second read before this report was finalized — noting
it here since it's the kind of drift worth watching for in doc comments
written alongside fast-moving stubs.

## Deferred — not built in this phase

`api/` (compartments, networking, compute REST clients) is **not
implemented**. Sections §15–§26 of the doc (context discovery, network
idempotency, instance provisioning, capacity/quota handling, SSH
verification, host registration) all depend on exact OCI SDK request/
response shapes, pagination behavior, and endpoint versioning that
shouldn't be guessed at from a spec doc — a wrong implementation here is
worse than an honest gap. `OciOnboardingViewModel.verifyConnection()` is
wired up to call this client but currently just reports it's unbuilt.

Also not built: SSH key generation/attachment for the *provisioned VM*
(§20) — distinct from the OCI API signing key, which Phase A does cover.

## Verification

No Android SDK/Gradle toolchain in this sandbox (same constraint as prior
Phase 3/5/9 work) — verified structurally: all 15 new files brace-balanced,
cross-file symbol references checked by hand against actual repo state
(`AuthType`/`ProtocolType` enum values, `ConnectionProfile` fields,
`AppError`/`AppResult` constructors, Moshi version 1.15.2's
`com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory` import path).
One bug caught and fixed pre-commit: `OciProvisioningError.cause` is a
`Throwable`, which has no Moshi adapter — would have failed at runtime the
first time a session with a populated error was saved, since
`OciProvisioningSession` (which nests it) is persisted via Moshi's
reflection adapter. Marked `@Transient`. Not verified by an actual build —
flag for review before merging past a real compile.

## Branch

Committed to `oci-provisioning-phase-a`, not `main` — new package, low risk
of file-level conflict with AI Studio's concurrent work, but a real
architectural decision (item 2 above) is embedded in it that should be
reviewed before merge rather than landing directly on `main`.
