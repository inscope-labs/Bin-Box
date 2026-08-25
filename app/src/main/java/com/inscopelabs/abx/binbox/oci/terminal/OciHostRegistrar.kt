package com.inscopelabs.abx.binbox.oci.terminal

import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.repository.IHostRepository

/**
 * Converts a provisioned instance into a real Bin-Box host (§26) — the
 * last step before the OCI provisioning package is usable end to end.
 * Just calls [OciShellHost.toConnectionProfile] and persists it via the
 * existing [IHostRepository]; no new registration mechanism, consistent
 * with [OciShellHost]'s kdoc on reusing the existing terminal stack rather
 * than duplicating it.
 */
class OciHostRegistrar(private val hostRepository: IHostRepository) {

    /**
     * [sshKeyRepositoryId] is the [com.inscopelabs.abx.binbox.domain.model.SshKey]
     * row id (from [com.inscopelabs.abx.binbox.oci.provisioning.OciSshKeyProvisioner]),
     * not the OCI API signing key alias — same distinction called out in
     * that class's kdoc.
     */
    suspend fun register(host: OciShellHost, sshKeyRepositoryId: Long?): AppResult<Long> =
        hostRepository.saveHost(host.toConnectionProfile(sshKeyRepositoryId))
}

/**
 * Default SSH username for a freshly launched instance, confirmed against
 * Oracle's own connection docs (docs.oracle.com/en-us/iaas/Content/Compute/Tasks/connect-to-linux-instance.htm) —
 * NOT a guess: Oracle Linux/RHEL-compatible and CentOS images default to
 * `opc`; Ubuntu images default to `ubuntu`. Matched on a substring of the
 * image's `operatingSystem` field since OCI doesn't expose this as a
 * separate enum. Falls back to `opc` (Oracle's own default image family)
 * if the OS string doesn't match anything recognized, rather than
 * guessing `root` or `ubuntu`.
 */
fun defaultSshUsernameFor(operatingSystem: String): String {
    val os = operatingSystem.lowercase()
    return if (os.contains("ubuntu")) "ubuntu" else "opc"
}
