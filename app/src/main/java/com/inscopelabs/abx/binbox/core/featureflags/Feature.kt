package com.inscopelabs.abx.binbox.core.featureflags

/**
 * Registry of gate-able, user-facing features. Each entry is pinned to the
 * ReleaseStage it was introduced at (see ReleaseStage). Stabilization-pass
 * stages (0.4.0, 0.6.0, 0.8.0) and the Release-Candidate stage (0.9.0)
 * intentionally introduce no new gate-able feature of their own — they are
 * hardening/feedback stages for what came before, tracked by
 * FeatureFlags.CURRENT_PRODUCTION_STAGE advancing without a new Feature
 * entry appearing here.
 */
enum class Feature(val stage: ReleaseStage) {
    DIAGNOSTICS_INSPECTOR(ReleaseStage.DIAGNOSTICS),
    OCI_EXTENDED_SHELL_HOST(ReleaseStage.OCI_EXTENDED),
    AI_MCP_SHELL_CLIENT(ReleaseStage.AI_MCP_CLIENT),
    PLUGIN_SCRIPT_INSTALLER(ReleaseStage.PLUGIN_INSTALLER)
}
