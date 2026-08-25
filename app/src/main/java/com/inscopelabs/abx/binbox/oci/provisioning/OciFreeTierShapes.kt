package com.inscopelabs.abx.binbox.oci.provisioning

/**
 * Always Free-eligible compute shapes and their limits, confirmed against
 * docs.oracle.com/en-us/iaas/Content/FreeTier/resourceref.htm — not
 * assumed defaults.
 *
 * Ampere A1 (`VM.Standard.A1.Flex`): tenancy-wide pool of 4 OCPUs / 24 GB
 * memory, splittable across up to 4 instances. [DEFAULT_A1_OCPUS]/
 * [DEFAULT_A1_MEMORY_GB] are [OciProvisioner.provision]'s DEFAULT when the
 * caller doesn't specify a split — deliberately HALF the pool (2 OCPUs /
 * 12 GB) rather than the tenancy max, so a single default call doesn't
 * silently consume the entire free Ampere allocation. To split across two
 * VMs, pass an explicit `flexOcpus`/`flexMemoryInGBs` to each of two
 * `provision()` calls instead of relying on this default — see
 * [OciProvisioner]'s kdoc.
 *
 * E2 Micro (`VM.Standard.E2.1.Micro`): fixed shape (no shapeConfig), AMD,
 * up to 2 instances tenancy-wide, restricted to a single availability
 * domain (which one is region-dependent and not fixed here — must be
 * discovered via ListAvailabilityDomains + capacity report, same as A1).
 *
 * Minimum boot volume for either shape: 47 GB (confirmed, same page).
 */
object OciFreeTierShapes {
    const val AMPERE_A1_FLEX = "VM.Standard.A1.Flex"
    const val E2_MICRO = "VM.Standard.E2.1.Micro"

    const val DEFAULT_A1_OCPUS = 2.0
    const val DEFAULT_A1_MEMORY_GB = 12.0

    const val MIN_BOOT_VOLUME_GB = 47

    fun isFlexShape(shape: String): Boolean = shape == AMPERE_A1_FLEX
}
