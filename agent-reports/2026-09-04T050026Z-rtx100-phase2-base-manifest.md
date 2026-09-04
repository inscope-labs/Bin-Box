# RTX-100 Phase 2 (Base Tier) — Manifest Finalization

**UTC:** 2026-09-04T050026Z
**Branch:** rtx100-phase2-base-manifest (off main @ 9ab6e25)

## What changed
- Rewrote `app/src/main/assets/shell/manifests/base.json` to the extended
  schema (see `RTX-100_Phase2_Base_Manifest_DRAFT.md` delivered to John
  for full rationale). Replaced the old fallback-oriented draft
  (`toybox`/`toolbox`/`echo` with `fallbackSystemPath`) entirely — no
  fallbacks, per explicit instruction.
- New `sharedBinaries[]` array: single entry `busybox` ->
  `libbusybox.so`, with real per-ABI SHA-256 hashes computed by John
  from the actual `build-busybox-base.yml` run 33753045572 artifacts
  (arm64-v8a, x86_64, armeabi-v7a).
- `binaries[]`: all 14 Base-tier applets (`sh ash ls cp mv rm mkdir cat
  chmod ps kill env which uname`), each referencing the shared
  `busybox` binary via `sharedBinary`.

## What did NOT change (and why)
- No `app/build.gradle.kts` edit. Checked for existing
  `jniLibs`/`packagingOptions`/`sourceSets`/`abiFilters`/`ndk{}`
  blocks — none exist, so AGP's default behavior (auto-package anything
  under `src/main/jniLibs/<abi>/*.so`) applies with no config needed.
- **The actual `libbusybox.so` binaries are NOT in this patch.** I only
  ever had the SHA-256 hashes (computed by John locally from the
  artifacts) — never the binary bytes themselves (artifact download
  redirects to Azure blob storage, unreachable from this sandbox). This
  patch is text-only.

## Still required (manual, by John — not part of this patch)
Rename and place each ABI's `busybox` binary as `libbusybox.so` under:
```
app/src/main/jniLibs/arm64-v8a/libbusybox.so
app/src/main/jniLibs/x86_64/libbusybox.so
app/src/main/jniLibs/armeabi-v7a/libbusybox.so
```
from the files already unzipped in `~/arm64-v8a/busybox`,
`~/x86_64/busybox`, `~/armeabi-v7a/busybox`. Commit those three binary
files directly (not via this patch). Recommend re-running `sha256sum`
against the placed files right before committing, as a sanity check that
nothing got corrupted in the copy/rename.

## Unverified / assumptions
- Assumed `0755` executable permission bit should be set on the placed
  `.so` file — standard for AGP-packaged native libs, not independently
  confirmed against this project's specific packaging behavior.
- The "Open Source Licenses" screen (GPL-2.0 compliance, flagged in the
  manifest draft §2) is NOT part of this patch — separate scope,
  presumed Studio-owned UI work, not yet filed as an explicit task.
