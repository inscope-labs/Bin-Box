package com.inscopelabs.abx.binbox.binboxshell.runtime

import android.content.Context
import android.os.Build
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

enum class ShellTier(val identifier: String) {
    BASE("base"),
    STANDARD("standard"),
    EXTENDED("extended")
}

data class SourceInfo(
    val project: String,
    val license: String,
    val upstreamVersion: String,
    val mirrorUrl: String,
    val commit: String
)

data class SharedBinaryDescriptor(
    val id: String,
    val nativeLibName: String,
    val dependsOn: List<String> = emptyList(),
    val sha256: Map<String, String> = emptyMap()
)

data class BinaryDescriptor(
    val name: String,
    val description: String,
    val category: String,
    val sharedBinary: String,
    val required: Boolean = false
)

data class TierManifest(
    val tier: ShellTier,
    val version: String,
    val source: SourceInfo?,
    val sharedBinaries: List<SharedBinaryDescriptor>,
    val binaries: List<BinaryDescriptor>
)

class BinaryRegistry(
    private val context: Context,
    private val runtimePaths: RuntimePaths
) {
    private val manifests = mutableMapOf<ShellTier, TierManifest>()

    // Cache of nativeLibName -> verified-OK, so we hash each shared binary at most
    // once per process lifetime rather than on every resolveBinaryPath() call.
    private val verifiedLibs = mutableSetOf<String>()
    private val failedLibs = mutableSetOf<String>()

    init {
        loadAllManifests()
    }

    fun loadAllManifests() {
        ShellTier.entries.forEach { tier ->
            loadManifestForTier(tier)?.let { manifests[tier] = it }
        }
    }

    fun getManifest(tier: ShellTier): TierManifest? = manifests[tier]

    fun getAllBinaries(): List<BinaryDescriptor> = manifests.values.flatMap { it.binaries }

    fun loadManifestForTier(tier: ShellTier): TierManifest? {
        val assetPath = "shell/manifests/${tier.identifier}.json"
        return try {
            val jsonStr = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            parseManifest(tier, jsonStr)
        } catch (e: Exception) {
            BinBoxLogger.w(TAG, "Failed to load manifest for $tier from $assetPath: ${e.message}")
            null
        }
    }

    fun parseManifest(tier: ShellTier, jsonString: String): TierManifest {
        val root = JSONObject(jsonString)
        val version = root.optString("version", "1.0.0")

        val source = root.optJSONObject("source")?.let {
            SourceInfo(
                project = it.optString("project", ""),
                license = it.optString("license", ""),
                upstreamVersion = it.optString("upstreamVersion", ""),
                mirrorUrl = it.optString("mirrorUrl", ""),
                commit = it.optString("commit", "")
            )
        }

        val sharedBinariesArray = root.optJSONArray("sharedBinaries") ?: org.json.JSONArray()
        val sharedBinaries = mutableListOf<SharedBinaryDescriptor>()
        for (i in 0 until sharedBinariesArray.length()) {
            val item = sharedBinariesArray.getJSONObject(i)
            val sha256Obj = item.optJSONObject("sha256")
            val sha256Map = mutableMapOf<String, String>()
            sha256Obj?.keys()?.forEach { abi -> sha256Map[abi] = sha256Obj.getString(abi) }

            val dependsOnArray = item.optJSONArray("dependsOn")
            val dependsOn = mutableListOf<String>()
            if (dependsOnArray != null) {
                for (j in 0 until dependsOnArray.length()) dependsOn.add(dependsOnArray.getString(j))
            }

            sharedBinaries.add(
                SharedBinaryDescriptor(
                    id = item.getString("id"),
                    nativeLibName = item.getString("nativeLibName"),
                    dependsOn = dependsOn,
                    sha256 = sha256Map
                )
            )
        }

        val binariesArray = root.optJSONArray("binaries") ?: org.json.JSONArray()
        val binaries = mutableListOf<BinaryDescriptor>()
        for (i in 0 until binariesArray.length()) {
            val item = binariesArray.getJSONObject(i)
            binaries.add(
                BinaryDescriptor(
                    name = item.getString("name"),
                    description = item.optString("description", ""),
                    category = item.optString("category", "general"),
                    sharedBinary = item.getString("sharedBinary"),
                    required = item.optBoolean("required", false)
                )
            )
        }

        BinBoxLogger.d(TAG, "Loaded manifest for $tier (${sharedBinaries.size} shared binaries, ${binaries.size} applets)")
        return TierManifest(tier, version, source, sharedBinaries, binaries)
    }

    /**
     * Resolves [name] to an absolute, hash-verified path, or null if the applet
     * isn't known, its shared binary isn't present, or its hash doesn't match
     * the manifest for the device's primary ABI.
     */
    fun resolveBinaryPath(name: String): String? {
        val manifest = manifests.values.firstOrNull { m -> m.binaries.any { it.name == name } }
        val descriptor = manifest?.binaries?.firstOrNull { it.name == name }
        val shared = manifest?.sharedBinaries?.firstOrNull { it.id == descriptor?.sharedBinary }

        if (shared != null) {
            val libDir = runtimePaths.nativeLibDir
            if (libDir != null) {
                val f = File(libDir, shared.nativeLibName)
                if (f.exists() && f.canExecute() && verifyHash(f, shared)) {
                    return f.absolutePath
                }
            }
        }

        // Fallback: a binary placed directly in the sandbox bin dir (e.g. a
        // shell script, or a future non-shared-binary applet).
        val sandboxBin = runtimePaths.getBinFile(name)
        if (sandboxBin.exists()) return sandboxBin.absolutePath

        val systemPaths = listOf("/system/bin/$name", "/system/xbin/$name", "/vendor/bin/$name")
        return systemPaths.firstOrNull { File(it).exists() }
    }

    /**
     * Verifies [file]'s SHA-256 against [shared]'s manifest hash for the
     * device's primary ABI. Result is cached per nativeLibName for the life
     * of this registry instance so repeated command invocations don't re-hash
     * the same binary on every call.
     */
    private fun verifyHash(file: File, shared: SharedBinaryDescriptor): Boolean {
        if (verifiedLibs.contains(shared.nativeLibName)) return true
        if (failedLibs.contains(shared.nativeLibName)) return false

        val abi = Build.SUPPORTED_ABIS.firstOrNull { shared.sha256.containsKey(it) }
        val expected = abi?.let { shared.sha256[it] }
        if (expected == null) {
            BinBoxLogger.w(TAG, "No manifest sha256 for ${shared.nativeLibName} matching device ABIs ${Build.SUPPORTED_ABIS.joinToString()}")
            failedLibs.add(shared.nativeLibName)
            return false
        }

        val actual = try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buf = ByteArray(8192)
                var read: Int
                while (input.read(buf).also { read = it } != -1) digest.update(buf, 0, read)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            BinBoxLogger.w(TAG, "Failed to hash ${shared.nativeLibName}: ${e.message}")
            null
        }

        return if (actual == expected) {
            verifiedLibs.add(shared.nativeLibName)
            true
        } else {
            BinBoxLogger.w(TAG, "Hash mismatch for ${shared.nativeLibName}: expected $expected, got $actual")
            failedLibs.add(shared.nativeLibName)
            false
        }
    }

    companion object {
        private const val TAG = "BinaryRegistry"
    }
}
