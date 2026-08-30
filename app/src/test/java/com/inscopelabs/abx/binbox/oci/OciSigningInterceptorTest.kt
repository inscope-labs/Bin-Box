package com.inscopelabs.abx.binbox.oci

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.abx.binbox.oci.api.OciSigningInterceptor
import com.inscopelabs.abx.binbox.oci.diagnostics.OciCallTraceStore
import com.inscopelabs.abx.binbox.oci.diagnostics.OciStepCallTagger
import com.inscopelabs.abx.binbox.oci.diagnostics.OciStepContext
import com.inscopelabs.abx.binbox.oci.identity.OciCredentials
import com.inscopelabs.abx.binbox.oci.identity.OciFingerprint
import com.inscopelabs.abx.binbox.oci.identity.OciKeyManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OciSigningInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var context: Context

    private val testCredentials by lazy {
        OciKeyManager.ensureSigningKey("test-interceptor-alias")
        OciCredentials(
            tenancyOcid = "ocid1.tenancy.oc1..test",
            userOcid = "ocid1.user.oc1..test",
            fingerprint = OciFingerprint("20:3b:97:13:55:1c:5b:0d:d3:37:d8:50:4e:c9:42:4a"),
            region = "us-ashburn-1",
            keyAlias = "test-interceptor-alias"
        )
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        OciCallTraceStore.initialize(context, "interceptor-test-session")
        OciCallTraceStore.clear("interceptor-test-session")
        OciStepContext.clear()
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        OciStepContext.clear()
    }

    @Test
    fun testSuccessfulRequestRecordsFullTrace() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("opc-request-id", "test-request-id-123")
                .setBody("{\"id\":\"ocid1.vcn.oc1..test\",\"lifecycleState\":\"AVAILABLE\"}")
        )

        val client = OkHttpClient.Builder()
            .eventListenerFactory(OciStepCallTagger)
            .addInterceptor(OciSigningInterceptor { testCredentials })
            .build()

        val jsonBody = "{\"cidrBlock\":\"10.0.0.0/16\"}"
        val request = Request.Builder()
            .url(mockWebServer.url("/20160918/vcns"))
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = OciStepContext.withStep("NETWORK_PROVISIONING", "ensure_vcn.create") {
            client.newCall(request).execute()
        }

        assertEquals(200, response.code)
        response.close()

        val traces = OciCallTraceStore.currentSessionEntries()
        assertEquals(1, traces.size)
        val entry = traces[0]

        assertEquals("NETWORK_PROVISIONING", entry.stageId)
        assertEquals("ensure_vcn.create", entry.stepId)
        assertEquals("POST", entry.method)
        assertEquals(200, entry.httpStatusCode)
        assertNotNull(entry.requestHeaders["Authorization"])
        assertTrue(entry.requestHeaders["Authorization"]!!.startsWith("Signature "))
        assertEquals(jsonBody, entry.requestBody)
        assertTrue(entry.responseBody!!.contains("AVAILABLE"))
    }

    @Test
    fun testErrorResponseExtractsOciErrorCode() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("{\"code\":\"LimitExceeded\",\"message\":\"Cannot create more VCNs\"}")
        )

        val client = OkHttpClient.Builder()
            .eventListenerFactory(OciStepCallTagger)
            .addInterceptor(OciSigningInterceptor { testCredentials })
            .build()

        val request = Request.Builder()
            .url(mockWebServer.url("/20160918/vcns"))
            .get()
            .build()

        val response = OciStepContext.withStep("NETWORK_PROVISIONING", "ensure_vcn.list") {
            client.newCall(request).execute()
        }

        assertEquals(400, response.code)
        response.close()

        val traces = OciCallTraceStore.currentSessionEntries()
        assertEquals(1, traces.size)
        val entry = traces[0]

        assertEquals("LimitExceeded", entry.ociErrorCode)
        assertEquals("Cannot create more VCNs", entry.ociErrorMessage)
    }

    @Test
    fun testMutatingRequestPreservesAndSignsWireContentType() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"id\":\"ocid1.routetable.oc1..test\"}")
        )

        val client = OkHttpClient.Builder()
            .eventListenerFactory(OciStepCallTagger)
            .addInterceptor(OciSigningInterceptor { testCredentials })
            .build()

        val jsonBody = "{\"routeRules\":[{\"cidrBlock\":\"0.0.0.0/0\"}]}"
        val request = Request.Builder()
            .url(mockWebServer.url("/20160918/routeTables/ocid1.routetable.oc1..test"))
            .put(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val response = OciStepContext.withStep("NETWORK_PROVISIONING", "update_route_table") {
            client.newCall(request).execute()
        }

        assertEquals(200, response.code)
        response.close()

        val recordedRequest = mockWebServer.takeRequest()
        val authHeader = recordedRequest.getHeader("Authorization")
        val contentTypeHeader = recordedRequest.getHeader("Content-Type")

        assertNotNull(authHeader)
        assertTrue(authHeader!!.startsWith("Signature "))
        assertTrue(authHeader.contains("headers=\"(request-target) host date x-content-sha256 content-length content-type\""))
        assertEquals("application/json; charset=utf-8", contentTypeHeader)
    }
}
