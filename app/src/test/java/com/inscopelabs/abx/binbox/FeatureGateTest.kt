package com.inscopelabs.abx.binbox

import com.inscopelabs.abx.binbox.core.distribution.BinBoxFeature
import com.inscopelabs.abx.binbox.core.distribution.FeatureGate
import com.inscopelabs.abx.binbox.core.distribution.FeatureTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureGateTest {

    @Test
    fun testCoreFeaturesAreProductionTier() {
        assertEquals(FeatureTier.PRODUCTION, BinBoxFeature.AGNOSTIC_SHELL_HOST.tier)
        assertEquals(FeatureTier.PRODUCTION, BinBoxFeature.LOCAL_BINBOX_SHELL.tier)
        assertEquals(FeatureTier.PRODUCTION, BinBoxFeature.MCP_CLIENT.tier)

        assertTrue(BinBoxFeature.AGNOSTIC_SHELL_HOST.isProduction)
        assertTrue(BinBoxFeature.LOCAL_BINBOX_SHELL.isProduction)
        assertTrue(BinBoxFeature.MCP_CLIENT.isProduction)
    }

    @Test
    fun testBetaFeaturesAreBetaTier() {
        assertEquals(FeatureTier.BETA, BinBoxFeature.OCI_CLOUD_PROVISIONING.tier)
        assertEquals(FeatureTier.BETA, BinBoxFeature.REMOTE_BACKEND_TRANSPORT.tier)
        assertEquals(FeatureTier.BETA, BinBoxFeature.EXTENDED_SHELL_MODULES.tier)

        assertFalse(BinBoxFeature.OCI_CLOUD_PROVISIONING.isProduction)
    }

    @Test
    fun testCoreFeaturesAlwaysEnabledRegardlessOfBeta() {
        assertTrue(FeatureGate.isEnabled(BinBoxFeature.AGNOSTIC_SHELL_HOST))
        assertTrue(FeatureGate.isEnabled(BinBoxFeature.LOCAL_BINBOX_SHELL))
        assertTrue(FeatureGate.isEnabled(BinBoxFeature.MCP_CLIENT))
    }
}
