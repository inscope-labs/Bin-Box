package com.inscopelabs.abx.binbox.core.featureflags

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger

/**
 * Single source of truth for whether a gate-able Feature is visible on this
 * install. Everything at or before CURRENT_PRODUCTION_STAGE is always on.
 * Beta enrollment additionally unlocks exactly the one stage immediately
 * after CURRENT_PRODUCTION_STAGE — never further ahead, since anything
 * beyond that either isn't built yet or hasn't itself completed a beta
 * cycle. Advancing CURRENT_PRODUCTION_STAGE is how a stage graduates to
 * production; nothing else in this file should need to change per release.
 */
object FeatureFlags {

    /**
     * The stage this build's production track has actually proven stable.
     * Bump exactly once per promotion, never speculatively ahead of what has
     * actually completed a full beta cycle.
     */
    val CURRENT_PRODUCTION_STAGE = ReleaseStage.CORE

    fun isEnabled(feature: Feature, betaEnrolled: Boolean): Boolean {
        val enabled = when {
            feature.stage.ordinal <= CURRENT_PRODUCTION_STAGE.ordinal -> true
            feature.stage.ordinal == CURRENT_PRODUCTION_STAGE.ordinal + 1 -> betaEnrolled
            else -> false
        }
        BinBoxLogger.d(
            "FeatureFlags",
            "isEnabled(${feature.name}) stage=${feature.stage.versionName} " +
                "betaEnrolled=$betaEnrolled -> $enabled"
        )
        return enabled
    }
}
