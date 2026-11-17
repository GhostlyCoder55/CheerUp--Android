package com.example.cheerup.core.haptics

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cheerup.core.localization.LanguageManager

class HapticsManager private constructor() {

    private var prefs: SharedPreferences? = null
    private var vibrator: Vibrator? = null

    private var _isEnabled: Boolean by mutableStateOf(true)
    val isEnabled: Boolean
        get() = _isEnabled

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        val sp = appContext.getSharedPreferences(LanguageManager.PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        if (!sp.contains(ENABLED_KEY)) {
            sp.edit().putBoolean(ENABLED_KEY, true).apply()
        }
        _isEnabled = sp.getBoolean(ENABLED_KEY, true)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled = enabled
        prefs?.edit()?.putBoolean(ENABLED_KEY, enabled)?.apply()
    }

    private fun vibrateEffect(effect: VibrationEffect) {
        if (!isEnabled) return
        try {
            val v = vibrator ?: return
            if (v.hasVibrator()) {
                v.vibrate(effect)
            }
        } catch (_: Throwable) {
            // Safe no-op in unit tests or devices without vibrator hardware
        }
    }

    fun lightTap() {
        if (!isEnabled) return
        try {
            vibrateEffect(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } catch (_: Throwable) {
            // Ignored in JVM unit tests
        }
    }

    fun mediumImpact() {
        if (!isEnabled) return
        try {
            vibrateEffect(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } catch (_: Throwable) {
            // Ignored in JVM unit tests
        }
    }

    fun heavyImpact() {
        if (!isEnabled) return
        try {
            vibrateEffect(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } catch (_: Throwable) {
            // Ignored in JVM unit tests
        }
    }

    fun selection() {
        if (!isEnabled) return
        try {
            vibrateEffect(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } catch (_: Throwable) {
            // Ignored in JVM unit tests
        }
    }

    fun success() {
        if (!isEnabled) return
        try {
            vibrateEffect(VibrationEffect.createWaveform(longArrayOf(0, 18, 45, 28), intArrayOf(0, 140, 0, 220), -1))
        } catch (_: Throwable) {
            // Ignored in JVM unit tests
        }
    }

    fun warning() {
        if (!isEnabled) return
        try {
            vibrateEffect(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } catch (_: Throwable) {
            // Ignored in JVM unit tests
        }
    }

    fun error() {
        if (!isEnabled) return
        try {
            vibrateEffect(VibrationEffect.createWaveform(longArrayOf(0, 35, 40, 45), intArrayOf(0, 255, 0, 200), -1))
        } catch (_: Throwable) {
            // Ignored in JVM unit tests
        }
    }

    companion object {
        private const val ENABLED_KEY = "cheerup.haptics.enabled"
        val shared: HapticsManager by lazy { HapticsManager() }
    }
}
