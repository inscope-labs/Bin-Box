package com.inscopelabs.abx.binbox.oci

import com.inscopelabs.abx.binbox.oci.diagnostics.OciStepContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OciStepContextTest {

    @Before
    fun setUp() {
        OciStepContext.clear()
    }

    @Test
    fun testDefaultIsUnknown() {
        val (stage, step) = OciStepContext.currentOrUnknown()
        assertEquals("UNKNOWN", stage)
        assertEquals("unknown", step)
    }

    @Test
    fun testWithStepSetsAndRestoresContext() {
        assertEquals("UNKNOWN" to "unknown", OciStepContext.currentOrUnknown())

        val result = OciStepContext.withStep("NETWORK_PROVISIONING", "ensure_vcn.create") {
            val (stage, step) = OciStepContext.currentOrUnknown()
            assertEquals("NETWORK_PROVISIONING", stage)
            assertEquals("ensure_vcn.create", step)
            "success"
        }

        assertEquals("success", result)
        assertEquals("UNKNOWN" to "unknown", OciStepContext.currentOrUnknown())
    }

    @Test
    fun testNestedStepContexts() {
        OciStepContext.withStep("STAGE_OUTER", "step_outer") {
            assertEquals("STAGE_OUTER" to "step_outer", OciStepContext.currentOrUnknown())

            OciStepContext.withStep("STAGE_INNER", "step_inner") {
                assertEquals("STAGE_INNER" to "step_inner", OciStepContext.currentOrUnknown())
            }

            assertEquals("STAGE_OUTER" to "step_outer", OciStepContext.currentOrUnknown())
        }

        assertEquals("UNKNOWN" to "unknown", OciStepContext.currentOrUnknown())
    }

    @Test
    fun testExceptionRestoresContext() {
        try {
            OciStepContext.withStep("FAIL_STAGE", "fail_step") {
                assertEquals("FAIL_STAGE" to "fail_step", OciStepContext.currentOrUnknown())
                throw IllegalStateException("Intentional failure")
            }
        } catch (_: IllegalStateException) {
            // Expected
        }

        assertEquals("UNKNOWN" to "unknown", OciStepContext.currentOrUnknown())
    }
}
