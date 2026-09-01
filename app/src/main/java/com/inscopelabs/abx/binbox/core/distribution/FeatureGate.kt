package com.inscopelabs.abx.binbox.core.distribution

import android.content.Context
import com.inscopelabs.abx.binbox.BuildConfig
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FeatureTier {
    PRODUCTION,
    BETA
}

enum class BinBoxFeature(
    val tier: FeatureTier,
    val displayName: String,
    val description: String
) {
    // === Core Production Features (v0.1.0 Baseline) ===
    // Features with tier = PRODUCTION are automatically included in Production release.
    AGNOSTIC_SHELL_HOST(
        tier = FeatureTier.PRODUCTION,
        displayName = "Agnostic Shell Host",
        description = "Core terminal emulator and ANSI control sequence processing."
    ),
    LOCAL_BINBOX_SHELL(
        tier = FeatureTier.PRODUCTION,
        displayName = "Local BinBoxShell",
        description = "Embedded rootless Linux-like shell and local utilities execution."
    ),
    MCP_CLIENT(
        tier = FeatureTier.PRODUCTION,
        displayName = "MCP Client Infrastructure",
        description = "Model Context Protocol client bridge for LLM tool integration."
    ),

    // === Beta-Gated Features ===
    // To promote any feature to production, simply change tier to FeatureTier.PRODUCTION!
    OCI_CLOUD_PROVISIONING(
        tier = FeatureTier.BETA,
        displayName = "OCI Cloud VM Provisioning",
        description = "Automated Oracle Cloud Infrastructure Free-Tier VM lifecycle & onboarding."
    ),
    REMOTE_BACKEND_TRANSPORT(
        tier = FeatureTier.BETA,
        displayName = "Remote Backend Bridge",
        description = "WebSocket and multi-cloud remote daemon relay infrastructure."
    ),
    EXTENDED_SHELL_MODULES(
        tier = FeatureTier.BETA,
        displayName = "Extended Toolchain Tiers",
        description = "Dynamic package delivery for compiler and language runtimes."
    ),
    TERMUX_INTEGRATION(
        tier = FeatureTier.BETA,
        displayName = "Termux Discovery Bridge",
        description = "Inter-app communication and remote socket attachment with Termux."
    );

    val isProduction: Boolean
        get() = tier == FeatureTier.PRODUCTION
}

object FeatureGate {
    private const val TAG = "FeatureGate"
    private const val PREFS_NAME = "binbox_distribution_prefs"
    private const val KEY_BETA_OPT_IN = "beta_testing_opt_in"

    private val _isBetaEnabled = MutableStateFlow(BuildConfig.IS_BETA_BUILD)
    val isBetaEnabled: StateFlow<Boolean> = _isBetaEnabled.asStateFlow()

    fun initialize(context: Context) {
        BinBoxLogger.i(TAG, "Initializing FeatureGate (BuildConfig.IS_BETA_BUILD=${BuildConfig.IS_BETA_BUILD})")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userOptIn = prefs.getBoolean(KEY_BETA_OPT_IN, BuildConfig.IS_BETA_BUILD)
        _isBetaEnabled.value = userOptIn || BuildConfig.IS_BETA_BUILD
        BinBoxLogger.i(TAG, "FeatureGate initialized. Effective beta mode = ${_isBetaEnabled.value}")
    }

    fun isEnabled(feature: BinBoxFeature): Boolean {
        // Any feature marked PRODUCTION is unconditionally enabled.
        if (feature.isProduction) return true
        // Beta features are gated behind beta enrollment / opt-in.
        return _isBetaEnabled.value
    }

    fun setBetaOptIn(context: Context, enabled: Boolean) {
        BinBoxLogger.i(TAG, "Updating user beta opt-in: $enabled")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BETA_OPT_IN, enabled).apply()
        _isBetaEnabled.value = enabled || BuildConfig.IS_BETA_BUILD
    }
}
