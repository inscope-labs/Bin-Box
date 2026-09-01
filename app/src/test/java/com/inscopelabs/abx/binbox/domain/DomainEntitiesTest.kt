package com.inscopelabs.abx.binbox.domain

import com.inscopelabs.abx.binbox.data.entity.HistoryEntity
import com.inscopelabs.abx.binbox.data.entity.HostEntity
import com.inscopelabs.abx.binbox.data.entity.KeyEntity
import com.inscopelabs.abx.binbox.data.entity.SnippetEntity
import com.inscopelabs.abx.binbox.data.mapper.toDomain
import com.inscopelabs.abx.binbox.data.mapper.toEntity
import com.inscopelabs.abx.binbox.domain.model.AuthType
import com.inscopelabs.abx.binbox.domain.model.CommandHistory
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.model.ShellProfile
import com.inscopelabs.abx.binbox.domain.model.Snippet
import com.inscopelabs.abx.binbox.domain.model.SshKey
import com.inscopelabs.abx.binbox.domain.model.TerminalSession
import com.inscopelabs.abx.binbox.domain.model.TerminalSessionState
import com.inscopelabs.abx.binbox.domain.model.VmState
import com.inscopelabs.abx.binbox.domain.model.VmStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DomainEntitiesTest {

    @Test
    fun testConnectionProfileDefaultsAndDisplayEndpoint() {
        val sshProfile = ConnectionProfile(
            id = 1L,
            label = "Prod Bastion",
            host = "192.168.1.100",
            port = 2222,
            username = "admin",
            protocol = ProtocolType.SSH
        )
        assertEquals("admin@192.168.1.100:2222", sshProfile.displayEndpoint)

        val localProfile = ConnectionProfile(
            id = 2L,
            label = "Termux Local",
            host = "localhost",
            protocol = ProtocolType.LOCAL_SHELL
        )
        assertEquals("localhost (Local PTY)", localProfile.displayEndpoint)

        val demoProfile = ConnectionProfile(
            id = 3L,
            label = "Sandbox Demo",
            host = "demo.sandbox",
            protocol = ProtocolType.DEMO_HOST
        )
        assertEquals("demo.sandbox (Simulated)", demoProfile.displayEndpoint)
    }

    @Test
    fun testTerminalSessionLifecycleState() {
        val profile = ConnectionProfile(label = "Dev Server", host = "dev.local")
        val session = TerminalSession(
            profile = profile,
            state = TerminalSessionState.CONNECTING
        )

        assertNotNull(session.sessionId)
        assertFalse(session.isConnected)
        assertFalse(session.isTerminal)

        val connectedSession = session.copy(state = TerminalSessionState.CONNECTED)
        assertTrue(connectedSession.isConnected)
        assertFalse(connectedSession.isTerminal)

        val terminatedSession = session.copy(state = TerminalSessionState.TERMINATED)
        assertFalse(terminatedSession.isConnected)
        assertTrue(terminatedSession.isTerminal)
    }

    @Test
    fun testShellProfileConfigurations() {
        val bash = ShellProfile.BASH
        assertEquals("/bin/bash", bash.shellPath)
        assertEquals("xterm-256color", bash.termType)

        val zsh = ShellProfile.ZSH
        assertEquals("/bin/zsh", zsh.shellPath)
    }

    @Test
    fun testVmStatusMetrics() {
        val vm = VmStatus(
            instanceId = "ocid1.instance.oc1.iad.abx",
            displayName = "Oracle Ampere A1",
            state = VmState.RUNNING,
            publicIp = "140.238.10.22",
            ocpus = 4.0f,
            memoryGbs = 24.0f,
            cpuUtilizationPercent = 14.5f,
            memoryUtilizationPercent = 32.0f
        )

        assertEquals(VmState.RUNNING, vm.state)
        assertEquals("140.238.10.22", vm.publicIp)
        assertEquals(4.0f, vm.ocpus, 0.01f)
        assertEquals(24.0f, vm.memoryGbs, 0.01f)
    }

    @Test
    fun testEntityDomainMappersBidirectional() {
        val hostEntity = HostEntity(
            id = 42L,
            label = "Staging Cluster",
            host = "staging.company.internal",
            port = 22,
            protocol = "SSH",
            username = "deploy",
            authType = "PRIVATE_KEY",
            keyId = 7L,
            groupTag = "Production",
            isFavorite = true
        )

        val domainHost = hostEntity.toDomain()
        assertEquals(42L, domainHost.id)
        assertEquals("Staging Cluster", domainHost.label)
        assertEquals(ProtocolType.SSH, domainHost.protocol)
        assertEquals(AuthType.PRIVATE_KEY, domainHost.authType)
        assertTrue(domainHost.isFavorite)

        val entityReconstructed = domainHost.toEntity()
        assertEquals(hostEntity, entityReconstructed)

        val keyEntity = KeyEntity(
            id = 10L,
            title = "Personal Ed25519",
            keyType = "ED25519",
            publicKey = "ssh-ed25519 AAAAC3...",
            privateKey = "-----BEGIN OPENSSH PRIVATE KEY-----...",
            fingerprint = "SHA256:abcd1234"
        )
        val domainKey = keyEntity.toDomain()
        assertEquals(10L, domainKey.id)
        assertEquals("Personal Ed25519", domainKey.title)
        assertEquals(keyEntity, domainKey.toEntity())

        val snippetEntity = SnippetEntity(
            id = 5L,
            title = "Docker prune",
            commandTemplate = "docker system prune -af",
            category = "Docker"
        )
        val domainSnippet = snippetEntity.toDomain()
        assertEquals("Docker prune", domainSnippet.title)
        assertEquals(snippetEntity, domainSnippet.toEntity())

        val historyEntity = HistoryEntity(
            id = 99L,
            command = "htop",
            hostLabel = "Prod Server"
        )
        val domainHistory = historyEntity.toDomain()
        assertEquals("htop", domainHistory.command)
        assertEquals(historyEntity, domainHistory.toEntity())
    }

    @Test
    fun testDeleteHostChallengeWordsGeneration() {
        val host1 = HostEntity(id = 1L, label = "Host A", host = "1.2.3.4", protocol = "SSH")
        val host2 = HostEntity(id = 2L, label = "Host B", host = "1.2.3.5", protocol = "SSH")

        val words1 = com.inscopelabs.abx.binbox.ui.components.getChallengeWordsForHost(host1)
        val words2 = com.inscopelabs.abx.binbox.ui.components.getChallengeWordsForHost(host2)

        assertTrue(words1.isNotBlank())
        assertEquals(2, words1.split(" ").size)
        assertTrue(words2.isNotBlank())
        assertEquals(2, words2.split(" ").size)
    }
}
