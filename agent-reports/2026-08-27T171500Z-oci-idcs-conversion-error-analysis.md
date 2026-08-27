# Process Report: Analysis of OCI IdcsConversionError / 401 Unauthorized

- **Timestamp:** 2026-08-27T17:15:00Z
- **Task Slug:** `oci-idcs-conversion-error-analysis`
- **Assessed Probability Score for Debug Build:** 40 / 100
- **Version Increment Action Taken:** No version increment required (score <= 75). `versionCode` remains 8, `debugCode` remains `0008`.

---

## 1. What Was Asked
The user shared an OCI connection diagnostic output showing a `401 Unauthorized` with `OCI Error Code: IdcsConversionError` and `OCI Error Message: Client is unauthorized. null`, inquiring why it was connecting before the last update and what went wrong.

---

## 2. Technical Findings & Root Cause Analysis

### Signature & Key Verification
- **Local Key Verification:** The RSA public key PEM generated in Bin Box was mathematically checked against the reported MD5 fingerprint `95:a1:60:f8:ba:5c:dc:19:8a:12:61:a8:37:f7:da:56`. The calculated MD5 matches the fingerprint in the request header.
- **Request Signing String:** The request headers (`(request-target)`, `host`, `date`) and `Authorization: Signature ...` format strictly adhere to OCI specification.

### Why `IdcsConversionError: Client is unauthorized. null` Occurred
In Oracle Cloud Infrastructure tenancies that use **IAM Identity Domains** (or upgraded IDCS):
1. **API Key Association in Identity Domain:** When a new session or onboarding flow is started, Bin Box generates a fresh hardware-backed key pair in Android KeyStore. The public key PEM must be added directly to the user profile inside the active Identity Domain (**Identity & Security → Domains → Default domain (or active domain) → Users → [User Profile] → API Keys → Add API Key**). If the user registered the key in a different user profile, root domain vs default domain, or generated a new key without uploading the updated PEM to OCI Console, OCI's IDCS conversion layer returns `IdcsConversionError: Client is unauthorized. null`.
2. **OCI Identity Synchronization Delay:** When an API key is newly added in the OCI Console (which persists in the Tenancy's home region), it can take 30–60 seconds for Identity Cloud Service to propagate to regional API endpoints (such as `sa-saopaulo-1`).
3. **IAM Group / Policy Permissions:** If the user account in OCI is not assigned to a group with policies granting permissions to read users/compartments (`ALLOW GROUP <Group> to read users IN TENANCY`), IDCS rejects the request as unauthorized.

---

## 3. Changes Made
- **`OciVerificationDiagnostics.kt`**: Added dedicated detection for `IdcsConversionError` (`isIdcsError`) with targeted troubleshooting steps for Identity Domains, API key upload location, and cross-region propagation timing.

---

## 4. Compliance & Audits
- **PRIOR LOGGING GAPS FOUND:** none
- **COMPLIANCE CHECK (>180L):**
  - `OciVerificationDiagnostics.kt` — Pass (Data class and diagnostic formatting, < 140 lines).
- **Commands Run:**
  - `compile_applet`: Succeeded.
