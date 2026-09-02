package com.inscopelabs.abx.binbox.transport.backend.api

import com.inscopelabs.abx.binbox.transport.backend.models.BackendDiscoveryResponse
import com.inscopelabs.abx.binbox.transport.backend.models.ProvisionSessionRequest
import com.inscopelabs.abx.binbox.transport.backend.models.ProvisionSessionResponse
import com.inscopelabs.abx.binbox.transport.backend.models.SessionHeartbeatRequest
import com.inscopelabs.abx.binbox.transport.backend.models.SessionHeartbeatResponse
import com.inscopelabs.abx.binbox.transport.backend.models.VmInstanceDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit REST API interface for BinBox backend discovery, VM telemetry,
 * and remote session provisioning.
 */
interface BinBoxBackendApi {

    @GET("api/v1/discovery")
    suspend fun getDiscovery(): BackendDiscoveryResponse

    @GET("api/v1/instances")
    suspend fun listInstances(): List<VmInstanceDto>

    @GET("api/v1/instances/{instanceId}/status")
    suspend fun getInstanceStatus(
        @Path("instanceId") instanceId: String
    ): VmInstanceDto

    @POST("api/v1/sessions/provision")
    suspend fun provisionSession(
        @Body request: ProvisionSessionRequest
    ): ProvisionSessionResponse

    @DELETE("api/v1/sessions/{sessionId}")
    suspend fun terminateSession(
        @Path("sessionId") sessionId: String
    )

    @POST("api/v1/sessions/heartbeat")
    suspend fun sendHeartbeat(
        @Body request: SessionHeartbeatRequest
    ): SessionHeartbeatResponse
}
