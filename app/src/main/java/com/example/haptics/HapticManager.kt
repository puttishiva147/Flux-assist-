package com.example.haptics

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.example.data.models.BallSettings

class HapticManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val hasVibratorHardware: Boolean = vibrator?.hasVibrator() == true
    private val hasAmplitudeControl: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.hasAmplitudeControl() == true
    } else false

    private val hapticExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "assistx-haptics").apply {
            priority = Thread.NORM_PRIORITY
            isDaemon = true
        }
    }

    @Volatile
    private var cachedSystemHapticOn = true
    @Volatile
    private var lastSystemHapticCheckTime = 0L
    private var lastVibrateTimestamp = 0L

    private fun isSystemHapticOn(): Boolean {
        val now = SystemClock.uptimeMillis()
        if (now - lastSystemHapticCheckTime > 5000L) {
            lastSystemHapticCheckTime = now
            try {
                cachedSystemHapticOn = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.HAPTIC_FEEDBACK_ENABLED,
                    1
                ) != 0
            } catch (ignored: Exception) {
                cachedSystemHapticOn = true
            }
        }
        return cachedSystemHapticOn
    }

    fun performHaptic(
        type: HapticType,
        settings: BallSettings? = null,
        isDragEvent: Boolean = false,
        isActionEvent: Boolean = false
    ) {
        if (!hasVibratorHardware || vibrator == null) return

        // 1. Check user settings
        if (settings != null) {
            if (!settings.hapticsEnabled) return
            if (isDragEvent && !settings.dragHapticsEnabled) return
            if (isActionEvent && !settings.actionHapticsEnabled) return
        }

        // 2. Prevent excessive spam for drag/slider events
        val now = SystemClock.uptimeMillis()
        if (type == HapticType.TINY && now - lastVibrateTimestamp < 40) {
            return
        }
        lastVibrateTimestamp = now

        val intensity = settings?.hapticIntensity ?: HapticIntensity.MEDIUM
        val mult = intensity.multiplier

        // Execute vibration asynchronously so touch dispatch is instant (<0.02ms)
        hapticExecutor.execute {
            if (!isSystemHapticOn()) return@execute
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    when (type) {
                        HapticType.TINY -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasAmplitudeControl) {
                                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                            } else {
                                val amp = (45 * mult).toInt().coerceIn(1, 255)
                                val dur = (10 * mult).toLong().coerceAtLeast(6L)
                                vibrator.vibrate(VibrationEffect.createOneShot(dur, amp))
                            }
                        }

                        HapticType.LIGHT -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasAmplitudeControl) {
                                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                            } else {
                                val amp = (90 * mult).toInt().coerceIn(1, 255)
                                val dur = (18 * mult).toLong().coerceAtLeast(10L)
                                vibrator.vibrate(VibrationEffect.createOneShot(dur, amp))
                            }
                        }

                        HapticType.MEDIUM -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasAmplitudeControl) {
                                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                            } else {
                                val amp = (175 * mult).toInt().coerceIn(1, 255)
                                val dur = (28 * mult).toLong().coerceAtLeast(16L)
                                vibrator.vibrate(VibrationEffect.createOneShot(dur, amp))
                            }
                        }

                        HapticType.LONG_PRESS -> {
                            val amp = (220 * mult).toInt().coerceIn(1, 255)
                            val dur = (42 * mult).toLong().coerceAtLeast(25L)
                            vibrator.vibrate(VibrationEffect.createOneShot(dur, amp))
                        }

                        HapticType.SUCCESS -> {
                            val timings = longArrayOf(0, 14, 50, 22)
                            val amplitudes = intArrayOf(
                                0,
                                (120 * mult).toInt().coerceIn(1, 255),
                                0,
                                (200 * mult).toInt().coerceIn(1, 255)
                            )
                            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                        }

                        HapticType.WARNING -> {
                            val timings = longArrayOf(0, 25, 40, 25)
                            val amplitudes = intArrayOf(
                                0,
                                (180 * mult).toInt().coerceIn(1, 255),
                                0,
                                (180 * mult).toInt().coerceIn(1, 255)
                            )
                            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                        }

                        HapticType.ERROR -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasAmplitudeControl) {
                                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                            } else {
                                val timings = longArrayOf(0, 35, 45, 35)
                                val amplitudes = intArrayOf(
                                    0,
                                    (230 * mult).toInt().coerceIn(1, 255),
                                    0,
                                    (230 * mult).toInt().coerceIn(1, 255)
                                )
                                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val dur = when (type) {
                        HapticType.TINY -> (8 * mult).toLong()
                        HapticType.LIGHT -> (16 * mult).toLong()
                        HapticType.MEDIUM -> (28 * mult).toLong()
                        HapticType.LONG_PRESS -> (45 * mult).toLong()
                        HapticType.SUCCESS -> (35 * mult).toLong()
                        HapticType.WARNING, HapticType.ERROR -> (60 * mult).toLong()
                    }
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(dur)
                }
            } catch (ignored: Exception) {
            }
        }
    }

    companion object {
        @Volatile
        private var instance: HapticManager? = null

        fun getInstance(context: Context): HapticManager {
            return instance ?: synchronized(this) {
                instance ?: HapticManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
