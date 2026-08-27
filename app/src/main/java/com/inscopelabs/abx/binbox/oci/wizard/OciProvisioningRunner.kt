package com.inscopelabs.abx.binbox.oci.wizard

import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.data.repository.HostRepositoryImpl
import com.inscopelabs.abx.binbox.data.repository.KeyRepositoryImpl
import com.inscopelabs.abx.binbox.domain.model.SshKey
import com.inscopelabs.abx.binbox.oci.api.OciClient
import com.inscopelabs.abx.binbox.oci.api.compartments.Compartment
import com.inscopelabs.abx.binbox.oci.api.compute.Image
import com.inscopelabs.abx.binbox.oci.identity.OciCredentials
import com.inscopelabs.abx.binbox.oci.provisioning.*
import com.inscopelabs.abx.binbox.oci.terminal.OciHostRegistrar
import com.inscopelabs.abx.binbox.oci.terminal.OciShellHost
import com.inscopelabs.abx.binbox.oci.terminal.defaultSshUsernameFor

class OciProvisioningRunner(
    keyRepository: KeyRepositoryImpl,
    hostRepository: HostRepositoryImpl
) {
    private val sshKeyProvisioner = OciSshKeyProvisioner(keyRepository)
    private val hostRegistrar = OciHostRegistrar(hostRepository)

    suspend fun generateVmSshKey(sessionId: String): AppResult<SshKey> {
        BinBoxLogger.d(TAG, "Generating VM SSH key for session $sessionId")
        return sshKeyProvisioner.generateForSession(sessionId)
    }

    suspend fun discoverContext(client: OciClient, tenancyOcid: String): AppResult<Pair<List<Compartment>, List<String>>> {
        BinBoxLogger.i(TAG, "Discovering compartments and availability domains for tenancy $tenancyOcid")
        val discovery = OciContextDiscovery(client)
        val compRes = discovery.fetchCompartments(tenancyOcid)
        val adRes = discovery.fetchAvailabilityDomains(tenancyOcid)

        val err = (compRes as? OciResult.Error)?.error ?: (adRes as? OciResult.Error)?.error
        if (err != null) {
            return AppResult.Error(AppError.NetworkError.Generic(err.whatHappened))
        }

        val compartments = (compRes as? OciResult.Success)?.data.orEmpty()
        val ads = (adRes as? OciResult.Success)?.data.orEmpty().map { it.name }
        return AppResult.Success(Pair(compartments, ads))
    }

    suspend fun registerHost(
        credentials: OciCredentials,
        provisioned: OciProvisioningSession,
        discoveredImages: List<Image>
    ): AppResult<Long> {
        val instanceId = provisioned.instanceOcid ?: return AppResult.Error(AppError.UnexpectedError("Missing instance OCID"))
        val publicIp = provisioned.publicIp ?: return AppResult.Error(AppError.UnexpectedError("Missing public IP"))
        val compartmentId = provisioned.context.selectedCompartmentOcid ?: return AppResult.Error(AppError.UnexpectedError("Missing compartment OCID"))

        val selectedImage = discoveredImages.firstOrNull { it.id == provisioned.context.selectedImageOcid }
        val username = selectedImage?.operatingSystem?.let { defaultSshUsernameFor(it) } ?: "opc"
        val sshKeyRepositoryId = provisioned.sshKeyAlias?.toLongOrNull()

        val shellHost = OciShellHost(
            id = provisioned.sessionId,
            displayName = "Oracle Cloud (${credentials.region})",
            hostname = publicIp,
            username = username,
            sshKeyAlias = provisioned.sshKeyAlias,
            instanceOcid = instanceId,
            region = credentials.region,
            compartmentOcid = compartmentId
        )
        BinBoxLogger.i(TAG, "Registering shell host: $publicIp as $username")
        return hostRegistrar.register(shellHost, sshKeyRepositoryId)
    }

    companion object {
        private const val TAG = "OciProvisioningRunner"
    }
}
