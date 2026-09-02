# Hosted Local Shell — Addendum: Agent Role Boundaries

**Status:** Draft, not yet committed. Covers RTX-100 stages R0.1 and R0.2.

## 1. Purpose

`Hosted_Local_Shell.md` (v2) assumes a fixed split between what the AI Studio
build agent does and what Claude/tooling does. That split was agreed but
never written into the repo, so AI Studio had no enforceable boundary — the
Sept 1, 2026 incident (a "core vs. beta" task that deleted ~130 files,
including the CI workflow and Gradle wrapper, instead of feature-gating them)
happened partly because there was no committed file telling the agent those
paths were off-limits. This document is that file.

## 2. Role Split

- **AI Studio** — Android-build-system work inside its own Gradle/Kotlin/
  NDK-via-Gradle sandbox: Compose UI, ViewModels, module business logic,
  tests, resource files, anything it can build and verify itself in that
  environment.
- **Claude / tooling** — anything requiring external fetch (binary sourcing,
  dependency/license/hash verification), CI orchestration, or an edit to a
  **Protected Path** (below).

AI Studio must **defer**, not circumvent or fabricate, any task that falls
outside its own sandbox — e.g. it must not hand-author binary manifests,
invent hash values it cannot actually compute, or write CI YAML itself.
Deferring means stopping and flagging the task (per the existing
`issues/pending/` pattern in AGENTS.md §1.1), not attempting a workaround.

## 3. Protected Paths

AI Studio must never edit these, even incidentally as part of a larger
task's scope. A task that appears to require touching one of these is out
of AI Studio's scope entirely and must be flagged, not attempted:

- `.github/workflows/*.yml`
- `proguard-rules.pro`
- `AndroidManifest.xml`
- `app/build.gradle.kts`
- `version.properties`
- `build-logs/**`
- `settings.gradle.kts`
- `build.gradle.kts` (root)
- `gradle/wrapper/**`

The first six were the original list; the last three (`settings.gradle.kts`,
root `build.gradle.kts`, `gradle/wrapper/**`) are added here per RTX-100
stage R0.2, ahead of Phase 0/1 module work that will touch them.

## 4. Enforcement

This file must be linked from `AGENTS.md` so the boundary is visible to the
agent at the start of every task, not just documented somewhere it won't be
read. See the proposed `AGENTS.md` §5 addition delivered alongside this file.

## 5. Open item

This addendum does not attempt to re-litigate `AGENTS.md` §4.1 (file-size
thresholds) — that was already revised 2026-09-01 to a 500-line
(logic)/1000-line (UI) role split, separately from this role-boundary work.
