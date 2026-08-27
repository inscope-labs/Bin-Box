package com.inscopelabs.abx.binbox.core.featureflags

import android.content.Context
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger

/**
 * Local, device-only Beta Testing opt-in. No account or backend involved —
 * this is the single source of truth FeatureFlags checks to decide whether
 * this install sees the one stage beyond CURRENT_PRODUCTION_STAGE.
 */
object BetaEnrollment {
    private const val PREFS_NAME = "abx_beta_enrollment_prefs"
    private const val PREF_ENROLLED = "pref_beta_enrolled"

    fun isEnrolled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_ENROLLED, false)
    }

    fun setEnrolled(context: Context, enrolled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_ENROLLED, enrolled).apply()
        BinBoxLogger.i("BetaEnrollment", "Beta enrollment set to $enrolled")
    }
}
