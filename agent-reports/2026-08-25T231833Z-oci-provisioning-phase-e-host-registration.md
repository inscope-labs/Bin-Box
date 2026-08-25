# OCI Provisioning Package — Phase E (host registration)

**Agent:** Claude
**Date:** 2026-08-25
**Depends on:** Phases A/B/C/D

## What this covers

§26 — the last piece needed for the OCI provisioning package to produce a
connectable Bin-Box host, not just a running OCI instance.

- `OciHostRegistrar.kt` — thin wrapper around the existing `IHostRepository`.
  No new registration mechanism: calls `OciShellHost.toConnectionProfile()`
  (already built in Phase A) and persists it, consistent with that class's
  kdoc on reusing the existing terminal stack rather than duplicating it.
- `defaultSshUsernameFor(operatingSystem: String)` — confirmed against
  Oracle's own connection docs this session, not guessed: Oracle
  Linux/RHEL-compatible and CentOS images default to `opc`; Ubuntu images
  default to `ubuntu`. Falls back to `opc` (Oracle's own default image
  family) for anything unrecognized rather than guessing `root`.
- `OciOnboardingViewModel.registerHost()` — runs automatically after a
  successful `startProvisioning()`, not as a separate wizard event. There's
  no user judgment call left at that point (unlike shape/image selection),
  so making it a manual step would just be friction.

## What this deliberately does NOT do

Does not implement §25 (SSH verification). `registerHost()` runs as soon as
the instance and its public IP exist, without confirming SSH is actually
reachable first. A host registered this way that isn't yet reachable will
simply fail to connect in the terminal, the same as any host with a bad
address — not silently broken, but not pre-verified either. This is a
real, acknowledged gap, not an oversight papered over — flagged plainly in
both the registration function's kdoc and the ViewModel's class kdoc.

## On "merge, then build" — a real constraint to flag before either happens

**Merge:** proceeding as asked — `oci-provisioning-phase-a` into `main`.

**Build:** I can't actually run a Gradle/Android build in this sandbox.
The network allowlist here covers npm/pip/crates/GitHub-adjacent domains
only — no `dl.google.com`, `maven.google.com`, or `services.gradle.org`,
which is where the Gradle distribution, Android Gradle Plugin, and every
AndroidX/Compose/Jetpack artifact this project depends on actually live.
There's no way to download what a build needs from here, so an "actual
build" attempt would fail at dependency resolution, not at a Kotlin
compile error — it wouldn't tell you anything about whether the OCI
package itself is correct.

The `.github/workflows/build-apk-debug.yml` workflow already in this repo
(with the `assets/launcher-icons` branch fix from earlier this session) is
the actual way to get a real build signal: it runs on GitHub's runners,
which do have access to Google's Maven and the Gradle distribution
servers. It's `workflow_dispatch`-only (manual trigger), so it needs to be
run from the GitHub Actions tab, or the trigger conditions changed to run
automatically. That's outside what I can do from this sandbox — it needs
to happen on your end, or by me being given a different environment with
broader network access.

Structural verification for this phase is at the same level as A-D: 40
files across the whole `oci/` package brace/paren-balanced, cross-file
references checked by hand.

## Summary of what's now complete vs. still open

Complete (structurally, pending a real build): account setup → API key →
fingerprint registration → connection verification → VM SSH key
generation → context discovery (compartments/ADs/shapes/images) →
selection → provisioning (network + capacity check + launch + poll +
public IP) → host registration. That's the full doc scope minus §25 (SSH
verification) and any Compose UI.

Open: §25 SSH verification, all UI screens, and — now the highest-priority
item — an actual compiler pass, since none of Phases A-E have been
verified by anything other than careful manual reading.
