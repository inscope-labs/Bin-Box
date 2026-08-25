# OCI Provisioning Package — Phase D (context discovery + wizard wiring)

**Agent:** Claude
**Date:** 2026-08-25
**Depends on:** Phases A/B/C

## What this covers

The two biggest gaps flagged after Phase C: `OciProvisioningContext` had
nothing to populate it, and nothing in `OciOnboardingViewModel` called
`OciProvisioner`. Both closed this pass.

- `OciContextDiscovery.kt` — fetches compartments (`ListCompartments`),
  availability domains (`ListAvailabilityDomains`), Always-Free-eligible
  shapes (`ListShapes`, filtered to what `OciFreeTierShapes` knows about —
  raw paid-shape noise excluded), and images (`ListImages`). Doesn't pick
  defaults or make selections — same boundary `OciProvisioner` already
  draws for the context it consumes.
- `OciOnboardingViewModel` — new real events: `DiscoverContext`,
  `SelectCompartment`, `SelectAvailabilityDomain` (also triggers shape
  discovery, since `ListShapes` needs both compartment and AD),
  `SelectShape` (triggers image discovery), `SelectImage`,
  `StartProvisioning` (calls `OciProvisioner.provision()` end to end,
  resolving the VM SSH public key from UI state or, on resume, by looking
  it up via `IKeyRepository` from the persisted session's key reference).
  `stageFor()` extended to map the newly-real provisioning states to wizard
  stages, so resuming after a process death reflects actual progress
  instead of falling back to `OCI_CONTEXT_DISCOVERY` for everything past
  auth.

## One design bug caught and fixed mid-implementation

`OciProvisioningContext.availabilityDomains` (Phase C) was a list with no
corresponding `selectedAvailabilityDomain` field — `OciProvisioner` read
`.firstOrNull()` off that list as an implicit "the selection is whichever
one is first." My first draft of `selectAvailabilityDomain()` worked around
this by reordering the discovered list to put the selected AD first, which
would have worked but is fragile and non-obvious to a future reader. Fixed
at the source instead: added a proper `selectedAvailabilityDomain: String?`
field to `OciProvisioningContext` and updated `OciProvisioner` to read that
directly. Small, clean, and the kind of thing worth fixing immediately
rather than compounding — flagged in case a similar list-position-as-
selection pattern shows up elsewhere.

## Design decisions worth flagging

1. **Compartment defaults to tenancy root, but visibly.** `discoverContext()`
   pre-fills `selectedCompartmentOcid` with the tenancy OCID if nothing's
   selected yet — but as a value the user can see in UI state and override
   via `SelectCompartment`, not a silent internal default. Consistent with
   the standing rule from Phase A's `OciAccount` kdoc against silent
   tenancy-root defaulting.
2. **`startProvisioning()`'s SSH-key resume path.** If the wizard is
   resumed in a new process (session persisted, in-memory UI state gone),
   `vmSshPublicKey` won't be in `OciOnboardingUiState` even though the key
   itself still exists. Falls back to
   `session.sshKeyAlias?.toLongOrNull()?.let { keyRepository.getKeyById(it)?.publicKey }`
   — reads the Room row id Phase A's `OciSshKeyProvisioner` stored there.

## Still not built

SSH verification (§25) and host registration (§26). The wizard now reaches
`OciOnboardingStage.SSH_VERIFICATION` after a successful provision, but
nothing there confirms the instance is actually SSH-reachable, and nothing
calls `OciShellHost.toConnectionProfile()` to register it in
`IHostRepository`. That's the next phase — the last one before this
package is usable end to end.

No Compose UI exists for any wizard stage — everything through this phase
is ViewModel/state layer only.

## Verification

Structural only (39 files, whole `oci/` package brace/paren-balanced).
Cross-referenced `OciProvisioner.provision()`'s parameter list against the
new call site by hand (named args + trailing lambda, defaults skipped for
`flexOcpus`/`flexMemoryInGBs`). One doc-only bug caught on re-read: a kdoc
comment referenced `OciProvisioningUiState` (doesn't exist) instead of the
actual `OciOnboardingUiState` — fixed before this report was written, not
left in.
