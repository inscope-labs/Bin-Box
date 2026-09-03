# Agent Report — RTX-100 Phase 1 (BusyBox Core, Base Tier)

**Timestamp:** 2026-09-03T035523Z
**Task:** RTX-100 Phase 1 — source acquisition/pin, Android build config,
multi-ABI build wiring, base applet set, for the Base tier.

## What was actually changed
See commit `5a1e6ba`. Summary in the commit message; full detail in
`native/busybox/VERSION_PIN.md` and `native/busybox/APPLET_SET.md`.

## Commands run
- `git ls-remote --tags https://github.com/mirror/busybox.git` — to find
  the actual highest available tag (1_36_1; 1_37_0 does not exist on
  this mirror).
- `curl`/`sha256sum` — fetched and hash-recorded the pinned source
  tarball.
- `make allnoconfig`, then targeted `sed` toggles per applet, then
  `yes "" | make oldconfig` — resolved a dependency-consistent Base
  config through BusyBox's real Kconfig machinery rather than
  hand-writing a .config from documentation.
- `make -j$(nproc)` (host x86_64/glibc) and `make install` — sanity
  build + install-tree inspection, to verify the resolved config
  actually produces the intended 14 applet names before trusting it
  into a CI cross-compile job.

## Assumptions made
- NDK r27d (`ndkVersion 27.3.13750724`) — current LTS per
  `android/ndk` wiki at time of writing, supported until r30 ships.
  Not verified against this specific project's existing NDK pin (none
  found in `app/build.gradle.kts` — the project doesn't currently pin
  an NDK version anywhere, since no native code existed before this).
- minSdk 24 used for the per-ABI clang target
  (`aarch64-linux-android24-clang` etc.), matching `app/build.gradle.kts`'s
  existing `minSdk = 24`.
- CI job placed in its own workflow file
  (`build-busybox-base.yml`) rather than extending
  `build-apk-debug.yml`, since it has a different trigger cadence (only
  needs re-running when the BusyBox pin/config changes, not on every app
  build) and a different toolchain (NDK vs plain JDK/Gradle).

## Errors / things not verified
- **The CI cross-compile has not been run.** Everything BusyBox-specific
  was verified on the host architecture (x86_64/glibc) in the pipeline
  that prepared this, because that pipeline has no Android NDK and no
  path to one within its network allowlist. The workflow itself
  (`build-busybox-base.yml`) is what will actually produce and verify
  arm64-v8a/x86_64 binaries — it needs a manual `workflow_dispatch` run
  before Phase 1 can be considered actually done, not just prepared.
- **Source pin is 1.36.1, not upstream's current 1.37.0** — see
  `VERSION_PIN.md` for why (mirror hasn't synced past 1_36_1; no other
  reachable source). Re-pin is a reasonable follow-up if John has direct
  busybox.net access outside this pipeline.
- armeabi-v7a/x86 (the "if tested" half of 1.3) were not attempted -
  plan explicitly scoped Base-tier CI to arm64-v8a/x86_64 first.

PRIOR LOGGING GAPS FOUND: none.
