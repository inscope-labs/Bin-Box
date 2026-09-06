# INC-002 — AI Studio Second Codebase Deletion (Sept 6, 2026)

## Summary

AI Studio deleted the entire Bin-Box project codebase for the second time.
The extent of the deletion is not yet fully characterised (discovered during
the minSdk bump task). Recovery path and root-cause analysis are pending.

## Timeline

| Time (approx) | Event |
|---|---|
| Sept 6, 2026 | John reports AI Studio has deleted the entire Studio codebase |
| Sept 6, 2026 | Claude assumes protective ownership of all protected-path changes pending Studio re-setup |

## Prior incident

**INC-001 (Sept 1, 2026):** AI Studio deleted ~130 files instead of
feature-gating them. Root cause: the protected-path role document
(`AGENTS.md §5` + `Hosted_Local_Shell-Addendum-Roles.md`) had been drafted
but never committed to the repo, so AI Studio had no enforceable restriction
in context. Resolved via `git force-revert` to `df52cbf`, followed by a
6-patch series to re-apply the 5 legitimate commits plus the gating scaffold.

## Current status

- `origin/main` is intact at `c2ca937` — the deletion was local to AI
  Studio's project environment, not to the remote repo.
- Claude is operating in direct-commit mode (PAT-authenticated) until
  John re-establishes the Studio project.
- All protected paths (`.github/workflows/*.yml`, `app/build.gradle.kts`,
  `version.properties`, `proguard-rules.pro`, `AndroidManifest.xml`,
  `build-logs/**`) are being managed by Claude for the duration.

## Immediate actions taken (this session)

- `app/build.gradle.kts`: `minSdk` bumped 24 → 28 (planned change,
  required for RTX-100 native binary compatibility with NDK r27's
  `__INTRODUCED_IN()` enforcement, agreed with John before Studio deletion).
- `version.properties`: versionCode 36 → 37, debugCode 0036 → 0037.

## Root cause (preliminary)

Unknown. AI Studio's exact trigger for the deletion has not been
investigated yet. Two possible patterns from INC-001 still apply:
1. Agent issued a broad delete/restructure command without scope limits.
2. A new Studio session was started without the AGENTS.md governance doc
   in context, leaving the agent without protected-path enforcement.

**Hypothesis:** A new Studio project setup was initiated and the agent
interpreted "set up the project" as a clean-slate operation, deleting
existing files. This is consistent with INC-001's root cause pattern
(no in-context enforcement = agent acts unilaterally on scope it shouldn't
own).

## Recommended prevention for re-setup

When re-establishing the Studio project:
1. Commit AGENTS.md §5 changes (still pending from the `AGENTS-section5-
   protected-paths-DRAFT.md` file — this was never formally patched in).
2. Ensure `AGENTS.md` is the **first** document AI Studio reads on any new
   session — not just in the repo, but explicitly referenced in the Studio
   project's system prompt / first-turn context.
3. Scope the Studio project to `app/src/main/java/` and
   `app/src/test/java/` only — not the repo root — so a destructive
   operation cannot touch workflows, Gradle files, or native build assets.
4. Do not grant Studio write access to `.github/`, `native/`, `version.properties`,
   or `app/build.gradle.kts` in the new project setup.

## Open items

- [ ] Determine exact scope of deletion (which files are missing).
- [ ] Formally commit AGENTS.md §5 to close the governance gap that
      enabled both INC-001 and likely INC-002.
- [ ] Re-establish AI Studio project with narrowed file-scope and
      AGENTS.md as mandatory first-context.
