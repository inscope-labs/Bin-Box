# RTX-100 Phase 2.1b — Standard-Delta BusyBox Kconfig + Workflow

**UTC:** 2026-09-04T103127Z
**Branch:** rtx100-phase2-1b-standard-delta (off main @ $(git log -1 --format=%h origin/main))

## What this batch does
Resolves the "22 of Standard's 26 tools are already BusyBox applets"
finding into real, verified artifacts:
- `native/busybox/configs/standard-delta.config`: Kconfig built from
  `allnoconfig` (matching Phase 1's own convention, not hand-edited),
  containing exactly the 22 additional applets and explicitly excluding
  all 13 Base applets - including the non-obvious `SH_IS_ASH`/
  `SHELL_ASH` defaults that otherwise silently reintroduce `sh`.
- `native/busybox/STANDARD_DELTA_APPLET_SET.md`: full findings,
  including the SH_IS_ASH gotcha and what's still not done.
- `.github/workflows/build-busybox-standard-delta.yml`: manual-dispatch
  cross-compile workflow, cloned from `build-busybox-base.yml`'s proven
  pattern, targeting the same 3 ABIs. Includes a config-drift guard that
  fails the build if any Base-tier symbol is unexpectedly on, or any
  target Standard-delta symbol is off.

## Verified locally (host build, x86_64/glibc)
- Config resolves cleanly via `make oldconfig`, no unresolved prompts.
- Build succeeds, zero errors.
- Dependency closure: crypt/m/rt all excluded as unneeded - same result
  as Base, no extra shared libraries.
- `make install` produces exactly 22 applet symlinks, confirmed zero
  overlap with Base (no `sh`/`ash` present after the SH_IS_NONE fix).

## NOT done in this batch
- The workflow has NOT been triggered - no real per-ABI Android binaries
  exist yet from this config, only the host-arch sanity build (same
  staging as Phase 1 before `build-busybox-base.yml` was first run).
- No manifest work (`standard.json`) - deferred until real hashes exist,
  matching Base's own Phase 2.4/2.5 sequencing.
- No DFM placement (`shell_standard/jniLibs/`) - binary doesn't exist
  yet to place.

## Assumptions / unverified
- Reused Base's exact `BUSYBOX_TARBALL_SHA256` in the new workflow since
  it's the same pinned source commit - not independently re-verified in
  this batch (it was verified when Base's workflow was written).
- NDK version/MIN_SDK copied from Base's workflow for consistency: not
  re-justified here.
