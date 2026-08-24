package com.inscopelabs.abx.binbox.oci.provisioning

import android.content.Context
import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.UUID

/**
 * Persists the single active [OciProvisioningSession] so the wizard can
 * resume across process death (§32).
 *
 * Not encrypted like [com.inscopelabs.abx.binbox.oci.identity.OciCredentialsStore] —
 * this session holds OCIDs and provisioning state, not credentials or key
 * material, so there's nothing here that needs Keystore-backed protection.
 * Plain SharedPreferences + Moshi JSON is sufficient and keeps resumption
 * cheap to read on every app start.
 */
class OciProvisioningRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(OciProvisioningSession::class.java)

    fun save(session: OciProvisioningSession): AppResult<Unit> {
        return try {
            prefs.edit().putString(KEY_SESSION, adapter.toJson(session)).apply()
            AppResult.Success(Unit)
        } catch (e: Throwable) {
            BinBoxLogger.e("OciProvisioningRepository", "Failed to persist provisioning session", e)
            AppResult.Error(AppError.IoError("Failed to save provisioning session", e))
        }
    }

    fun load(): AppResult<OciProvisioningSession?> {
        return try {
            val json = prefs.getString(KEY_SESSION, null)
                ?: return AppResult.Success(null)
            AppResult.Success(adapter.fromJson(json))
        } catch (e: Throwable) {
            BinBoxLogger.e("OciProvisioningRepository", "Failed to load provisioning session", e)
            AppResult.Error(AppError.IoError("Failed to load provisioning session", e))
        }
    }

    /** Loads the existing session if one is in progress and not terminal, otherwise starts fresh. */
    fun loadOrCreate(): OciProvisioningSession {
        val existing = (load() as? AppResult.Success)?.data
        if (existing != null && !existing.state.isTerminal) return existing

        val now = System.currentTimeMillis()
        return OciProvisioningSession(
            sessionId = UUID.randomUUID().toString(),
            state = OciProvisioningState.NOT_STARTED,
            createdAtMillis = now,
            updatedAtMillis = now
        )
    }

    fun clear() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    companion object {
        private const val PREFS_NAME = "oci_provisioning_session"
        private const val KEY_SESSION = "session_blob"
    }
}
