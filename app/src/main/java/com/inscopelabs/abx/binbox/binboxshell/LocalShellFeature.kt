package com.inscopelabs.abx.binbox.binboxshell

import android.content.Context
import com.inscopelabs.abx.binbox.binboxshell.provider.LocalShellProvider
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger

/**
 * Feature facade and singleton registry for the BinBox Local Shell module.
 */
object LocalShellFeature {
    private const val TAG = "LocalShellFeature"
    private var instance: LocalShellProvider? = null

    @Synchronized
    fun getProvider(context: Context): LocalShellProvider {
        return instance ?: LocalShellProvider(context.applicationContext).also {
            instance = it
            BinBoxLogger.i(TAG, "LocalShellFeature singleton initialized")
        }
    }

    val isInitialized: Boolean
        get() = instance != null
}
