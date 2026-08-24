package com.inscopelabs.abx.binbox.security

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.data.dao.KnownHostKeyDao
import com.inscopelabs.abx.binbox.data.entity.KnownHostKeyEntity
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.UserInfo

/**
 * Trust-on-first-use SSH host-key verification (Phase 5 "Known-host
 * verification / Host-key management"; Phase 9 security hardening).
 *
 * Backs JSch's [HostKeyRepository] with Room storage. A host key seen for
 * the first time on a given host:port is accepted and remembered; on a
 * later connection, a key that doesn't match what's stored is rejected —
 * [SshTransport] pairs this with `StrictHostKeyChecking=yes`, so JSch treats
 * a [HostKeyRepository.CHANGED] result as a hard connection failure rather
 * than a soft warning that could be clicked through.
 *
 * Only a fingerprint is persisted, not the raw key bytes. That's sufficient
 * for the verification property this class provides, but it does mean
 * [getHostKey] / [getHostKey] (host, type) — rarely called by JSch itself;
 * mainly there for an application wanting to list known hosts — return an
 * empty result rather than reconstructed [HostKey] objects. Not a security
 * gap, only a listing convenience nothing currently calls.
 *
 * Known limitation, not addressed here: a host-key mismatch is a legitimate
 * outcome of an intentional VM rebuild, not only of an attack, and this
 * class has no way to tell the two apart — so it always rejects. There is
 * currently no UI path to review and clear a changed key; that's the
 * natural next step.
 */
class HostKeyStore(
    private val dao: KnownHostKeyDao
) : HostKeyRepository {

    override fun check(host: String?, key: ByteArray?): Int {
        if (host.isNullOrBlank() || key == null) return HostKeyRepository.NOT_INCLUDED

        val (parsedHost, parsedPort) = parseHostPort(host)
        val fingerprint = SshKeyManager.computeFingerprint(key)
        val existing = dao.findByHostPortBlocking(parsedHost, parsedPort)

        return when {
            existing == null -> {
                dao.upsertBlocking(
                    KnownHostKeyEntity(
                        host = parsedHost,
                        port = parsedPort,
                        keyType = parseKeyType(key),
                        fingerprint = fingerprint
                    )
                )
                BinBoxLogger.i(
                    "HostKeyStore",
                    "Trusting new host key for $parsedHost:$parsedPort on first use ($fingerprint)"
                )
                HostKeyRepository.OK
            }
            existing.fingerprint == fingerprint -> HostKeyRepository.OK
            else -> {
                BinBoxLogger.e(
                    "HostKeyStore",
                    "Host key MISMATCH for $parsedHost:$parsedPort — stored ${existing.fingerprint}, presented $fingerprint"
                )
                HostKeyRepository.CHANGED
            }
        }
    }

    override fun add(hostkey: HostKey?, ui: UserInfo?) {
        val h = hostkey ?: return
        try {
            val (host, port) = parseHostPort(h.host)
            dao.upsertBlocking(
                KnownHostKeyEntity(
                    host = host,
                    port = port,
                    keyType = h.type,
                    fingerprint = h.getFingerPrint(JSch())
                )
            )
        } catch (e: Exception) {
            BinBoxLogger.w("HostKeyStore", "Failed to persist host key via add(): ${e.message}", e)
        }
    }

    override fun remove(host: String?, type: String?) {
        if (host.isNullOrBlank()) return
        val (h, p) = parseHostPort(host)
        dao.deleteByHostPortBlocking(h, p)
    }

    override fun remove(host: String?, type: String?, key: ByteArray?) = remove(host, type)

    override fun getKnownHostsRepositoryID(): String = "binbox-known-hosts"

    override fun getHostKey(): Array<HostKey> = emptyArray()

    override fun getHostKey(host: String?, type: String?): Array<HostKey> = emptyArray()

    /** JSch formats non-default-port hosts as "[host]:port"; plain hostnames imply port 22. */
    private fun parseHostPort(raw: String): Pair<String, Int> {
        val bracketMatch = Regex("^\\[(.+)]:(\\d+)$").find(raw)
        if (bracketMatch != null) {
            val (h, p) = bracketMatch.destructured
            return h to (p.toIntOrNull() ?: 22)
        }
        return raw to 22
    }

    /** SSH public-key wire format: a 4-byte big-endian length prefix, then that many ASCII bytes of key-type string. */
    private fun parseKeyType(keyBytes: ByteArray): String {
        return try {
            if (keyBytes.size < 4) return "unknown"
            val len = ((keyBytes[0].toInt() and 0xFF) shl 24) or
                ((keyBytes[1].toInt() and 0xFF) shl 16) or
                ((keyBytes[2].toInt() and 0xFF) shl 8) or
                (keyBytes[3].toInt() and 0xFF)
            if (len <= 0 || 4 + len > keyBytes.size) return "unknown"
            String(keyBytes, 4, len, Charsets.US_ASCII)
        } catch (e: Exception) {
            "unknown"
        }
    }
}
