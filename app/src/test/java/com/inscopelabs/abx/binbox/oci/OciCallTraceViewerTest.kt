package com.inscopelabs.abx.binbox.oci

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.abx.binbox.oci.diagnostics.OciCallTraceEntry
import com.inscopelabs.abx.binbox.oci.diagnostics.OciCallTraceStore
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OciCallTraceViewerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        OciCallTraceStore.initialize(context, "trace-viewer-test-session")
        OciCallTraceStore.clear("trace-viewer-test-session")
    }

    @After
    fun tearDown() {
        OciCallTraceStore.clear()
    }

    @Test
    fun testStoreEntriesRecordedAndFiltered() {
        val entry1 = OciCallTraceEntry(
            id = "trace-1",
            timestampUtc = "2026-08-29T12:00:00Z",
            stageId = "CONTEXT_DISCOVERY",
            stepId = "discover_compartments",
            method = "GET",
            url = "https://iaas.us-ashburn-1.oraclecloud.com/20160918/compartments",
            requestHeaders = mapOf("Authorization" to "Signature ...", "date" to "Sat, 29 Aug 2026 12:00:00 GMT"),
            httpStatusCode = 200,
            durationMs = 150
        )

        val entry2 = OciCallTraceEntry(
            id = "trace-2",
            timestampUtc = "2026-08-29T12:00:01Z",
            stageId = "NETWORK_PROVISIONING",
            stepId = "create_vcn",
            method = "POST",
            url = "https://iaas.us-ashburn-1.oraclecloud.com/20160918/vcns",
            requestHeaders = mapOf("Authorization" to "Signature ..."),
            requestBody = "{\"displayName\":\"bin-box-managed\",\"cidrBlock\":\"10.0.0.0/16\"}",
            httpStatusCode = 400,
            ociErrorCode = "LimitExceeded",
            ociErrorMessage = "Maximum number of VCNs reached",
            durationMs = 280
        )

        OciCallTraceStore.record(entry1)
        OciCallTraceStore.record(entry2)

        val entries = OciCallTraceStore.currentSessionEntries()
        assertEquals(2, entries.size)
        assertEquals("trace-1", entries[0].id)
        assertEquals("trace-2", entries[1].id)

        // Verify export contains error details
        val textDump = OciCallTraceStore.exportAsText()
        assertTrue(textDump.contains("LimitExceeded"))
        assertTrue(textDump.contains("Maximum number of VCNs reached"))
        assertTrue(textDump.contains("bin-box-managed"))
        assertTrue(textDump.contains("discover_compartments"))
    }

    @Test
    fun testTraceExceptionFormattingInExport() {
        val entry = OciCallTraceEntry(
            id = "trace-err",
            timestampUtc = "2026-08-29T12:00:02Z",
            stageId = "CONNECTION_VERIFICATION",
            stepId = "verify_user",
            method = "GET",
            url = "https://identity.us-ashburn-1.oraclecloud.com/20160918/users/test",
            requestHeaders = mapOf("Authorization" to "Signature ..."),
            exceptionClass = "java.net.UnknownHostException",
            exceptionMessage = "Unable to resolve host identity.us-ashburn-1.oraclecloud.com",
            durationMs = 12
        )

        OciCallTraceStore.record(entry)
        val dump = OciCallTraceStore.exportAsText()
        assertTrue(dump.contains("java.net.UnknownHostException"))
        assertTrue(dump.contains("Unable to resolve host"))
        assertTrue(dump.contains("CONNECTION_VERIFICATION"))
    }
}
