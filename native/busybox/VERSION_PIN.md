# BusyBox Source Pin — RTX-100 Phase 1.1

**Status:** Verified against a live checkout in the environment that
prepared this pin (source downloaded, extracted, and actually built —
see §Verification below). Not yet cross-compiled for Android (that's
1.3, CI-only, no toolchain available where this pin was prepared).

## Pinned source

- **Upstream project:** BusyBox (busybox.net), GPL-2.0-only.
- **Mirror used:** `https://github.com/mirror/busybox` — a git mirror of
  the upstream `git.busybox.net/busybox` repo. Used because
  `busybox.net`/`git.busybox.net` are not reachable from this pipeline's
  network allowlist; GitHub is.
- **Tag:** `1_36_1` (BusyBox 1.36.1, released 2023-05-19)
- **Commit:** `1a64f6a20aaf6ea4dbba68bbfa8cc1ab7e5c57c4`
- **Tarball SHA-256** (of the GitHub codeload snapshot at that commit —
  this is *not* the same bytes as busybox.net's own release tarball,
  since GitHub repacks the archive; it pins the mirror snapshot, not an
  upstream-published artifact):
  `076a7449075feea57afc2750b10a9ff5e95e93f782ad454404fedd10a27c2933`

## Known gap — not 1.37.0

Upstream's actual latest stable release is **1.37.0** (Oct 2024, first
feature release in 18 months — Y2038 fixes, `getfattr`, Ash/Awk
improvements). The `mirror/busybox` GitHub mirror has **not been synced
past `1_36_1`** — no `1_37_0` tag exists there as of this pin (verified
via `git ls-remote --tags`). No other 1.37.0 source was reachable from
this pipeline's allowlist (no `busybox.net`/`git.busybox.net` access).

**This pin is 1.36.1, not 1.37.0, as a result.** If John has access to
busybox.net directly (outside this pipeline), re-pinning to 1.37.0 and
replacing this file is a reasonable follow-up — 1.36.1 vs 1.37.0 changes
don't touch any of the Base-tier applets below, so nothing here should
need to change functionally, just the source/hash references.

## Verification performed

- Tarball downloaded and extracted from the exact pinned commit.
- Applet Kconfig symbols for all 14 Base-tier applets confirmed by
  grepping `//config:` blocks in the actual applet source files (not
  assumed from documentation) — see `APPLET_SET.md`.
- `base.config` (Phase 1.2) resolved via BusyBox's own `make oldconfig`
  against this exact source tree — dependency-consistent, not
  hand-written.
- Full **host-architecture** build (`make`, x86_64, glibc) completed
  successfully with this exact config: `busybox_unstripped` linked with
  zero errors, stripped binary is a valid PIE ELF executable
  (`ELF 64-bit LSB pie executable, x86-64, ..., dynamically linked`),
  88176 bytes.
- `make install` against this config produces exactly the 14 expected
  applet names as symlinks to one binary — see `APPLET_SET.md` for the
  full list and one path-layout quirk worth knowing about.

**Not yet done:** actual Android/NDK cross-compilation (arm64-v8a,
x86_64). That's Phase 1.3, and needs to run somewhere with the NDK —
this pipeline doesn't have one. `.github/workflows/build-busybox-base.yml`
(committed alongside this) does that in CI.
