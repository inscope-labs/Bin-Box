package com.inscopelabs.abx.binbox.session

import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ShellProfile

/**
 * Snapshot of a single session for persistence across process recreations.
 */
data class SavedSessionSnapshot(
    val sessionId: String,
    val title: String,
    val profile: ConnectionProfile,
    val shellProfile: ShellProfile = ShellProfile.DEFAULT,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Container holding state of all active sessions at snapshot time.
 */
data class SessionRecoveryState(
    val activeIndex: Int = 0,
    val sessions: List<SavedSessionSnapshot> = emptyList(),
    val savedAt: Long = System.currentTimeMillis()
)
