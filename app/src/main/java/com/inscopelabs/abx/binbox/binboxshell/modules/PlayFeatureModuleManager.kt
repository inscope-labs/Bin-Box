package com.inscopelabs.abx.binbox.binboxshell.modules

import android.content.Context
import com.inscopelabs.abx.binbox.binboxshell.runtime.ShellTier
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages Dynamic Feature module requests for standard and extended shell packages.
 */
class PlayFeatureModuleManager(
    private val context: Context,
    private val stateManager: ModuleStateManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    fun requestInstall(tier: ShellTier, onComplete: ((Boolean) -> Unit)? = null) {
        if (tier == ShellTier.BASE) {
            stateManager.updateState(tier, ModuleState.Installed)
            onComplete?.invoke(true)
            return
        }

        if (stateManager.isInstalled(tier)) {
            BinBoxLogger.d(TAG, "Tier $tier is already installed")
            onComplete?.invoke(true)
            return
        }

        BinBoxLogger.i(TAG, "Initiating dynamic feature module installation for $tier")
        stateManager.updateState(tier, ModuleState.Installing(0.1f))

        scope.launch {
            try {
                // Simulate progressive dynamic feature bundle download & extraction
                for (step in 2..10) {
                    delay(80)
                    stateManager.updateState(tier, ModuleState.Installing(step / 10f))
                }
                stateManager.updateState(tier, ModuleState.Installed)
                BinBoxLogger.i(TAG, "Dynamic feature module installation succeeded for $tier")
                onComplete?.invoke(true)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Installation failed"
                BinBoxLogger.e(TAG, "Failed to install tier $tier: $errorMsg", e)
                stateManager.updateState(tier, ModuleState.Failed(errorMsg))
                onComplete?.invoke(false)
            }
        }
    }

    companion object {
        private const val TAG = "PlayFeatureModuleManager"
    }
}
