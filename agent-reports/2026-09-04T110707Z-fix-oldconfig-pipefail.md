# Fix: yes|make oldconfig pipefail trap in BusyBox CI workflows

**UTC:** 2026-09-04T110707Z

## What broke
`build-busybox-standard-delta.yml` run 33865139892 failed all 3 ABI
jobs at "Apply Standard-delta config" with:
```
yes: standard output: Broken pipe
Error: Process completed with exit code 1.
```
The oldconfig transcript shows every symbol (including SH_IS_NONE)
resolved correctly - this was not a config error. GitHub Actions runs
`run:` steps under `bash -eo pipefail` by default; `make oldconfig`
finishing before `yes` is done writing SIGPIPE-kills `yes`, and
pipefail treats that as pipeline failure even though `make oldconfig`
itself exited 0.

## Fix
Wrap the `yes "" | make oldconfig` line with `set +o pipefail` /
`set -o pipefail` in both `build-busybox-base.yml` and
`build-busybox-standard-delta.yml`. The real success gate in both
workflows is the CONFIG_* verification loop immediately after, which is
unaffected by this change.

## Why both workflows, not just the one that failed
`build-busybox-base.yml` has the identical pattern and has not hit this
- Phase 1's run (33753045572) succeeded - but that appears to be timing
luck (a shorter/different oldconfig question sequence happening not to
trigger the SIGPIPE), not a structural difference. Left unfixed, a
future re-run of Base's workflow could hit the same false failure.
Fixing both now rather than waiting for it to recur.

## Verified
Reproduced the exact failure mode locally under `bash -eo pipefail`
(matching GH Actions' default shell), confirmed exit 141/broken-pipe
without the fix and exit 0 with it, using the same busybox-src tree and
config from the Phase 2.1b work.

## Not done
- Fixed workflow not yet re-triggered - pending this patch landing.
