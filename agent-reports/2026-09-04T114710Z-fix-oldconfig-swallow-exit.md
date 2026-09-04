# Fix (attempt 3, confirmed): swallow oldconfig's own exit status

**UTC:** 2026-09-04T114710Z

## Root cause, now actually confirmed
Reproduced with a genuinely fresh build tree this time (earlier local
tests were likely reusing a stale `scripts/kconfig/conf` build artifact
from prior test iterations in the same directory - this run shows
`HOSTCC scripts/kconfig/conf.o` etc., a real from-scratch rebuild,
matching what CI does on every run). Full transcript confirms:
BusyBox's Kconfig `conf --oldconfig` genuinely returns exit 1 on EOF
even when every single prompt (hundreds of them, not just our 22 target
symbols) got a valid default answer - this is inherent tool behavior,
not a bug in our config or in either prior fix attempt.

## Fix
`make oldconfig < /dev/null || true` in both workflows - explicitly
stop trusting oldconfig's own exit status, since it does not correlate
with whether the resulting config is correct. The CONFIG_* verification
loop immediately after (which has passed in every attempt including
this one) is the real, independent correctness gate.

## Verified locally (this time genuinely fresh, not reused build state)
Full `make clean` -> fresh `.config` -> `make oldconfig < /dev/null ||
true` -> verification loop: `VERIFICATION LOOP PASSED`, overall exit 0.
Transcript matches the CI failure logs line-for-line up to the same
"System Logging Utilities" / Shells section, confirming this reproduces
the actual CI condition rather than a different local state.

## History of this bug (for anyone reading this later)
1. Attempt 1 (`69bf83d`): `set +o pipefail` around `yes | make
   oldconfig` - did not fix it (run 33866861740).
2. Attempt 2 (`71712fd`): switched to `make oldconfig < /dev/null` -
   did not fix it either (run 33868970366), same root cause (oldconfig's
   own EOF-triggered exit 1), pipe vs /dev/null was never the actual
   variable.
3. Attempt 3 (this commit): stopped trying to prevent oldconfig from
   exiting nonzero, instead stopped trusting that exit code at all.
