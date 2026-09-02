package com.inscopelabs.abx.binbox.transport.backend.api

import com.inscopelabs.abx.binbox.domain.model.VmStatus
import com.inscopelabs.abx.binbox.transport.backend.models.BackendDiscoveryResponse
import com.inscopelabs.abx.binbox.transport.backend.models.ProvisionSessionRequest
import com.inscopelabs.abx.binbox.transport.backend.models.ProvisionSessionResponse
import com.inscopelabs.abx.binbox.transport.backend.models.SessionHeartbeatRequest
import com.inscopelabs.abx.binbox.transport.backend.models.SessionHeartbeatResponse
import com.inscopelabs.abx.binbox.transport.backend.models.VmInstanceDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * High-level client for the BinBox Backend & Gateway APIs.
 * Supports live Retrofit communication and offline sandbox mock fallbacks.
 */
class BinBoxBackendClient(
    baseUrl: String = DEFAULT_BASE_URL,
    private val apiKey: String? = null,
    private val enableMockFallback: Boolean = true
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://gateway.abx.internal/"
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            if (!apiKey.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                requestBuilder.addHeader("X-API-Key", apiKey)
            }
            requestBuilder.addHeader("User-Agent", "BinBox-Android/1.0")
            chain.proceed(requestBuilder.build())
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

    private val api: BinBoxBackendApi by lazy {
        Retrofit.Builder()
            .baseUrl(sanitizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BinBoxBackendApi::class.java)
    }

    /**
     * Queries gateway discovery information.
     */
    suspend fun getDiscovery(): Result<BackendDiscoveryResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDiscovery()
            Result.success(response)
        } catch (e: Exception) {
            if (enableMockFallback) {
                Result.success(createMockDiscovery())
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Lists cloud/VM instances managed by this gateway.
     */
    suspend fun listInstances(): Result<List<VmStatus>> = withContext(Dispatchers.IO) {
        try {
            val dtoList = api.listInstances()
            Result.success(dtoList.map { it.toDomain() })
        } catch (e: Exception) {
            if (enableMockFallback) {
                Result.success(createMockInstances())
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Queries status and telemetry for a specific instance.
     */
    suspend fun getInstanceStatus(instanceId: String): Result<VmStatus> = withContext(Dispatchers.IO) {
        try {
            val dto = api.getInstanceStatus(instanceId)
            Result.success(dto.toDomain())
        } catch (e: Exception) {
            if (enableMockFallback) {
                val mock = createMockInstances().firstOrNull { it.instanceId == instanceId }
                    ?: createMockInstances().first()
                Result.success(mock)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Provisions a remote interactive terminal session.
     */
    suspend fun provisionSession(request: ProvisionSessionRequest): Result<ProvisionSessionResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.provisionSession(request)
            Result.success(response)
        } catch (e: Exception) {
            if (enableMockFallback) {
                val sessionId = UUID.randomUUID().toString().take(8)
                val wsHost = sanitizedBaseUrl.replace("http://", "ws://").replace("https://", "wss://")
                Result.success(
                    ProvisionSessionResponse(
                        sessionId = sessionId,
                        websocketUrl = "${wsHost}ws/terminal/$sessionId",
                        authToken = "bbx_token_${UUID.randomUUID()}",
                        status = "READY"
                    )
                )
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Terminates a remote session.
     */
    suspend fun terminateSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.terminateSession(sessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            if (enableMockFallback) {
                Result.success(Unit)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Sends heartbeat to keep the session alive.
     */
    suspend fun sendHeartbeat(sessionId: String): Result<SessionHeartbeatResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = api.sendHeartbeat(SessionHeartbeatRequest(sessionId))
            Result.success(resp)
        } catch (e: Exception) {
            if (enableMockFallback) {
                Result.success(SessionHeartbeatResponse(sessionId = sessionId, acknowledged = true))
            } else {
                Result.failure(e)
            }
        }
    }

    // --- Mock Fallbacks ---

    private fun createMockDiscovery(): BackendDiscoveryResponse = BackendDiscoveryResponse(
        serverVersion = "1.2.0-abx",
        gatewayName = "ABX Cloud Gateway (Simulated)",
        capabilities = listOf("ws_pty", "resize", "heartbeat", "token_auth", "metrics"),
        supportedProtocols = listOf("ssh", "websocket", "raw_tcp"),
        availableRegions = listOf("us-ashburn-1", "us-phoenix-1", "eu-frankfurt-1"),
        activeSessionsCount = 3,
        status = "OPERATIONAL"
    )

    private fun createMockInstances(): List<VmStatus> = listOf(
        VmInstanceDto(
            instanceId = "ocid1.instance.oc1.iad.abx_oracle_arm1",
            displayName = "Oracle Always-Free ARM (4-OCPU)",
            state = "RUNNING",
            publicIp = "129.153.64.102",
            privateIp = "10.0.0.14",
            region = "us-ashburn-1",
            shape = "VM.Standard.A1.Flex",
            ocpus = 4.0f,
            memoryGbs = 24.0f,
            uptimeSeconds = 345600L,
            cpuUtilizationPercent = 14.5f,
            memoryUtilizationPercent = 32.8f,
            diskUtilizationPercent = 21.0f
        ).toDomain(),
        VmInstanceDto(
            instanceId = "ocid1.instance.oc1.iad.abx_dev_box2",
            displayName = "ABX Edge Micro (x86_64)",
            state = "RUNNING",
            publicIp = "140.238.192.45",
            privateIp = "10.0.0.18",
            region = "us-phoenix-1",
            shape = "VM.Standard.E2.1.Micro",
            ocpus = 1.0f,
            memoryGbs = 1.0f,
            uptimeSeconds = 86400L,
            cpuUtilizationPercent = 6.2f,
            memoryUtilizationPercent = 54.0f,
            diskUtilizationPercent = 45.1f
        ).toDomain()
    )
}
