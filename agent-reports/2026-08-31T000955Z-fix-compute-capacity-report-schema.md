# Agent Process Report — Fix Compute Capacity Report Schema Deserialization

- **Timestamp (UTC):** `2026-08-31T00:09:55Z`
- **Task Slug:** `fix-compute-capacity-report-schema`

---

## 1. What Was Asked
The user reported a crash (`JsonDataException: Required value 'id' missing at $`) occurring during the compute provisioning phase right after network provisioning succeeded. The crash occurred when Moshi deserialized the OCI response from `POST /20160918/computeCapacityReports`. The user approved implementing the fix.

---

## 2. Version Increment Assessment (Section 2)
- **Assessed Probability Score:** 95 / 100 (Direct bugfix resolving a crash during OCI compute capacity report parsing in the onboarding flow).
- **Action Taken:** Increment `versionCode` (15 -> 16) and `debugCode` (0015 -> 0016) in `version.properties`.

---

## 3. Prior Logging Gaps Check (Section 3.1)
- PRIOR LOGGING GAPS FOUND: none

---

## 4. Compliance Check (Section 4.1)
- `app/src/main/java/com/inscopelabs/abx/binbox/oci/api/compute/ComputeModels.kt`: 106 lines — pass (compliant, data models, <= 180 lines).
- `app/src/test/java/com/inscopelabs/abx/binbox/oci/OciCallSiteInstrumentationTest.kt`: 161 lines — pass (<= 180 lines).

---

## 5. What Was Changed

### Files Touched:
1. `app/src/main/java/com/inscopelabs/abx/binbox/oci/api/compute/ComputeModels.kt`:
   - Updated `ComputeCapacityReport` so `id`, `compartmentId`, `availabilityDomain`, `timeCreated` are nullable with safe defaults, and `shapeAvailabilities` defaults to an empty list.
   - Updated `CapacityReportShapeAvailability` to make `availableCount` and `faultDomain` optional fields.
   - Resolved the Moshi `JsonDataException` caused by OCI's REST API not including an `id` field in the capacity report JSON response.

2. `app/src/test/java/com/inscopelabs/abx/binbox/oci/OciCallSiteInstrumentationTest.kt`:
   - Updated `testComputeProvisionerCheckCapacityTracesRecorded` to test the exact real-world OCI payload structure without an `id` field.

3. `version.properties`:
   - Incremented `versionCode=16` and `debugCode=0016`.

---

## 6. Commands Run & Results
- `compile_applet`: Succeeded. Full build and unit tests passed.

---

## 7. Assumptions Made
- OCI compute capacity report API returns JSON without a root `id` field; making model fields nullable with safe default values adheres to OCI's REST schema and prevents Moshi parsing exceptions.

---

## 8. Errors, Partial Failures, or Unverified Items
- None. Build and compilation succeeded cleanly.
