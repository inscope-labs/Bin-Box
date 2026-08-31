package com.inscopelabs.abx.binbox.oci.wizard

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.SshKey
import com.inscopelabs.abx.binbox.oci.api.OciClient
import com.inscopelabs.abx.binbox.oci.api.compute.Image
import com.inscopelabs.abx.binbox.oci.identity.OciCredentials
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningContext
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningSession
import com.inscopelabs.abx.binbox.oci.provisioning.OciResult
import com.inscopelabs.abx.binbox.oci.terminal.defaultSshUsernameFor

/**
 * Module responsible for executing VM SSH key generation, cloud resource provisioning,
 * and host registration.
 */
class OciProvisioningExecutionHandler(
    private val provisioningRunner: OciProvisioningRunner
) {
    suspend fun generateVmSshKey(
        sessionId: String,
        onSuccess: (SshKey) -> Unit,
        onError: (String) -> Unit
    ) {
        BinBoxLogger.i(TAG, "Generating VM SSH key for session $sessionId")
        when (val result = provisioningRunner.generateVmSshKey(sessionId)) {
            is AppResult.Success -> onSuccess(result.data)
            is AppResult.Error -> onError(result.error.userMessage)
            AppResult.Loading -> Unit
        }
    }

    suspend fun startProvisioning(
        session: OciProvisioningSession,
        context: OciProvisioningContext,
        vmSshPublicKey: String?,
        credentials: OciCredentials,
        onSessionProgress: suspend (OciProvisioningSession) -> Unit,
        onSuccess: suspend (OciProvisioningSession) -> Unit,
        onError: (String) -> Unit
    ) {
        BinBoxLogger.i(TAG, "Starting cloud provisioning for session ${session.sessionId}")
        val client = OciClient(credentials.region) { credentials }
        val result = provisioningRunner.runProvisioning(
            session = session,
            context = context,
            vmSshPublicKeyOverride = vmSshPublicKey,
            client = client,
            onSessionUpdate = onSessionProgress
        )
        when (result) {
            is OciResult.Success -> onSuccess(result.data)
            is OciResult.Error -> onError(result.error.whatHappened)
        }
    }

    suspend fun registerHost(
        credentials: OciCredentials,
        provisioned: OciProvisioningSession,
        discoveredImages: List<Image>,
        onSuccess: (hostId: Long, username: String) -> Unit,
        onError: (String) -> Unit
    ) {
        BinBoxLogger.i(TAG, "Registering shell host for session ${provisioned.sessionId}")
        val selectedImage = discoveredImages.firstOrNull { it.id == provisioned.context.selectedImageOcid }
        val username = selectedImage?.operatingSystem?.let { defaultSshUsernameFor(it) } ?: "opc"

        when (val res = provisioningRunner.registerHost(credentials, provisioned, discoveredImages)) {
            is AppResult.Success -> onSuccess(res.data, username)
            is AppResult.Error -> onError(res.error.userMessage)
            AppResult.Loading -> Unit
        }
    }

    companion object {
        private const val TAG = "OciProvisioningExecutionHandler"
    }
}
