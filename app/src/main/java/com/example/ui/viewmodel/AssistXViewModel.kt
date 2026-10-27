package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.ActionType
import com.example.data.models.BallSettings
import com.example.data.models.BallStyle
import com.example.data.models.FloatingShape
import com.example.data.repository.SettingsRepository
import com.example.haptics.HapticIntensity
import com.example.haptics.HapticManager
import com.example.haptics.HapticType
import com.example.receivers.AssistXDeviceAdminReceiver
import com.example.service.AssistXAccessibilityService
import com.example.service.FloatingBallService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PermissionStates(
    val hasOverlay: Boolean = false,
    val hasAccessibility: Boolean = false,
    val hasNotification: Boolean = false,
    val hasWriteSettings: Boolean = false,
    val hasDeviceAdmin: Boolean = false
)

class AssistXViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository.getInstance(application)
    val settings: StateFlow<BallSettings> = repository.settingsFlow

    private val hapticManager = HapticManager.getInstance(application)

    private val _permissions = MutableStateFlow(checkPermissions())
    val permissions: StateFlow<PermissionStates> = _permissions.asStateFlow()

    fun refreshPermissions() {
        _permissions.value = checkPermissions()
    }

    private fun checkPermissions(): PermissionStates {
        val context = getApplication<Application>()
        val hasOverlay = Settings.canDrawOverlays(context)
        val hasAccessibility = AssistXAccessibilityService.isAccessibilityEnabled(context)
        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val hasWriteSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }
        val hasDeviceAdmin = AssistXDeviceAdminReceiver.isAdminActive(context)

        return PermissionStates(
            hasOverlay = hasOverlay,
            hasAccessibility = hasAccessibility,
            hasNotification = hasNotification,
            hasWriteSettings = hasWriteSettings,
            hasDeviceAdmin = hasDeviceAdmin
        )
    }

    fun toggleService(enable: Boolean) {
        val context = getApplication<Application>()
        if (enable) {
            if (Settings.canDrawOverlays(context)) {
                repository.setServiceEnabled(true)
                FloatingBallService.start(context)
            }
        } else {
            repository.setServiceEnabled(false)
            FloatingBallService.stop(context)
        }
    }

    fun updateFloatingShape(shape: FloatingShape) {
        val current = settings.value
        val updated = current.copy(floatingShape = shape)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
    }

    fun updateMenuStyle(style: com.example.data.models.MenuStyle) {
        val current = settings.value
        val updated = current.copy(menuStyle = style)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
    }

    fun updateRadialCenterShape(shape: FloatingShape) {
        val current = settings.value
        val updated = current.copy(radialCenterShape = shape)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
    }

    fun updateRadialAccentColor(color: Long) {
        val current = settings.value
        val updated = current.copy(radialAccentColor = color)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
    }

    fun updateCustomIconForAction(actionId: String, iconId: String) {
        val current = settings.value
        val updatedMap = current.customIcons.toMutableMap()
        if (iconId == "default" || iconId.isEmpty()) {
            updatedMap.remove(actionId)
        } else {
            updatedMap[actionId] = iconId
        }
        val updated = current.copy(customIcons = updatedMap)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
    }

    fun updateBallSize(sizeDp: Int) {
        val current = settings.value
        val updated = current.copy(ballSizeDp = sizeDp)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
    }

    fun updateBallOpacity(opacity: Float) {
        val current = settings.value
        val updated = current.copy(ballOpacity = opacity)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
    }

    fun updateEdgeSnapping(enabled: Boolean) {
        val current = settings.value
        val updated = current.copy(edgeSnapping = enabled)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
    }

    fun updateAutoFade(enabled: Boolean) {
        val current = settings.value
        val updated = current.copy(autoFadeOnIdle = enabled)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
    }

    fun updateIdleOpacity(opacity: Float) {
        val current = settings.value
        val updated = current.copy(idleFadeOpacity = opacity)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
    }

    fun updateHaptic(enabled: Boolean) {
        val current = settings.value
        val updated = current.copy(hapticFeedback = enabled)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
    }

    fun updateHapticIntensity(intensity: HapticIntensity) {
        val current = settings.value
        val updated = current.copy(hapticIntensity = intensity)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
        testHaptic(HapticType.LIGHT)
    }

    fun updateActionHaptics(enabled: Boolean) {
        val current = settings.value
        val updated = current.copy(actionHapticsEnabled = enabled)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
    }

    fun updateDragHaptics(enabled: Boolean) {
        val current = settings.value
        val updated = current.copy(dragHapticsEnabled = enabled)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
    }

    fun testHaptic(type: HapticType) {
        hapticManager.performHaptic(type, settings.value)
    }

    fun updateBallStyle(style: BallStyle) {
        val current = settings.value
        val updated = current.copy(ballStyle = style)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
    }

    fun updateThemeMode(mode: String) {
        val current = settings.value
        val updated = current.copy(themeMode = mode)
        repository.updateSettings(updated)
    }

    fun updateStartOnBoot(enabled: Boolean) {
        val current = settings.value
        val updated = current.copy(startOnBoot = enabled)
        repository.updateSettings(updated)
    }

    fun addQuickApp(packageName: String) {
        repository.addQuickApp(packageName)
        FloatingBallService.updateSettings(getApplication())
    }

    fun removeQuickApp(packageName: String) {
        repository.removeQuickApp(packageName)
        FloatingBallService.updateSettings(getApplication())
    }

    fun updateActionSlot(index: Int, newAction: ActionType) {
        val currentActions = settings.value.actions.toMutableList()
        if (index in currentActions.indices) {
            currentActions[index] = newAction
            val updated = settings.value.copy(actions = currentActions)
            repository.updateSettings(updated)
            FloatingBallService.updateSettings(getApplication())
        }
    }

    fun setActionCount(count: Int) {
        val allDefaults = ActionType.entries
        val current = settings.value.actions.toMutableList()
        val newActions = if (current.size < count) {
            for (action in allDefaults) {
                if (current.size >= count) break
                if (!current.contains(action)) {
                    current.add(action)
                }
            }
            while (current.size < count) {
                current.add(ActionType.HOME)
            }
            current
        } else {
            current.take(count)
        }
        val updated = settings.value.copy(actions = newActions)
        repository.updateSettings(updated)
        FloatingBallService.updateSettings(getApplication())
    }

    fun resetMenuDefaults() {
        repository.resetToDefaults()
        FloatingBallService.updateSettings(getApplication())
    }
}
