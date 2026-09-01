package com.inscopelabs.abx.binbox.binboxshell.runtime

import android.content.Context
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import org.json.JSONObject
import java.io.File

enum class ShellTier(val identifier: String) {
    BASE("base"),
    STANDARD("standard"),
    EXTENDED("extended")
}

data class BinaryDescriptor(
    val name: String,
    val description: String,
    val category: String,
    val nativeLibName: String? = null,
    val fallbackSystemPath: String? = null,
    val required: Boolean = false
)

data class TierManifest(
    val tier: ShellTier,
    val version: String,
    val description: String,
    val binaries: List<BinaryDescriptor>
)

class BinaryRegistry(
    private val context: Context,
    private val runtimePaths: RuntimePaths
) {
    private val manifests = mutableMapOf<ShellTier, TierManifest>()

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
        val desc = root.optString("description", "")
        val binariesArray = root.optJSONArray("binaries") ?: org.json.JSONArray()
        val list = mutableListOf<BinaryDescriptor>()

        for (i in 0 until binariesArray.length()) {
            val item = binariesArray.getJSONObject(i)
            list.add(
                BinaryDescriptor(
                    name = item.getString("name"),
                    description = item.optString("description", ""),
                    category = item.optString("category", "general"),
                    nativeLibName = item.optString("nativeLibName").takeIf { it.isNotBlank() },
                    fallbackSystemPath = item.optString("fallbackSystemPath").takeIf { it.isNotBlank() },
                    required = item.optBoolean("required", false)
                )
            )
        }
        BinBoxLogger.d(TAG, "Loaded manifest for $tier (${list.size} binaries)")
        return TierManifest(tier, version, desc, list)
    }

    fun resolveBinaryPath(name: String): String? {
        val descriptor = getAllBinaries().firstOrNull { it.name == name }

        descriptor?.nativeLibName?.let { libName ->
            runtimePaths.nativeLibDir?.let { libDir ->
                val f = File(libDir, libName)
                if (f.exists() && f.canExecute()) return f.absolutePath
            }
        }

        descriptor?.fallbackSystemPath?.let { sysPath ->
            val f = File(sysPath)
            if (f.exists()) return f.absolutePath
        }

        val sandboxBin = runtimePaths.getBinFile(name)
        if (sandboxBin.exists()) return sandboxBin.absolutePath

        val systemPaths = listOf("/system/bin/$name", "/system/xbin/$name", "/vendor/bin/$name")
        return systemPaths.firstOrNull { File(it).exists() }
    }

    companion object {
        private const val TAG = "BinaryRegistry"
    }
}
