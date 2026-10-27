package com.example.actions

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import com.example.MainActivity
import com.example.data.models.ActionType
import com.example.data.repository.SettingsRepository
import com.example.haptics.HapticManager
import com.example.haptics.HapticType
import com.example.receivers.AssistXDeviceAdminReceiver
import com.example.service.AssistXAccessibilityService

class ActionExecutor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val hapticManager = HapticManager.getInstance(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        @Volatile
        var isFlashlightOn: Boolean = false
            private set
    }

    fun execute(
        action: ActionType,
        onShowBrightnessSlider: (() -> Unit)? = null,
        onCloseMenu: () -> Unit
    ) {
        val settings = SettingsRepository.getInstance(context).getSettings()

        // Semantic haptic feedback
        val hapticType = when (action) {
            ActionType.LOCK_SCREEN -> HapticType.MEDIUM
            ActionType.SCREENSHOT -> HapticType.SUCCESS
            else -> HapticType.LIGHT
        }
        hapticManager.performHaptic(hapticType, settings, isActionEvent = true)

        // For volume adjustments, preserve the menu open so users can tap +/- repeatedly
        if (action != ActionType.VOLUME_UP && action != ActionType.VOLUME_DOWN) {
            onCloseMenu()
        }

        when (action) {
            ActionType.BACK -> handleBack()
            ActionType.HOME -> handleHome()
            ActionType.RECENTS -> handleRecents()
            ActionType.SCREENSHOT -> handleScreenshot()
            ActionType.LOCK_SCREEN -> handleLockScreen()
            ActionType.VOLUME_UP -> handleVolumeUp()
            ActionType.VOLUME_DOWN -> handleVolumeDown()
            ActionType.BRIGHTNESS -> {
                if (onShowBrightnessSlider != null) {
                    onShowBrightnessSlider()
                } else {
                    openDisplaySettings()
                }
            }
            ActionType.FLASHLIGHT -> handleFlashlight()
            ActionType.SETTINGS -> openSettings()
            ActionType.NOTIFICATIONS -> handleNotifications()
            ActionType.QUICK_SETTINGS -> handleQuickSettings()
            ActionType.FREQUENT_APP -> handleFrequentApp()
        }
    }

    private fun handleFrequentApp() {
        val app = com.example.utils.AppUsageTracker.getMostFrequentlyUsedApp(context)
        if (app != null) {
            com.example.utils.AppUsageTracker.recordAppLaunch(context, app.packageName)
            val launched = com.example.utils.AppHelper.launchApp(context, app.packageName)
            if (!launched) {
                Toast.makeText(context, "Could not launch ${app.appName}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No frequently used app identified yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleBack() {
        if (AssistXAccessibilityService.isRunning()) {
            // Post delayed: allows WindowManager to detach overlay window and refocus the foreground app
            mainHandler.postDelayed({
                val success = AssistXAccessibilityService.performBack()
                if (!success) {
                    showAccessibilityPrompt("Could not execute Back navigation. Please verify Accessibility permissions.")
                }
            }, 80L)
        } else {
            showAccessibilityPrompt("Accessibility Service is required to perform Back navigation.")
        }
    }

    private fun handleHome() {
        if (AssistXAccessibilityService.isRunning()) {
            mainHandler.postDelayed({
                val success = AssistXAccessibilityService.performHome()
                if (!success) {
                    fallbackHomeIntent()
                }
            }, 60L)
        } else {
            fallbackHomeIntent()
        }
    }

    private fun fallbackHomeIntent() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(homeIntent)
        } catch (e: Exception) {
            showAccessibilityPrompt("Unable to return Home. Please enable Accessibility Service.")
        }
    }

    private fun handleRecents() {
        if (AssistXAccessibilityService.isRunning()) {
            mainHandler.postDelayed({
                val success = AssistXAccessibilityService.performRecents()
                if (!success) {
                    showAccessibilityPrompt("Could not open Recent Apps.")
                }
            }, 80L)
        } else {
            showAccessibilityPrompt("Accessibility Service is required for Recent Apps overview.")
        }
    }

    private fun handleScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (AssistXAccessibilityService.isRunning()) {
                // Wait 220ms so overlay card is completely dismissed from screen before capturing
                mainHandler.postDelayed({
                    val success = AssistXAccessibilityService.performScreenshot()
                    if (success) {
                        Toast.makeText(context, "Screenshot captured", Toast.LENGTH_SHORT).show()
                    } else {
                        showAccessibilityPrompt("Screenshot failed. Please verify Accessibility permissions.")
                    }
                }, 220L)
            } else {
                showAccessibilityPrompt("Accessibility Service is required to capture Screenshots.")
            }
        } else {
            Toast.makeText(context, "Screenshot requires Android 9.0 or higher", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && AssistXAccessibilityService.isRunning()) {
            mainHandler.postDelayed({
                val success = AssistXAccessibilityService.performLockScreen()
                if (!success) {
                    if (AssistXDeviceAdminReceiver.isAdminActive(context)) {
                        AssistXDeviceAdminReceiver.lockNow(context)
                    } else {
                        showAccessibilityPrompt("Unable to lock screen. Please check Accessibility permissions.")
                    }
                }
            }, 80L)
        } else if (AssistXDeviceAdminReceiver.isAdminActive(context)) {
            AssistXDeviceAdminReceiver.lockNow(context)
        } else {
            showAccessibilityPrompt("Enable Accessibility Service or Device Admin to Lock Screen.")
        }
    }

    private fun handleVolumeUp() {
        try {
            audioManager?.adjustSuggestedStreamVolume(
                AudioManager.ADJUST_RAISE,
                AudioManager.USE_DEFAULT_STREAM_TYPE,
                AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
            )
        } catch (e: Exception) {
            try {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
                )
            } catch (ignored: Exception) {
                Toast.makeText(context, "Volume control unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleVolumeDown() {
        try {
            audioManager?.adjustSuggestedStreamVolume(
                AudioManager.ADJUST_LOWER,
                AudioManager.USE_DEFAULT_STREAM_TYPE,
                AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
            )
        } catch (e: Exception) {
            try {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
                )
            } catch (ignored: Exception) {
                Toast.makeText(context, "Volume control unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleFlashlight() {
        val cm = cameraManager ?: run {
            Toast.makeText(context, "Flashlight unavailable: Camera service not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val characteristics = cm.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraId != null) {
                isFlashlightOn = !isFlashlightOn
                cm.setTorchMode(cameraId, isFlashlightOn)
                val status = if (isFlashlightOn) "ON" else "OFF"
                Toast.makeText(context, "Flashlight $status", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Flashlight not supported on this device", Toast.LENGTH_SHORT).show()
            }
        } catch (e: CameraAccessException) {
            Toast.makeText(context, "Flashlight access error: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Flashlight error: ${e.localizedMessage ?: "Hardware in use"}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleNotifications() {
        if (AssistXAccessibilityService.isRunning()) {
            mainHandler.postDelayed({
                AssistXAccessibilityService.performNotifications()
            }, 80L)
        } else {
            showAccessibilityPrompt("Accessibility Service is required to expand Notifications.")
        }
    }

    private fun handleQuickSettings() {
        if (AssistXAccessibilityService.isRunning()) {
            mainHandler.postDelayed({
                AssistXAccessibilityService.performQuickSettings()
            }, 80L)
        } else {
            showAccessibilityPrompt("Accessibility Service is required to open Quick Settings.")
        }
    }

    private fun openSettings() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    private fun openDisplaySettings() {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open display settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAccessibilityPrompt(message: String) {
        val settings = SettingsRepository.getInstance(context).getSettings()
        hapticManager.performHaptic(HapticType.WARNING, settings)
        Toast.makeText(context, "$message Tap to enable in Settings.", Toast.LENGTH_LONG).show()

        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (ignored: Exception) {
        }
    }
}
