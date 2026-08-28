package com.inscopelabs.abx.binbox.oci.terminal

import com.inscopelabs.abx.binbox.domain.model.AuthType
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType

/**
 * OCI supplies OciShellHost; the terminal only ever sees [ShellHost] (§27).
 *
 * ARCHITECTURAL DEVIATION FROM THE DOC, FLAGGED FOR REVIEW:
 * §28-30 propose a dedicated `ShellTransport` interface (`DirectSshTransport`,
 * `AbxRelayTransport`) as the bridge from ShellHost to an actual connection.
 * Bin-Box already has that bridge — [ITransport] / [TerminalSessionFactory],
 * built and wired during Phase 3/5/9 before this package existed — and it
 * already reads its connection parameters from [ConnectionProfile], which
 * IHostRepository already persists. Introducing a second, parallel
 * ShellTransport layer here would duplicate that path rather than complete
 * it, and is exactly the kind of drift the standing addendum flagged when it
 * called out reconciling the upgrade plan's TerminalProvider/TerminalSession
 * stack against this package's ShellHost/ShellTransport stack.
 *
 * This class resolves that reconciliation by converting directly into
 * [ConnectionProfile] instead: OciShellHost -> ConnectionProfile ->
 * TerminalSessionFactory -> ITransport (SshTransport), with no new
 * transport interface. `AbxRelayTransport` (§30) has no equivalent yet —
 * there is no ABX relay/gateway concept in this repo today — so relay-mode
 * connections stay unimplemented rather than stubbed against a transport
 * that doesn't exist. Confirm this direction before building on it further.
 */
data class OciShellHost(
    override val id: String,
    override val displayName: String,
    override val hostname: String,
    override val port: Int = 22,
    override val username: String,
    override val sshKeyAlias: String?,
    val instanceOcid: String,
    val region: String,
    val compartmentOcid: String
) : ShellHost {

    /**
     * Converts this host into the [ConnectionProfile] the existing terminal
     * stack already knows how to run. [keyId] is the Room-side key row id
     * that owns [sshKeyAlias]'s material — callers resolve that via
     * IKeyRepository before calling this, since OciShellHost itself only
     * knows the alias, not the repository row.
     */
    fun toConnectionProfile(keyId: Long?): ConnectionProfile = ConnectionProfile(
        label = displayName,
        host = hostname,
        port = port,
        protocol = ProtocolType.SSH,
        username = username,
        authType = if (keyId != null) AuthType.PRIVATE_KEY else AuthType.PASSWORD,
        keyId = keyId,
        groupTag = com.inscopelabs.abx.binbox.oci.management.OciProvisioningStatus.OCI_HOST_GROUP_TAG
    )
}
