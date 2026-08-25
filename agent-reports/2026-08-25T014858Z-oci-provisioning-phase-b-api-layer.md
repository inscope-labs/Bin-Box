# OCI Provisioning Package — Phase B (api/ REST client layer)

**Agent:** Claude
**Date:** 2026-08-25
**Depends on:** Phase A (`2026-08-24T205632Z-oci-provisioning-phase-a.md`)

## What this covers

Builds the `api/` package (§5) that Phase A deliberately deferred, plus
wires §14's connection verification into `OciOnboardingViewModel` for real.
Every endpoint path, request/response field name, and header requirement
below was pulled from Oracle's current public documentation this session
(docs.oracle.com/en-us/iaas/...) rather than reconstructed from training
data or the spec doc alone — see "Sources" at the bottom.

`com.inscopelabs.abx.binbox.oci.api/`
- `OciApiConfig.kt` — confirmed per-region host patterns for Identity and
  Core Services
- `OciSigningInterceptor.kt` — OkHttp interceptor, signs every request via
  Phase A's `OciRequestSigner`
- `OciClient.kt` — builds signed Retrofit clients per active region
- `compartments/IdentityApi.kt` + `CompartmentModels.kt` — ListCompartments,
  GetUser
- `networking/` — `VcnApi`, `SubnetApi`, `InternetGatewayApi`,
  `RouteTableApi`, `VnicApi`, `NetworkModels.kt`
- `compute/` — `ComputeApi` (capacity report, launch, get, VNIC
  attachments), `ShapeApi`, `ImageApi`, `ComputeModels.kt`

`provisioning/OciApiErrorMapper.kt` — maps HTTP responses to
`OciErrorCategory` using Oracle's confirmed common error table.

`OciOnboardingViewModel.verifyConnection()` is now real — calls
`IdentityApi.getUser` and transitions to `AUTHENTICATION_VERIFIED` or
`AUTH_FAILED` based on the actual response.

## Two findings that changed the design from what the spec doc assumed

**1. §23 capacity handling.** The doc implies a distinguishable
capacity-unavailable outcome. In practice, "out of host capacity" is not a
distinct OCI error code — it's an undocumented message string
(`"Out of host capacity."`) inside a generic `500 InternalError`, confirmed
from multiple independent real-world reports (Oracle doesn't document this
message as a stable contract). Building `CAPACITY_UNAVAILABLE` detection on
string-matching a 500 would be fragile. Oracle has a real mechanism for
this instead: `CreateComputeCapacityReport`, called with a shape + AD
*before* `LaunchInstance`, returning an explicit
`AVAILABLE` / `OUT_OF_HOST_CAPACITY` / `HARDWARE_NOT_SUPPORTED` status.
`ComputeApi.createComputeCapacityReport` is built around this — the
provisioning engine (not yet built — see below) should call it before every
launch attempt rather than parse launch failures.

**2. `QuotaExceeded` and `LimitExceeded` are real, distinct, documented 400
error codes** (confirmed from Oracle's API Errors reference) — directly
usable for the other half of §23. `OciApiErrorMapper` maps them to
`QUOTA_ERROR`.

## Deviations from the doc's literal §5 file list, flagged for review

- `CompartmentApi.kt` became `IdentityApi.kt` — also carries `GetUser`
  (needed for §14), which didn't fit under a "Compartment" name.
- `RouteTableApi.kt` and `VnicApi.kt` added — not in the doc's listing, but
  required to actually complete the flows the doc itself describes (§18's
  route table wiring, §24's VNIC → public IP resolution). `RouteTableApi`
  only exposes `getRouteTable`/`updateRouteTable`, not `createRouteTable` —
  every VCN already has a default route table (`Vcn.defaultRouteTableId`),
  so pointing that at the internet gateway is simpler than creating a
  second one, and keeps §19's idempotency property free (nothing to
  discover-or-create).
- The doc's `models/` folder (`CreateInstanceDto.kt`, `InstanceStatusDto.kt`,
  `ShapeDto.kt`, `ImageDto.kt`) was consolidated into `compute/ComputeModels.kt`
  — these are tightly coupled request/response pairs for a small number of
  endpoints; a full folder for four files felt like more indirection than
  the content warranted. Easy to split out later if preferred.
- Signing header set for PATCH: Oracle's signing-spec page only documents
  PUT/POST explicitly for body-signing. This client doesn't send any PATCH
  requests yet (all mutations so far are POST/PUT), so this hasn't come up
  in practice, but `OciRequestSigner` from Phase A does treat PATCH as
  body-signed by extrapolation from standard OCI SDK behavior — flagging
  since it's not yet confirmed against this specific page.

## Still not built

The provisioning *engine* — `OciProvisioner`, `NetworkProvisioner`,
`ComputeProvisioner`, `ProvisioningPoller`, `OciProvisioningPlan` (§15-26's
actual orchestration logic: discover-or-create for networking, the launch
+ poll + capacity-report sequence, public IP discovery, SSH verification,
host registration). Phase B is the API surface those will call — the
orchestration logic itself is the next phase.

## Verification

No Android/Gradle toolchain — structural only (32 files brace/paren-balanced
across the whole `oci/` package, cross-file symbols checked by hand). Two
things worth a real build to confirm before relying on this:
1. Moshi's reflection adapter correctly deserializing all the new nested
   data classes (same mechanism Phase A used successfully, but untested at
   this volume).
2. `OciSigningInterceptor`'s header-overwrite behavior — that overwriting
   `content-type` post-hoc actually produces byte-identical bytes to what
   was signed once real network bytes are on the wire, not just in the
   interceptor's own view of the request.

## Sources (fetched this session)

- docs.oracle.com/en-us/iaas/Content/API/Concepts/usingapi.htm — versioning,
  pagination, retry tokens, ETags, error format, CreateVcn example
- docs.oracle.com/en-us/iaas/Content/API/Concepts/signingrequests.htm —
  full signing spec, confirmed against Phase A's `OciRequestSigner`
- docs.oracle.com/en-us/iaas/Content/API/References/apierrors.htm — common
  error table
- OCI CLI reference pages (route-table, subnet, internet-gateway,
  compute-capacity-report) — request field names
- Multiple independent real-world reports confirming the "Out of host
  capacity" / 500 / InternalError behavior
