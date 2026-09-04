# Fix (attempt 2): yes|make oldconfig still failing under set +o pipefail

**UTC:** 2026-09-04T113557Z

## What happened
The previous fix (commit `69bf83d`: wrap `yes "" | make oldconfig` with
`set +o pipefail`/`set -o pipefail`) did NOT resolve the issue in
practice. Re-triggered run `33866861740` failed identically to the
first (`33865139892`) - same step, same `yes: standard output: Broken
pipe` / `Process completed with exit code 1.`, confirmed via the
correct job ID for the new run (not a stale-log mixup - verified
timestamps matched the new run).

## Honest assessment
I do not have a fully confirmed root-cause explanation for why
`set +o pipefail` didn't neutralize this on the actual GitHub Actions
runner, despite reproducing the original failure AND confirming the fix
locally (bash -eo pipefail, matching what I believe is GH Actions'
default shell invocation). Possible explanations I have NOT verified:
different bash version/behavior on the runner, or some other environment
difference. Rather than keep guessing at pipefail semantics I can't
directly observe on the runner, switching to an approach that removes
the failure mode's precondition entirely.

## Fix (attempt 2)
Replace `yes "" | make oldconfig` with `make oldconfig < /dev/null` in
both `build-busybox-base.yml` and `build-busybox-standard-delta.yml`.
`conf`/oldconfig takes the default answer for any prompt on EOF, so
`/dev/null` stdin produces the identical resulting config with zero live
writer process - there is no `yes` process that can ever be SIGPIPE'd,
so this isn't a workaround for the failure mode, it removes the
precondition.

## Verified locally
- `make oldconfig < /dev/null` under `bash -eo pipefail`: exit 0.
- Resulting .config confirmed identical in every symbol that matters:
  all 22 Standard-delta applets on, all 13 Base applets + SH_IS_ASH/
  SHELL_ASH off, SH_IS_NONE on.

## Not yet done
- Not yet re-triggered on the actual CI runner - this is a second
  attempt, and given the first "verified locally" fix didn't hold on the
  real runner, this should be treated as unconfirmed until it actually
  succeeds there, not assumed correct from local testing alone.
