# OCI Provisioning Package — Phase C (provisioning engine)

**Agent:** Claude
**Date:** 2026-08-25
**Depends on:** Phase A (identity/auth/state machine), Phase B (`api/` REST client)

## What this covers

The orchestration logic Phase B's `api/` layer exists to serve: idempotent
networking (§18-19), capacity-checked instance launch + poll + public-IP
discovery (§21-24).

- `NetworkProvisioner.kt` — discover-or-create for VCN, internet gateway,
  default route table, subnet. Discoverable by a stable tag
  (`"bin-box-managed"`), not a per-session id — re-running the wizard reuses
  the same network in a compartment instead of creating a new one every
  time. `opc-retry-token`s are session-scoped separately, guarding against
  a single request being duplicated by a crash-and-retry — a different
  concern from "does this resource already exist."
- `ComputeProvisioner.kt` — `checkCapacity` (via `CreateComputeCapacityReport`,
  per Phase B's finding) then `launchAndWait` (launch, poll to RUNNING,
  resolve public IP via VNIC attachment → VNIC). Callers must call
  `checkCapacity` before `launchAndWait` — kept as two separately-retryable
  steps rather than one method, so a capacity re-check doesn't also
  re-trigger a launch attempt.
- `ProvisioningPoller.kt` — generic poll-until-done/failed/timeout helper.
- `OciProvisioner.kt` — top-level orchestrator; sequences network → capacity
  check → launch, advancing and persisting `OciProvisioningSession` after
  every step (§32 resumability — a crash mid-provision leaves the session
  at the last completed step, not back at the start).
- `OciFreeTierShapes.kt` — Always Free shape constants
  (`VM.Standard.A1.Flex`, `VM.Standard.E2.1.Micro`), confirmed against
  Oracle's official free-tier resource page this session, not assumed.

## A real bug caught before pushing — worth flagging explicitly

The first draft of every file in this phase wrapped `OciProvisioningError`
inside `AppResult.Error(...)`. That would not have compiled:
`AppResult.Error` is `data class Error(val error: AppError)` — pinned to
`AppError` specifically, not generic. I'd been using `AppResult<T>` as if
it were a generic Result type throughout `NetworkProvisioner`,
`ComputeProvisioner`, `OciProvisioner`, and `ProvisioningPoller`.

Fixed by introducing `OciResult<T>` — a dedicated result type for this
package's provisioning functions, carrying `OciProvisioningError` instead.
This is also the architecturally correct call independent of the compile
error: `OciErrorCategory`'s own kdoc (from Phase A) already explains it's
deliberately kept separate from `AppError` because these categories map to
wizard-specific recovery actions, not generic app error handling — routing
through `AppResult<AppError>` would have fought that same separation.
`OciProvisioningRepository` and `OciSshKeyProvisioner` (Phase A) correctly
use real `AppResult<AppError>` throughout and were not affected — this bug
was isolated to the four files written this session.

Flagging this one more explicitly than usual: it's a class of mistake
(assuming a project's Result type is generic when it's actually pinned to
one error type) that's easy to make silently and only surfaces at compile
time — worth double-checking anywhere else in the codebase where a
domain-specific error type might get threaded through `AppResult`.

## Design decisions worth flagging

1. **Compute instance discovery is NOT idempotent the way networking is.**
   `ComputeProvisioner` doesn't search for an existing "bin-box-managed"
   instance before launching, unlike `NetworkProvisioner`'s VCN/subnet/IGW
   handling. An existing instance found that way would still need its
   lifecycle state, shape, and reachability re-verified before reuse is
   actually safe — equivalent cost to just re-running via the retry token.
   Retry safety here comes from `opc-retry-token`, not a pre-launch
   discovery pass.
2. **`OciFreeTierShapes` defaults to HALF the Ampere A1 tenancy pool** (2
   OCPUs / 12 GB, not the max 4/24) so one instance doesn't consume the
   entire free allocation and block a second instance later. This is a
   product decision I made a default call on, not a technical constraint —
   flagged for confirmation, easy to change.
3. **`OciProvisioner` does not select compartment/AD/shape/image itself** —
   takes a fully resolved `OciProvisioningContext` and fails cleanly if any
   selection is missing. Those selections belong in a discovery/UI step in
   front of this class, which is NOT built yet (see below).

## Explicitly NOT wired into the ViewModel this pass

`OciOnboardingViewModel` is untouched in this phase. `OciProvisioner` is a
real, callable, structurally-verified unit — but nothing in the wizard
calls it yet. The context-discovery step that would populate
`OciProvisioningContext` (compartment list, AD list, shape/image list) also
isn't built. Both are natural next steps, kept separate from this phase to
keep the diff reviewable and the bug above easier to isolate.

## Verification

Structural only, as with Phases A/B (no Android/Gradle toolchain) — 38
files across the whole `oci/` package brace/paren-balanced. Cross-file
Retrofit interface signatures checked by hand against every call site
(argument names/order) after the `OciResult` rewrite. Two things a real
build should confirm before relying on this:
1. The `return@poll` labeled-return usage inside `ComputeProvisioner`'s
   `discoverPublicIp` fetch lambda — local return via implicit
   function-name label, valid Kotlin for both inline and non-inline
   higher-order functions, but not exercised by a compiler here.
2. Moshi's null-field omission default for `LaunchInstanceRequest.shapeConfig`
   (null for non-flex shapes) — relying on Moshi's default
   `serializeNulls(false)` behavior to omit the field entirely rather than
   send `"shapeConfig":null`, not explicitly configured/tested.
