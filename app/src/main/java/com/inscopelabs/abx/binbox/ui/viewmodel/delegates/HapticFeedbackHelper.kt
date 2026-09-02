package com.inscopelabs.abx.binbox.ui.viewmodel.delegates

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticFeedbackHelper(private val application: Application) {

    fun triggerHaptic(enabled: Boolean) {
        if (!enabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(15)
                }
            }
        } catch (_: Throwable) {
            // Ignore if vibration unsupported or in test environment
        }
    }

    fun onTerminalBell(enabled: Boolean) {
        if (!enabled) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(80, 200))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (_: Throwable) {
            // Ignore
        }
    }
}
