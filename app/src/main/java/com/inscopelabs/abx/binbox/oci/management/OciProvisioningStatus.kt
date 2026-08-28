package com.inscopelabs.abx.binbox.oci.management

import com.inscopelabs.abx.binbox.data.entity.HostEntity
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile

/**
 * Single source of truth for "has OCI provisioning verifiably completed" — i.e. does at least
 * one real, registered host exist that came out of the OCI wizard. Deliberately keyed off the
 * host repository rather than the wizard's local
 * [com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningSession] state: a session can be
 * abandoned, fail, or get cleared, but a registered host is the durable evidence that
 * provisioning *and* registration both actually succeeded (see
 * [com.inscopelabs.abx.binbox.oci.terminal.OciHostRegistrar]) — that's what "verifiably" means
 * here, and why every OCI entry point should check this rather than a wizard-side flag before
 * deciding whether to open onboarding or [OciManagementScreen].
 */
object OciProvisioningStatus {
    /** [ConnectionProfile.groupTag] / [HostEntity.groupTag] value set by
     * [com.inscopelabs.abx.binbox.oci.terminal.OciShellHost.toConnectionProfile] — the marker
     * tying a host back to OCI provisioning. Single definition so the wizard side (which sets
     * it) and every entry point (which checks it) can't silently drift apart. */
    const val OCI_HOST_GROUP_TAG = "Oracle Cloud"

    fun isOciHost(host: HostEntity): Boolean = host.groupTag == OCI_HOST_GROUP_TAG
    fun isOciHost(profile: ConnectionProfile): Boolean = profile.groupTag == OCI_HOST_GROUP_TAG

    fun hasCompletedProvisioning(hosts: List<HostEntity>): Boolean = hosts.any(::isOciHost)
}
