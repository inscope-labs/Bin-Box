# Process Report: Debug Workflow Keystore Analysis & Single Signing Key Assurance

- **Date / Timestamp**: 2026-08-31T12:06:10Z
- **Task**: Analyze debug workflow to ensure consistent single signing key usage across all debug builds to allow seamless in-place APK updates without uninstall/reinstall.

## Prior Logging Gaps Found
- `PRIOR LOGGING Gaps Found: none`

## Compliance Check (>180L)
- `COMPLIANCE CHECK (>180L): .github/workflows/build-apk-debug.yml — pass` (155 lines, clean single-purpose GitHub Actions workflow).

## Root Cause & Workflow Analysis
1. **Android In-Place Upgrade Requirements**:
   - Android permits APK updates over existing installations only if the package name matches, `versionCode` is >= installed `versionCode`, and the **signing certificate SHA-256 fingerprint matches byte-for-byte**.
   - If an initial install came from a local Android Studio build (`~/.android/debug.keystore`), an ad-hoc generated keystore, or a different CI runner before `debug.keystore.base64` was restored, Android blocks updates with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
2. **Current Repository Keystore State**:
   - `debug.keystore.base64` exists at repository root and decodes to an RSA 2048-bit certificate:
     - **Alias**: `androiddebugkey`
     - **SHA-256 Fingerprint**: `6A:68:B6:93:37:70:D8:ED:4B:71:83:1F:26:40:66:BE:56:78:C3:2F:EA:AE:E2:D4:E3:D7:8C:2A:32:42:37:4C`
     - **Validity**: 2026-08-21 until 2054-01-06
3. **Workflow Enhancements**:
   - Updated `.github/workflows/build-apk-debug.yml`:
     - Added support for GitHub Actions Secret `DEBUG_KEYSTORE_BASE64` as highest priority override.
     - Preserved fallback to repository file `debug.keystore.base64`.
     - Added automated printing of keystore certificate details (Alias, SHA1, SHA256 fingerprints, validity) directly to GitHub Actions build logs so signatures can be verified against installed APKs.

## Files Touched
- `.github/workflows/build-apk-debug.yml`

## Version Increment Action
- **Probability Score**: 40 (CI workflow configuration enhancement; versionCode 19 already available from prior step).
- **Action Taken**: Maintained `versionCode=19` / `debugCode=0019`.

## Commands Run & Build Verification
- Keystore decoding & verification: `keytool -list -v -keystore ...` -> PKCS12 `androiddebugkey` SHA256 verified.
- `compile_applet` -> `Build succeeded - the applet is compiled`.
