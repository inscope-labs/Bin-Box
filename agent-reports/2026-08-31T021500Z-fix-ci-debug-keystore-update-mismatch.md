# Process Report: Fix Debug APK Update Certificate Mismatch in CI

- **Date / Timestamp**: 2026-08-31T02:15:00Z
- **Task**: Scan Gradle and CI build configs to identify root cause of Android debug APK in-place update failures, research the signature mechanism, and implement the permanent fix.

## Prior Logging Gaps Found
- `PRIOR LOGGING GAPS FOUND: none`

## Compliance Check (>180L)
- No Kotlin files touched or modified in this task.

## Root Cause Analysis
- **Android Package Signature Verification Rule**: Android requires that any updated APK installed over an existing package with the same `applicationId` (`com.inscopelabs.abx.binbox`) MUST be signed with the exact same cryptographic signing key/certificate. If the certificate changes between builds, Android throws `INSTALL_FAILED_UPDATE_INCOMPATIBLE` ("App not installed: Conflicting package with existing signature").
- **CI Keystore Regeneration Flaw**:
  - In `.github/workflows/build-apk-debug.yml`, the runner generated a new `debug.keystore` with `keytool -genkeypair` on every build whenever `debug.keystore` was missing on the ephemeral runner.
  - Because `debug.keystore` is gitignored, each CI run minted a completely distinct RSA keypair and certificate.
  - The repository already contained a canonical `debug.keystore.base64` file, but the workflow was never decoding it.
- **Impact on User Data**:
  - The signature mismatch forced the user to uninstall the existing app before installing the new build.
  - Uninstalling an Android app erases `/data/data/com.inscopelabs.abx.binbox/`, wiping all local SQLite Room databases, saved hosts, and generated OCI SSH private keys.

## What Was Changed
1. **CI Workflow Keystore Restoration**:
   - Updated `.github/workflows/build-apk-debug.yml` in the `Ensure debug keystore exists` step to decode `debug.keystore.base64` using `base64 -d debug.keystore.base64 > debug.keystore`.
   - Now every CI build produces APKs signed with the exact same deterministic debug certificate.
2. **Version Incrementation**:
   - Assessed probability score of 85.
   - Incremented `versionCode` (17 -> 18) and `debugCode` (`0017` -> `0018`) in `version.properties`.

## Files Touched
- `.github/workflows/build-apk-debug.yml`
- `version.properties`

## Build & Test Verification
- `compile_applet` build succeeded cleanly.
