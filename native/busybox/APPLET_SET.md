# BusyBox Base Applet Set — RTX-100 Phase 1.4

Verified against `configs/base.config` on the pinned source
(`VERSION_PIN.md`) via an actual host-architecture build + `make install`
pass — this is the observed result, not a reading of documentation.

## Target applet set (per RTX-100 plan, Phase 1.4)

`sh ash ls cp mv rm mkdir cat chmod ps kill env which uname` — 14 names.

## Kconfig symbols enabled (on top of `allnoconfig`)

| Applet(s) | CONFIG symbol | Note |
|---|---|---|
| `sh` | `CONFIG_SH_IS_ASH` | Already the default even under `allnoconfig` (Kconfig `choice` blocks can't leave all options unset) — the fallback is `sh → ash` for free. |
| `ash` | `CONFIG_ASH` | Must be set explicitly for the `ash` *name* itself to exist as a symlink — the Config.src comment notes it can't be `select`ed from the choice above without breaking `allnoconfig`. |
| `ls` | `CONFIG_LS` | |
| `cp` | `CONFIG_CP` | |
| `mv` | `CONFIG_MV` | |
| `rm` | `CONFIG_RM` | |
| `mkdir` | `CONFIG_MKDIR` | |
| `cat` | `CONFIG_CAT` | |
| `chmod` | `CONFIG_CHMOD` | |
| `ps` | `CONFIG_PS` | |
| `kill` | `CONFIG_KILL` | |
| `env` | `CONFIG_ENV` | |
| `which` | `CONFIG_WHICH` | |
| `uname` | `CONFIG_UNAME` | |

Plus `CONFIG_PIE=y` (Phase 1.2, see below). All 15 toggles survived
`make oldconfig` dependency resolution unchanged — no conflicts.

## Verified install-time result

`make CONFIG_PREFIX=<out> install` against this config produces exactly:

```
<out>/bin/ash      <out>/bin/cp    <out>/bin/ls   <out>/bin/rm     <out>/bin/sh
<out>/bin/busybox   <out>/bin/kill  <out>/bin/mkdir <out>/bin/uname
<out>/bin/cat       <out>/bin/mv    <out>/bin/ps    <out>/bin/chmod
<out>/usr/bin/env
<out>/usr/bin/which
```

All 14 requested names are present — **but 12 land under `bin/` and 2
(`env`, `which`) land under `usr/bin/`.** This is BusyBox's own baked-in
per-applet install directory (`BB_DIR_USR_BIN` vs `BB_DIR_BIN` in
`include/applets.h`), not a config mistake. Android has no FHS-style
`/usr/bin` — whatever packages this into `jniLibs`/the manifest (Phase
2.5/2.6) needs to either flatten both into one directory at packaging
time or have `TierManifest`/`BinaryRegistry` (Phase 2.6, Studio-owned)
aware that `env` and `which` need the same one-binary-many-names
treatment as everything else, just sourced from a different install
subpath. Flagging this now so it isn't rediscovered as a bug later.

## Size (host x86_64 sanity build, not the Android target)

`busybox` stripped: 88176 bytes. Unstripped: 111832 bytes. Final link
required no extra libraries beyond libc (`crypt`, `m`, `rt` all
auto-excluded as unneeded) — consistent with "size-optimized."

## STATIC vs PIE — resolved in favor of PIE, not both

BusyBox's own `Config.in` makes `CONFIG_PIE` and `CONFIG_STATIC`
mutually exclusive (`config PIE ... depends on !STATIC`). The plan asks
for "PIE, size-optimized" — this config sets `CONFIG_PIE=y` and leaves
`CONFIG_STATIC` unset (dynamically linked against libc/bionic). That's
also the Android-idiomatic choice: NDK-built executables are normally
dynamically linked against bionic, and Android has required PIE
executables since API 21 — a genuinely static+PIE combination isn't
something BusyBox's build system exposes without bypassing this check,
and there was no stated reason in the plan to need it (Base tier ships
as a Play Feature Delivery module, not a standalone downloaded binary,
so bionic availability isn't in question the way libc availability would
be on an unknown Linux host).
