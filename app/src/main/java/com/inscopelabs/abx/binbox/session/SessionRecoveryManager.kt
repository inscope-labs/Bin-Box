package com.inscopelabs.abx.binbox.session

import android.content.Context
import android.content.SharedPreferences
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ShellProfile
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Manages session persistence and state restoration across process recreations.
 * (Phase 3 — Session & Transport Framework)
 */
class SessionRecoveryManager(
    private val prefs: SharedPreferences,
    moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
) {
    private val adapter = moshi.adapter(SessionRecoveryState::class.java)

    fun saveActiveSessions(
        activeIndex: Int,
        sessionsList: List<Pair<ConnectionProfile, ShellProfile>>
    ) {
        try {
            val snapshots = sessionsList.mapIndexed { idx, pair ->
                SavedSessionSnapshot(
                    sessionId = "recovered_sess_$idx",
                    title = pair.first.label.ifBlank { "Session ${idx + 1}" },
                    profile = pair.first,
                    shellProfile = pair.second
                )
            }
            val recoveryState = SessionRecoveryState(
                activeIndex = activeIndex.coerceIn(0, (snapshots.size - 1).coerceAtLeast(0)),
                sessions = snapshots
            )
            val json = adapter.toJson(recoveryState)
            prefs.edit().putString(KEY_RECOVERY_DATA, json).apply()
            BinBoxLogger.i("SessionRecoveryManager", "Saved ${snapshots.size} active sessions to recovery store")
        } catch (e: Exception) {
            BinBoxLogger.e("SessionRecoveryManager", "Failed to persist session recovery state", e)
        }
    }

    fun loadSavedSessions(): SessionRecoveryState? {
        val json = prefs.getString(KEY_RECOVERY_DATA, null) ?: return null
        return try {
            val state = adapter.fromJson(json)
            BinBoxLogger.i("SessionRecoveryManager", "Loaded ${state?.sessions?.size ?: 0} saved sessions from recovery store")
            state
        } catch (e: Exception) {
            BinBoxLogger.e("SessionRecoveryManager", "Failed to deserialize session recovery state", e)
            null
        }
    }

    fun clearSavedSessions() {
        prefs.edit().remove(KEY_RECOVERY_DATA).apply()
        BinBoxLogger.d("SessionRecoveryManager", "Cleared saved recovery sessions")
    }

    companion object {
        private const val PREFS_NAME = "binbox_session_recovery"
        private const val KEY_RECOVERY_DATA = "saved_sessions_json"

        fun fromContext(context: Context): SessionRecoveryManager {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return SessionRecoveryManager(prefs)
        }
    }
}
