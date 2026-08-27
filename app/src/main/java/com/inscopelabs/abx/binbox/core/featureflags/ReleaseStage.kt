package com.inscopelabs.abx.binbox.core.featureflags

/**
 * BinBox's 10-stage production/beta roadmap, v0.1.0 through v1.0.0.
 *
 * Both tracks climb this same sequence. Beta is always exactly one stage
 * ahead of production: production sits at FeatureFlags.CURRENT_PRODUCTION_STAGE,
 * and beta-enrolled users additionally see that stage's immediate successor.
 * No stage is ever skipped, and beta never runs more than one stage ahead —
 * a stage only advances once the one before it has proven stable.
 */
enum class ReleaseStage(val versionName: String, val displayName: String) {
    CORE("0.1.0", "Core Shell Host & Management Console"),
    DIAGNOSTICS("0.2.0", "Diagnostics & Telemetry Inspector"),
    OCI_EXTENDED("0.3.0", "Extended OCI Always Free VM Shell Host"),
    STABILIZATION_1("0.4.0", "Stabilization Pass I"),
    AI_MCP_CLIENT("0.5.0", "AI Agnostic API / MCP Shell Client"),
    STABILIZATION_2("0.6.0", "Stabilization Pass II"),
    PLUGIN_INSTALLER("0.7.0", "Host Agnostic Plugin Script Installer"),
    STABILIZATION_3("0.8.0", "Stabilization Pass III"),
    RELEASE_CANDIDATE("0.9.0", "Final Release-Candidate Hardening"),
    STABLE("1.0.0", "Public Stable Release");

    val nextStage: ReleaseStage?
        get() = entries.getOrNull(ordinal + 1)
}
