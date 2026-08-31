package com.inscopelabs.abx.binbox.oci

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.abx.binbox.oci.api.OciClient
import com.inscopelabs.abx.binbox.oci.diagnostics.OciCallTraceStore
import com.inscopelabs.abx.binbox.oci.diagnostics.OciStepContext
import com.inscopelabs.abx.binbox.oci.identity.OciCredentials
import com.inscopelabs.abx.binbox.oci.identity.OciFingerprint
import com.inscopelabs.abx.binbox.oci.identity.OciKeyManager
import com.inscopelabs.abx.binbox.oci.provisioning.ComputeProvisioner
import com.inscopelabs.abx.binbox.oci.provisioning.NetworkProvisioner
import com.inscopelabs.abx.binbox.oci.provisioning.OciContextDiscovery
import com.inscopelabs.abx.binbox.oci.provisioning.OciResult
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OciCallSiteInstrumentationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var context: Context
    private lateinit var testClient: OciClient

    private val testCredentials by lazy {
        OciKeyManager.ensureSigningKey("test-callsite-alias")
        OciCredentials(
            tenancyOcid = "ocid1.tenancy.oc1..test",
            userOcid = "ocid1.user.oc1..test",
            fingerprint = OciFingerprint("20:3b:97:13:55:1c:5b:0d:d3:37:d8:50:4e:c9:42:4a"),
            region = "us-ashburn-1",
            keyAlias = "test-callsite-alias"
        )
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        OciCallTraceStore.initialize(context, "instrumentation-test-session")
        OciCallTraceStore.clear("instrumentation-test-session")
        OciStepContext.clear()
        mockWebServer = MockWebServer()
        mockWebServer.start()

        testClient = OciClient(
            region = "us-ashburn-1",
            baseUrlOverride = mockWebServer.url("/").toString(),
            credentialsProvider = { testCredentials }
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        OciStepContext.clear()
    }

    @Test
    fun testContextDiscoveryTracesRecordedWithStageAndStep() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[{\"id\":\"ocid1.compartment.oc1..test\",\"compartmentId\":\"ocid1.tenancy.oc1..test\",\"name\":\"test-comp\",\"description\":\"desc\",\"lifecycleState\":\"ACTIVE\",\"timeCreated\":\"2026-01-01T00:00:00.000Z\"}]")
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[{\"id\":\"ocid1.ad.oc1..test\",\"compartmentId\":\"ocid1.tenancy.oc1..test\",\"name\":\"AD-1\"}]")
        )

        val discovery = OciContextDiscovery(testClient)
        val compRes = discovery.fetchCompartments("ocid1.tenancy.oc1..test")
        assertTrue(compRes is OciResult.Success)

        val adRes = discovery.fetchAvailabilityDomains("ocid1.tenancy.oc1..test")
        assertTrue(adRes is OciResult.Success)

        val traces = OciCallTraceStore.currentSessionEntries()
        assertEquals(2, traces.size)
        assertEquals("CONTEXT_DISCOVERY", traces[0].stageId)
        assertEquals("discover_compartments", traces[0].stepId)
        assertEquals("CONTEXT_DISCOVERY", traces[1].stageId)
        assertEquals("discover_availability_domains", traces[1].stepId)
    }

    @Test
    fun testNetworkProvisionerTracesRecordedWithStageAndStep() = runBlocking {
        // 1. listVcns -> empty
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        // 2. createVcn -> success
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                "{\"id\":\"ocid1.vcn.oc1..test\",\"compartmentId\":\"ocid1.compartment.oc1..test\",\"displayName\":\"bin-box-managed\",\"cidrBlock\":\"10.0.0.0/16\",\"defaultRouteTableId\":\"ocid1.rt.oc1..test\",\"defaultSecurityListId\":\"ocid1.sl.oc1..test\",\"defaultDhcpOptionsId\":\"ocid1.dhcp.oc1..test\",\"lifecycleState\":\"AVAILABLE\",\"timeCreated\":\"2026-01-01T00:00:00.000Z\"}"
            )
        )
        // 3. listInternetGateways -> empty
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        // 4. createInternetGateway -> success
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                "{\"id\":\"ocid1.igw.oc1..test\",\"compartmentId\":\"ocid1.compartment.oc1..test\",\"vcnId\":\"ocid1.vcn.oc1..test\",\"displayName\":\"bin-box-managed\",\"isEnabled\":true,\"lifecycleState\":\"AVAILABLE\",\"timeCreated\":\"2026-01-01T00:00:00.000Z\"}"
            )
        )
        // 5. updateRouteTable -> success
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                "{\"id\":\"ocid1.rt.oc1..test\",\"compartmentId\":\"ocid1.compartment.oc1..test\",\"vcnId\":\"ocid1.vcn.oc1..test\",\"displayName\":\"bin-box-managed\",\"routeRules\":[],\"lifecycleState\":\"AVAILABLE\",\"timeCreated\":\"2026-01-01T00:00:00.000Z\"}"
            )
        )
        // 6. listSubnets -> empty
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        // 7. createSubnet -> success
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                "{\"id\":\"ocid1.subnet.oc1..test\",\"compartmentId\":\"ocid1.compartment.oc1..test\",\"vcnId\":\"ocid1.vcn.oc1..test\",\"displayName\":\"bin-box-managed\",\"cidrBlock\":\"10.0.0.0/24\",\"routeTableId\":\"ocid1.rt.oc1..test\",\"lifecycleState\":\"AVAILABLE\",\"timeCreated\":\"2026-01-01T00:00:00.000Z\"}"
            )
        )

        val provisioner = NetworkProvisioner(testClient)
        val result = provisioner.ensureNetwork("ocid1.compartment.oc1..test", "test-session-123")
        assertTrue(result is OciResult.Success)

        val traces = OciCallTraceStore.currentSessionEntries()
        assertEquals(7, traces.size)
        traces.forEach { assertEquals("NETWORK_PROVISIONING", it.stageId) }
        assertEquals("ensure_vcn.list", traces[0].stepId)
        assertEquals("ensure_vcn.create", traces[1].stepId)
        assertEquals("ensure_igw.list", traces[2].stepId)
        assertEquals("ensure_igw.create", traces[3].stepId)
        assertEquals("update_route_table", traces[4].stepId)
        assertEquals("ensure_subnet.list", traces[5].stepId)
        assertEquals("ensure_subnet.create", traces[6].stepId)
    }

    @Test
    fun testComputeProvisionerCheckCapacityTracesRecorded() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                "{\"compartmentId\":\"ocid1.compartment.oc1..test\",\"availabilityDomain\":\"AD-1\",\"shapeAvailabilities\":[{\"instanceShape\":\"VM.Standard.A1.Flex\",\"availabilityStatus\":\"AVAILABLE\"}],\"timeCreated\":\"2026-08-30T19:58:47.000Z\"}"
            )
        )

        val provisioner = ComputeProvisioner(testClient)
        val result = provisioner.checkCapacity("ocid1.compartment.oc1..test", "AD-1", "VM.Standard.A1.Flex", null)
        assertTrue(result is OciResult.Success)

        val traces = OciCallTraceStore.currentSessionEntries()
        assertEquals(1, traces.size)
        assertEquals("COMPUTE_PROVISIONING", traces[0].stageId)
        assertEquals("check_capacity", traces[0].stepId)
    }
}
