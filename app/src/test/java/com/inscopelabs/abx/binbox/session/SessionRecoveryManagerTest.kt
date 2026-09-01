package com.inscopelabs.abx.binbox.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.model.ShellProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionRecoveryManagerTest {

    @Test
    fun sessionRecoveryManager_savesAndLoadsSessionsSuccessfully() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = SessionRecoveryManager.fromContext(context)

        val profile1 = ConnectionProfile(
            label = "Production Server",
            host = "prod.server.com",
            port = 22,
            protocol = ProtocolType.SSH
        )
        val profile2 = ConnectionProfile(
            label = "Local Term",
            host = "localhost",
            protocol = ProtocolType.LOCAL_SHELL
        )

        val sessionList = listOf(
            Pair(profile1, ShellProfile.DEFAULT),
            Pair(profile2, ShellProfile(id = "custom_sh", shellPath = "/system/bin/sh"))
        )

        manager.saveActiveSessions(activeIndex = 1, sessionsList = sessionList)

        val recovered = manager.loadSavedSessions()
        assertNotNull(recovered)
        assertEquals(1, recovered!!.activeIndex)
        assertEquals(2, recovered.sessions.size)

        val s1 = recovered.sessions[0]
        assertEquals("Production Server", s1.profile.label)
        assertEquals(ProtocolType.SSH, s1.profile.protocol)

        val s2 = recovered.sessions[1]
        assertEquals("Local Term", s2.profile.label)
        assertEquals(ProtocolType.LOCAL_SHELL, s2.profile.protocol)
        assertEquals("/system/bin/sh", s2.shellProfile.shellPath)

        // Clear sessions
        manager.clearSavedSessions()
        assertNull(manager.loadSavedSessions())
    }
}
