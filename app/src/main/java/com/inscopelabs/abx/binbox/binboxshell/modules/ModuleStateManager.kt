package com.inscopelabs.abx.binbox.binboxshell.modules

import com.inscopelabs.abx.binbox.binboxshell.runtime.ShellTier
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class ModuleState {
    data object Available : ModuleState()
    data class Installing(val progress: Float) : ModuleState()
    data object Installed : ModuleState()
    data class Failed(val message: String) : ModuleState()
}

/**
 * Tracks and publishes installation and readiness state for modular shell tiers.
 */
class ModuleStateManager {

    private val _tierStates = MutableStateFlow<Map<ShellTier, ModuleState>>(
        mapOf(
            ShellTier.BASE to ModuleState.Installed,
            ShellTier.STANDARD to ModuleState.Available,
            ShellTier.EXTENDED to ModuleState.Available
        )
    )
    val tierStates: StateFlow<Map<ShellTier, ModuleState>> = _tierStates.asStateFlow()

    fun updateState(tier: ShellTier, state: ModuleState) {
        BinBoxLogger.i(TAG, "Tier state transition: $tier -> ${state::class.simpleName}")
        _tierStates.update { current ->
            current.toMutableMap().apply { put(tier, state) }
        }
    }

    fun getState(tier: ShellTier): ModuleState {
        return _tierStates.value[tier] ?: ModuleState.Available
    }

    fun isInstalled(tier: ShellTier): Boolean {
        return getState(tier) is ModuleState.Installed
    }

    companion object {
        private const val TAG = "ModuleStateManager"
    }
}
