package com.inscopelabs.abx.binbox.oci

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.abx.binbox.oci.diagnostics.OciCallTraceEntry
import com.inscopelabs.abx.binbox.oci.diagnostics.OciCallTraceStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class OciCallTraceStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        OciCallTraceStore.initialize(context, "test-session-123")
        OciCallTraceStore.clear("test-session-123")
    }

    @Test
    fun testRecordAndRetrieveEntries() {
        val entry = OciCallTraceEntry(
            id = UUID.randomUUID().toString(),
            timestampUtc = "2026-08-29T11:00:00Z",
            stageId = "NETWORK_PROVISIONING",
            stepId = "ensure_vcn.create",
            method = "POST",
            url = "https://iaas.us-ashburn-1.oraclecloud.com/20160918/vcns",
            requestHeaders = mapOf("Authorization" to "Signature keyId=..."),
            requestBody = "{\"cidrBlock\":\"10.0.0.0/16\"}",
            httpStatusCode = 200,
            responseHeaders = mapOf("opc-request-id" to "req-1"),
            responseBody = "{\"id\":\"ocid1.vcn.oc1...\"}",
            ociErrorCode = null,
            ociErrorMessage = null,
            exceptionClass = null,
            exceptionMessage = null,
            durationMs = 250L
        )

        OciCallTraceStore.record(entry)
        val entries = OciCallTraceStore.currentSessionEntries()

        assertEquals(1, entries.size)
        assertEquals("NETWORK_PROVISIONING", entries[0].stageId)
        assertEquals("ensure_vcn.create", entries[0].stepId)
        assertEquals(200, entries[0].httpStatusCode)

        val exported = OciCallTraceStore.exportAsText()
        assertTrue(exported.contains("ensure_vcn.create"))
        assertTrue(exported.contains("10.0.0.0/16"))
    }

    @Test
    fun testJsonlFilePersistence() {
        val entry = OciCallTraceEntry(
            id = UUID.randomUUID().toString(),
            timestampUtc = "2026-08-29T11:05:00Z",
            stageId = "COMPUTE_PROVISIONING",
            stepId = "launch_instance",
            method = "POST",
            url = "https://iaas.us-ashburn-1.oraclecloud.com/20160918/instances",
            requestHeaders = emptyMap(),
            requestBody = null,
            httpStatusCode = 400,
            responseHeaders = null,
            responseBody = "{\"code\":\"LimitExceeded\",\"message\":\"Out of host capacity\"}",
            ociErrorCode = "LimitExceeded",
            ociErrorMessage = "Out of host capacity",
            exceptionClass = null,
            exceptionMessage = null,
            durationMs = 120L
        )

        OciCallTraceStore.record(entry)

        val traceDir = File(context.filesDir, "oci-trace")
        val jsonlFile = File(traceDir, "test-session-123.jsonl")

        assertTrue(jsonlFile.exists())
        val content = jsonlFile.readText()
        assertTrue(content.contains("LimitExceeded"))
        assertTrue(content.contains("Out of host capacity"))
    }

    @Test
    fun testClearResetsSession() {
        val entry = OciCallTraceEntry(
            id = UUID.randomUUID().toString(),
            timestampUtc = "2026-08-29T11:10:00Z",
            stageId = "STAGE_A",
            stepId = "step_1",
            method = "GET",
            url = "https://iaas.us-ashburn-1.oraclecloud.com/test",
            requestHeaders = emptyMap(),
            requestBody = null,
            httpStatusCode = 200,
            responseHeaders = null,
            responseBody = null,
            ociErrorCode = null,
            ociErrorMessage = null,
            exceptionClass = null,
            exceptionMessage = null,
            durationMs = 50L
        )

        OciCallTraceStore.record(entry)
        assertEquals(1, OciCallTraceStore.currentSessionEntries().size)

        OciCallTraceStore.clear("test-session-456")
        assertEquals(0, OciCallTraceStore.currentSessionEntries().size)
    }
}
