# Bash Source Pin (RTX-100 Standard Tier)

## Source

- **Upstream mirror:** [`tianon/mirror-bash`](https://github.com/tianon/mirror-bash) — the actively-synced GitHub
  mirror of GNU Bash's real upstream (`git.savannah.gnu.org/cgit/bash.git`), maintained by Docker's
  official-images maintainer. Chosen because GNU's own distribution (`ftp.gnu.org`/mirrors) is not reachable
  from the build sandbox's network allowlist, the same constraint that led Phase 1 to pin BusyBox from
  `github.com/mirror/busybox` instead of upstream.
- **Pinned commit:** `b8c60bc9ca365f8261fa97900b6fa939f6ebc303` (tag `bash-5.3`, current stable release —
  not a beta/rc). Pinned by commit, not tag, since tags can move.
- **License:** GPL-3.0-or-later (note: GPLv3, not GPLv2 like BusyBox — separate license-compliance entry
  needed on the eventual Open Source Licenses screen, Phase 10.6).

## Integrity verification — tree-content hash, NOT tarball hash

**Important finding (Sept 5, 2026):** GitHub's `codeload.github.com` tarball generation for a given commit
is **not** guaranteed byte-identical across separate requests — two independent fetches of the exact same
commit produced different SHA-256 tarball hashes despite identical file counts and total size, almost
certainly due to gzip container metadata (timestamps) differing between on-the-fly archive generations.
Pinning by raw tarball SHA-256 (the pattern used for `build-busybox-base.yml`/`build-busybox-standard-delta-r1.yml`)
is therefore unreliable for long-term reproducibility, even though it happened to hold up across those
earlier builds.

The fix used here instead: hash the **extracted file contents**, not the compressed container, in a
format-independent way (rebuilding the per-file hash lines manually rather than trusting either GNU
coreutils' or a BusyBox/toybox `sha256sum`'s native two-column output format, which can also differ).

```
EXPECTED_TREE_SHA256 = 674ddae6484ddf8d3311fc6bb02b5ee866e80ee2e24590f2a79a4bc2e5f6cd4a
```

Verification method (must match exactly — path-prefix or format differences change the result even with
identical file content):

```bash
cd <extracted-source-root>
find . -type f | sort | while IFS= read -r f; do
  h=$(sha256sum "$f" | awk '{print $1}')
  printf '%s  %s\n' "$h" "$f"
done | sha256sum | awk '{print $1}'
```

## Cross-compile configuration

Uses `configure --cache-file=cross-build/android.cache` — bash's own native mechanism for pre-seeding
`ac_cv_*`/`bash_cv_*` autoconf cache variables that can't be probed by executing target-arch test binaries
during a cross-compile (the same problem BusyBox's Kconfig `oldconfig` step ran into, just autotools'
version of it). Five pre-baked cache files already ship in `cross-build/` for other cross targets
(`cygwin32`, `msys32`, `opennt`, `qnx`, `x86-beos`) — `android.cache` here follows that same established
format, added as a new sibling.

Seed values are adapted from Termux's `packages/bash/build.sh` (`termux/termux-packages`, public repo,
GPL-licensed build recipe) — used as **reference for known Android/Bionic cross-compile answers only**,
not as a source of any binary or compiled artifact. Every value here still gets fresh-compiled from the
pinned commit above.

**Scope decisions for this first build:**
- `--enable-multibyte` **NOT** used — would require Bionic's missing `iconv()`, which means pulling in
  `libiconv` as a new cross-compiled dependency. Deferred; no locale-aware multibyte input in this pass.
- Bash's own **bundled** `lib/readline` is used (not `--with-installed-readline`) — gives real line-editing/
  history without needing a separately cross-compiled `libreadline`.
- `--without-bash-malloc` — bash's own malloc implementation has a history of misbehaving on non-glibc
  targets; deferring to Bionic's malloc is the safer default (same reasoning Termux uses).
- `--disable-nls` — no gettext/translation support needed.

## Known risk

This cache file is a **best-effort first draft**, built from documented Android cross-compile answers but
not yet CI-verified. BusyBox's own Kconfig config took 3 CI round-trips to get right (see
`native/busybox/VERSION_PIN.md` history) despite being conceptually simpler than autotools' `ac_cv_*`
surface. Expect at least one iteration here too — the runlog-push step in `build-bash.yml` exists
specifically so a `configure` failure's exact missing/wrong cache variable is visible without needing
another blocked-artifact-download round trip.
