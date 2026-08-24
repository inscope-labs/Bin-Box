package com.inscopelabs.abx.binbox.oci.terminal

/**
 * Generic terminal-host abstraction (OCI provisioning doc §27). The
 * terminal never depends on OCI directly — OCI just happens to be the
 * first provider that implements this.
 */
interface ShellHost {
    val id: String
    val displayName: String
    val hostname: String
    val port: Int
    val username: String
    /** Reference into secure key storage — never the key material itself. */
    val sshKeyAlias: String?
}
