# OCI provisioning: stuck progress screen, step-2 dead end, session resume

**Commit**: b96170f (pushed directly to `main`, per explicit instruction)

## Trigger

User report: provisioning reaches step 7 after repeated tries, then displays
"Starting…" indefinitely with no progress, no visible status, and no way to
tell what's finished vs. remaining. No resume across app restarts — always
starts over. The back button gets stuck at step 2 because the regular
continue button isn't shown there. Also asked whether propagation delay
relates to a previously-seen "user not found" error.

## Root causes (found by tracing the wizard code, not guessed)

1. **Stuck "Starting…" at step 7**: `OciOnboardingViewModel.generateVmSshKey()`
   advanced the stage to `NETWORK_PROVISIONING` on success but never called
   `startProvisioning()`. `ProvisioningProgressStage` only renders
   `LoadingRow("Starting…")` when `provisioningState == null && error == null`
   — exactly the state landed in, with no button and no auto-trigger. The
   actual `OciProvisioner.provision()` call (the only thing that emits
   progress) was simply never invoked.

2. **Back button stuck at step 2**: `ApiKeyGenerationStage` only rendered a
   button in the `publicKeyPem == null` branch. Once a key exists (true on
   any return visit), the stage showed static text claiming an "automatic"
   advance that only ever fired as a side effect of the original generate
   click — a genuine dead end on back-navigation.

3. **No resume**: `generateVmSshKey()` never called `persistSessionState(...)`,
   so the persisted session state never advanced past `CONTEXT_DISCOVERED`
   even after the UI visibly reached step 7. Separately, `init` only restored
   credentials into `uiState` on relaunch — never `context` (selected
   compartment/AD/shape/image) or the discovered lists — so even the partial
   resume that did happen dropped the user back near step 5-6 with every
   host-config selection wiped.

4. **Propagation / "user not found"**: confirmed this was already diagnosed
   earlier today in `agent-reports/2026-08-27T171500Z-oci-idcs-conversion-error-analysis.md`
   — IAM Identity Domains can take 30-60s to propagate a new API key to
   regional endpoints. Told the user this is real but not the only cause
   (wrong identity-domain upload, missing IAM policy produce the same
   generic error) and pointed at that report rather than re-deriving it.

## Fixes

- `generateVmSshKey()` now persists `SSH_KEY_READY` and immediately calls
  `startProvisioning()`.
- `ApiKeyGenerationStage` gets a manual "Continue" button in the
  already-generated-key branch, wired to a new `ContinueToKeyRegistration`
  event.
- New resume flow: on launch, a persisted non-terminal, non-`NOT_STARTED`
  session surfaces a Resume/Start Over `AlertDialog` instead of silently
  jumping in (previous behavior) or being dropped (the practical effect
  before this fix, since nothing restored `context`). Resuming (new
  `OciResumeHandler` module):
  - restores the VM SSH public key from `IKeyRepository` via the session's
    `sshKeyAlias`
  - re-fetches compartments/ADs/shapes/images live (not persisted — only
    OCIDs are) while preserving the session's existing selections
  - re-invokes `startProvisioning()` or `registerHost()` if either was left
    mid-flight, which is safe: `NetworkProvisioner` discovers-before-creating
    and `ComputeProvisioner.launchAndWait` uses a session-stable
    `opcRetryToken`, so re-running never creates duplicate infrastructure
- Selection-change invalidation (new `OciProvisioningInvalidation` module,
  used by `OciHostConfigSelectionHandler` and directly on account-info
  resubmission): changing compartment/AD/shape/image, or editing account
  info, after provisioning may already have started now clears the stale
  downstream progress (`provisioningState`, `provisionedPublicIp`, and the
  session's VCN/instance/public-IP fields) rather than silently continuing
  under the old selection.
- Back navigation through the full wizard now round-trips without dead ends.

## File-size note

`OciOnboardingViewModel.kt` had a same-day resolved FILE-SIZE issue
(restructured from 530L down, per `agent-reports/2026-08-27T140000Z-oci-wizard-assisted-navigation-diagnostics.md`).
This fix reopened it: 368L after this pass. Extracted three new modules in
this pass to limit the growth (`OciResumeHandler`, `OciHostConfigSelectionHandler`,
`OciProvisioningInvalidation` — file would otherwise be ~460L+). A further
extraction of the VM-SSH-key → provisioning → host-registration chain into
its own module was attempted and reverted: it required re-deriving the exact
`advanceTo(SSH_VERIFICATION)` / `persistSessionState(SHELL_READY)` sequencing
and risked a subtle regression under time pressure. Correctness was
prioritized over hitting the line-count target for this push. Filed a new
FILE-SIZE issue in `issues/pending/` documenting this and recommending the
follow-up extraction with the sequencing constraint called out explicitly.

## Testing

Added three regression tests to `OciOnboardingViewModelTest.kt`: the
Continue-button event wiring, the resume prompt appearing for a persisted
in-progress session, and Start-Over correctly discarding it. Not run against
a full Gradle build — no Android/Gradle toolchain in this sandbox; verified
structurally (types, imports, call signatures) against the existing code
paths instead. Recommend running `build-apk-debug.yml` to confirm.

## Deferred (not part of this fix, flagged only)

- The requested "on successful provisioning, existing OCI entry points
  (FAB/promo card/quick-action tile) should route to a management endpoint
  instead of onboarding" is a separate, larger change — not started, per
  explicit instruction to finish this fix first.
