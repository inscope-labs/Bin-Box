# Process Report: Fix SSH Private Key PEM Encoding (PKCS#8 / PKCS#1 Mismatch)

- **Date / Timestamp**: 2026-08-31T03:32:00Z
- **Task**: Fix SSH private key PEM encoding where JSch threw `InvalidKeyException("invalid privatekey")` during session connection.

## Prior Logging Gaps Found
- `PRIOR LOGGING Gaps Found: none`

## Compliance Check (>180L)
- `COMPLIANCE CHECK (>180L): app/src/main/java/com/inscopelabs/abx/binbox/security/SshKeyManager.kt — pass` (183 lines, cohesive cryptographic key generator Module with error/info logging via `BinBoxLogger`).
- `COMPLIANCE CHECK (>180L): app/src/test/java/com/inscopelabs/abx/binbox/core/DiagnosticsAndSecurityTest.kt — pass` (228 lines, existing unit test suite).

## What Was Asked & Implemented
1. **Root Cause Analysis**:
   - JCA's `KeyPairGenerator.getInstance("RSA")` returns private key bytes in PKCS#8 format (`PrivateKey.getEncoded()`).
   - `SshKeyManager.generateRsaKey()` previously wrapped these PKCS#8 bytes in a `-----BEGIN RSA PRIVATE KEY-----` (PKCS#1) header.
   - JSch parsed the header expecting pure PKCS#1 DER bytes, resulting in `invalid privatekey` on connect.
2. **Fix Implemented**:
   - Added ASN.1 `pkcs8ToPkcs1` unwrap helper to `SshKeyManager.kt` that walks the PKCS#8 sequence structure and extracts the raw PKCS#1 `RSAPrivateKey` bytes from the OCTET STRING payload for `-----BEGIN RSA PRIVATE KEY-----`.
   - Updated `generateEcKey()` to use the standard `-----BEGIN PRIVATE KEY-----` header wrapping standard PKCS#8 bytes, fully compatible with JSch.
3. **Unit Tests Added & Updated**:
   - Created `app/src/test/java/com/inscopelabs/abx/binbox/security/SshKeyManagerTest.kt` with tests for RSA/EC JSch parsing, keypair loading (`KeyPair.load`), and public/private fingerprint verification.
   - Updated `DiagnosticsAndSecurityTest.kt` for the standardized EC PEM header.

## Files Touched
- `app/src/main/java/com/inscopelabs/abx/binbox/security/SshKeyManager.kt`
- `app/src/test/java/com/inscopelabs/abx/binbox/security/SshKeyManagerTest.kt`
- `app/src/test/java/com/inscopelabs/abx/binbox/core/DiagnosticsAndSecurityTest.kt`
- `version.properties`

## Version Increment Action
- **Probability Score**: 95 (Core crypto bugfix affecting SSH terminal connections).
- **Action Taken**: Incremented `versionCode` (18 -> 19) and `debugCode` (`0018` -> `0019`) in `version.properties`.

## Commands Run & Build Verification
- `gradle :app:testDebugUnitTest` -> `BUILD SUCCESSFUL` (all unit tests passed).
- `compile_applet` -> `Build succeeded - the applet is compiled`.
