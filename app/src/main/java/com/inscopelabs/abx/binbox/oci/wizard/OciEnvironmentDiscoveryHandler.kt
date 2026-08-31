package com.inscopelabs.abx.binbox.oci.wizard

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.oci.api.OciApiConfig
import com.inscopelabs.abx.binbox.oci.api.OciClient
import com.inscopelabs.abx.binbox.oci.api.compartments.Compartment
import com.inscopelabs.abx.binbox.oci.api.compute.Image
import com.inscopelabs.abx.binbox.oci.api.compute.Instance
import com.inscopelabs.abx.binbox.oci.diagnostics.OciStepContext
import com.inscopelabs.abx.binbox.oci.identity.OciCredentials
import com.inscopelabs.abx.binbox.oci.identity.OciKeyManager
import com.inscopelabs.abx.binbox.oci.provisioning.OciApiErrorMapper
import com.inscopelabs.abx.binbox.oci.provisioning.OciContextDiscovery
import com.inscopelabs.abx.binbox.oci.provisioning.OciResult

/**
 * Module responsible for OCI connection verification and environment discovery (compartments, ADs, shapes, images).
 */
class OciEnvironmentDiscoveryHandler(
    private val provisioningRunner: OciProvisioningRunner
) {
    suspend fun verifyConnection(
        credentials: OciCredentials,
        currentPublicKeyPem: String?,
        onSuccess: (OciVerificationDiagnostics, String?) -> Unit,
        onError: (String, OciVerificationDiagnostics, String?) -> Unit
    ) {
        val endpointUrl = "${OciApiConfig.identityBaseUrl(credentials.region)}20160918/users/${credentials.userOcid}"
        val pubKeyPem = currentPublicKeyPem ?: OciKeyManager.ensureSigningKey(credentials.keyAlias).let {
            if (it is AppResult.Success) it.data else null
        }
        val keyDigest = OciKeyManager.localPublicKeyDigest(credentials.keyAlias)

        BinBoxLogger.i(TAG, "Starting OCI connection verification for user ${credentials.userOcid} in region ${credentials.region}")
        try {
            val client = OciClient(credentials.region) { credentials }
            val response = OciStepContext.withStep(STAGE_VERIFY, "verify_connection.get_user") {
                client.identityApi.getUser(credentials.userOcid)
            }
            val opcReqId = response.headers()["opc-request-id"]

            if (response.isSuccessful) {
                BinBoxLogger.i(TAG, "OCI connection verified successfully. OPC Request ID: $opcReqId")
                val diagnostics = baseDiagnostics(endpointUrl, credentials, pubKeyPem, keyDigest).copy(
                    httpStatusCode = response.code(),
                    opcRequestId = opcReqId
                )
                onSuccess(diagnostics, pubKeyPem)
            } else {
                val apiError = OciApiErrorMapper.fromErrorResponse(response)
                BinBoxLogger.w(TAG, "OCI verification failed with code ${response.code()}: ${apiError.whatHappened}")
                val diagnostics = baseDiagnostics(endpointUrl, credentials, pubKeyPem, keyDigest).copy(
                    httpStatusCode = response.code(),
                    ociErrorCode = apiError.whyItHappened,
                    ociErrorMessage = apiError.whatHappened,
                    opcRequestId = opcReqId
                )
                onError(apiError.whatHappened, diagnostics, pubKeyPem)
            }
        } catch (e: Exception) {
            BinBoxLogger.e(TAG, "Connection verification exception", e)
            val isHostError = e.javaClass.simpleName.contains("UnknownHost", ignoreCase = true) ||
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true
            val userMsg = if (isHostError) {
                "Unable to reach Oracle Cloud (${credentials.region}). Check your network connection and region name."
            } else {
                "Couldn't reach OCI: ${e.localizedMessage ?: e.javaClass.simpleName}"
            }
            val diagnostics = baseDiagnostics(endpointUrl, credentials, pubKeyPem, keyDigest).copy(
                exceptionClass = e.javaClass.name,
                rawExceptionMessage = e.message ?: e.toString()
            )
            onError(userMsg, diagnostics, pubKeyPem)
        }
    }

    suspend fun discoverContext(
        client: OciClient,
        credentials: OciCredentials,
        currentPublicKeyPem: String?,
        onSuccess: (List<Compartment>, List<String>, List<Instance>) -> Unit,
        onError: (String, OciVerificationDiagnostics) -> Unit
    ) {
        val endpointUrl = "${OciApiConfig.identityBaseUrl(credentials.region)}20160918/compartments?compartmentId=${credentials.tenancyOcid}"
        val pubKeyPem = currentPublicKeyPem ?: OciKeyManager.ensureSigningKey(credentials.keyAlias).let {
            if (it is AppResult.Success) it.data else null
        }
        val keyDigest = OciKeyManager.localPublicKeyDigest(credentials.keyAlias)

        BinBoxLogger.i(TAG, "Discovering compartments and availability domains for tenancy ${credentials.tenancyOcid}")
        when (val res = provisioningRunner.discoverContext(client, credentials.tenancyOcid)) {
            is OciResult.Success -> {
                val (compartments, ads) = res.data
                val existingInstances = discoverExistingInstances(client, credentials.tenancyOcid)
                BinBoxLogger.i(TAG, "Discovered ${compartments.size} compartments, ${ads.size} ADs, and ${existingInstances.size} existing instances")
                onSuccess(compartments, ads, existingInstances)
            }
            is OciResult.Error -> {
                val apiError = res.error
                BinBoxLogger.w(TAG, "Context discovery failed: ${apiError.whatHappened}")
                val diagnostics = baseDiagnostics(endpointUrl, credentials, pubKeyPem, keyDigest).copy(
                    ociErrorCode = apiError.whyItHappened,
                    ociErrorMessage = apiError.whatHappened
                )
                onError(apiError.whatHappened, diagnostics)
            }
        }
    }

    suspend fun fetchShapes(
        client: OciClient,
        compartmentOcid: String,
        availabilityDomain: String,
        onSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        BinBoxLogger.i(TAG, "Fetching eligible shapes for AD $availabilityDomain in compartment $compartmentOcid")
        when (val result = OciContextDiscovery(client).fetchEligibleShapes(compartmentOcid, availabilityDomain)) {
            is OciResult.Success -> {
                BinBoxLogger.i(TAG, "Discovered ${result.data.size} eligible shapes")
                onSuccess(result.data)
            }
            is OciResult.Error -> {
                BinBoxLogger.w(TAG, "Failed fetching shapes: ${result.error.whatHappened}")
                onError(result.error.whatHappened)
            }
        }
    }

    suspend fun fetchImages(
        client: OciClient,
        compartmentOcid: String,
        shape: String,
        onSuccess: (List<Image>) -> Unit,
        onError: (String) -> Unit
    ) {
        BinBoxLogger.i(TAG, "Fetching images for shape $shape in compartment $compartmentOcid")
        when (val result = OciContextDiscovery(client).fetchImages(compartmentOcid, shape)) {
            is OciResult.Success -> {
                BinBoxLogger.i(TAG, "Discovered ${result.data.size} eligible images")
                onSuccess(result.data)
            }
            is OciResult.Error -> {
                BinBoxLogger.w(TAG, "Failed fetching images: ${result.error.whatHappened}")
                onError(result.error.whatHappened)
            }
        }
    }

    suspend fun discoverExistingInstances(client: OciClient, compartmentOcid: String): List<Instance> {
        BinBoxLogger.i(TAG, "Checking existing compute instances in $compartmentOcid")
        return when (val res = provisioningRunner.discoverExistingInstances(client, compartmentOcid)) {
            is OciResult.Success -> res.data
            is OciResult.Error -> {
                BinBoxLogger.w(TAG, "Existing instance query returned: ${res.error.whatHappened}")
                emptyList()
            }
        }
    }

    private fun baseDiagnostics(
        endpointUrl: String,
        credentials: OciCredentials,
        pubKeyPem: String?,
        keyDigest: String?
    ) = OciVerificationDiagnostics(
        endpointUrl = endpointUrl,
        httpMethod = "GET",
        region = credentials.region,
        tenancyOcid = credentials.tenancyOcid,
        userOcid = credentials.userOcid,
        fingerprint = credentials.fingerprint.value,
        keyAlias = credentials.keyAlias,
        publicKeyPem = pubKeyPem,
        localKeyDigest = keyDigest
    )

    companion object {
        private const val TAG = "OciEnvDiscoveryHandler"
        const val STAGE_VERIFY = "CONNECTION_VERIFICATION"
    }
}
