package com.inscopelabs.abx.binbox.binboxshell.provider

import android.content.Context
import com.inscopelabs.abx.binbox.binboxshell.modules.ModuleStateManager
import com.inscopelabs.abx.binbox.binboxshell.modules.PlayFeatureModuleManager
import com.inscopelabs.abx.binbox.binboxshell.runtime.BinaryDescriptor
import com.inscopelabs.abx.binbox.binboxshell.runtime.BinaryRegistry
import com.inscopelabs.abx.binbox.binboxshell.runtime.EnvironmentManager
import com.inscopelabs.abx.binbox.binboxshell.runtime.RuntimePaths
import com.inscopelabs.abx.binbox.binboxshell.runtime.ShellTier
import com.inscopelabs.abx.binbox.binboxshell.security.ShellSecurity
import com.inscopelabs.abx.binbox.binboxshell.session.ShellSessionManager
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import java.io.File

/**
 * High-level provider and lifecycle coordinator for the BinBox local POSIX shell subsystem.
 */
class LocalShellProvider(
    private val context: Context,
    val runtimePaths: RuntimePaths = RuntimePaths(context),
    val binaryRegistry: BinaryRegistry = BinaryRegistry(context, runtimePaths),
    val environmentManager: EnvironmentManager = EnvironmentManager(context, runtimePaths),
    val shellSecurity: ShellSecurity = ShellSecurity(runtimePaths),
    val moduleStateManager: ModuleStateManager = ModuleStateManager(),
    val featureModuleManager: PlayFeatureModuleManager = PlayFeatureModuleManager(context, moduleStateManager),
    val sessionManager: ShellSessionManager = ShellSessionManager(runtimePaths, environmentManager, shellSecurity)
) {

    init {
        initialize()
    }

    private fun initialize() {
        BinBoxLogger.i(TAG, "Initializing LocalShellProvider")
        runtimePaths.ensureDirectories()
        environmentManager.setupInitialEnvironment()
    }

    fun getAvailableBinaries(): List<BinaryDescriptor> = binaryRegistry.getAllBinaries()

    fun isBinaryAvailable(name: String): Boolean {
        return binaryRegistry.resolveBinaryPath(name) != null
    }

    fun requestTierInstallation(tier: ShellTier, onComplete: ((Boolean) -> Unit)? = null) {
        featureModuleManager.requestInstall(tier, onComplete)
    }

    fun createShellProcess(
        command: List<String>? = null,
        workingDir: File? = null,
        environment: Map<String, String>? = null
    ): Process {
        return sessionManager.spawnProcess(command, workingDir, environment)
    }

    companion object {
        private const val TAG = "LocalShellProvider"
    }
}
