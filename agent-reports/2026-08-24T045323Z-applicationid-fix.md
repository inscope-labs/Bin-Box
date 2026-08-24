# Agent Report — applicationId correction

**Timestamp (UTC):** 2026-08-24T045323Z
**Agent:** Claude (architect/auditor)
**Scope:** app/build.gradle.kts only

## What changed
`applicationId` corrected from `com.aistudio.binbox.tmvrt` (leftover AI Studio
scaffold default) to `com.inscopelabs.abx.binbox`, matching the Kotlin
`namespace` exactly.

## Why
Reconnaissance ahead of Phase 3/5/9 work found the repo's actual applicationId
did not match either the addendum's expected value or either plan document's
stated value. Decision from John: applicationId must equal namespace exactly,
no hyphen. This supersedes the hyphenated `com.inscopelabs.abx.bin-box`
applicationId written in upgrade-plan.md and oci-provision.md — those docs
should be updated to reflect this (tracked as an open item, not yet done).

## Files touched
- `app/build.gradle.kts` (protected path — edited directly, not via AI Studio push)

## Verification performed
- Searched repo for other references to the old applicationId or the
  hyphenated form — none found outside the plan documents themselves.

## Not done in this change
- Plan documents (upgrade-plan.md, oci-provision.md) still show the
  hyphenated applicationId — text-only, does not affect the build.
- No functional code changed — this was a config correction only, ahead of
  starting Phase 3 (ITransport abstraction).
