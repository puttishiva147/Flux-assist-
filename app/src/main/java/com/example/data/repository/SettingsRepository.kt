package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.models.ActionType
import com.example.data.models.BallSettings
import com.example.data.models.BallStyle
import com.example.data.models.FloatingShape
import com.example.data.models.MenuStyle
import com.example.haptics.HapticIntensity
import com.example.utils.AppHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<BallSettings> = _settingsFlow.asStateFlow()

    fun getSettings(): BallSettings = _settingsFlow.value

    fun updateSettings(newSettings: BallSettings) {
        prefs.edit().apply {
            putBoolean(KEY_SERVICE_ENABLED, newSettings.isServiceEnabled)
            putString(KEY_FLOATING_SHAPE, newSettings.floatingShape.id)
            putString(KEY_MENU_STYLE, newSettings.menuStyle.id)
            putString(KEY_RADIAL_CENTER_SHAPE, newSettings.radialCenterShape.id)
            putLong(KEY_RADIAL_ACCENT_COLOR, newSettings.radialAccentColor)
            putInt(KEY_BALL_SIZE, newSettings.ballSizeDp)
            putFloat(KEY_BALL_OPACITY, newSettings.ballOpacity)
            putBoolean(KEY_EDGE_SNAPPING, newSettings.edgeSnapping)
            putBoolean(KEY_AUTO_FADE, newSettings.autoFadeOnIdle)
            putFloat(KEY_IDLE_OPACITY, newSettings.idleFadeOpacity)
            putBoolean(KEY_HAPTIC, newSettings.hapticFeedback)
            putString(KEY_HAPTIC_INTENSITY, newSettings.hapticIntensity.id)
            putBoolean(KEY_ACTION_HAPTICS, newSettings.actionHapticsEnabled)
            putBoolean(KEY_DRAG_HAPTICS, newSettings.dragHapticsEnabled)
            putString(KEY_BALL_STYLE, newSettings.ballStyle.id)
            putString(KEY_THEME_MODE, newSettings.themeMode)
            putBoolean(KEY_START_ON_BOOT, newSettings.startOnBoot)
            putString(KEY_ACTIONS, newSettings.actions.joinToString(",") { it.id })
            putString(KEY_CUSTOM_ICONS, newSettings.customIcons.entries.joinToString(";") { "${it.key}:${it.value}" })
            putString(KEY_QUICK_APPS, newSettings.quickApps.joinToString(","))
            putInt(KEY_POS_X, newSettings.lastPosX)
            putInt(KEY_POS_Y, newSettings.lastPosY)
            apply()
        }
        _settingsFlow.value = newSettings
    }

    fun setServiceEnabled(enabled: Boolean) {
        val current = _settingsFlow.value
        updateSettings(current.copy(isServiceEnabled = enabled))
    }

    fun savePosition(x: Int, y: Int) {
        prefs.edit().apply {
            putInt(KEY_POS_X, x)
            putInt(KEY_POS_Y, y)
            apply()
        }
        _settingsFlow.value = _settingsFlow.value.copy(lastPosX = x, lastPosY = y)
    }

    fun addQuickApp(packageName: String) {
        val current = _settingsFlow.value.quickApps.toMutableList()
        if (!current.contains(packageName)) {
            current.add(packageName)
            updateSettings(_settingsFlow.value.copy(quickApps = current))
        }
    }

    fun removeQuickApp(packageName: String) {
        val current = _settingsFlow.value.quickApps.toMutableList()
        if (current.remove(packageName)) {
            updateSettings(_settingsFlow.value.copy(quickApps = current))
        }
    }

    fun resetToDefaults() {
        val defaults = BallSettings(
            quickApps = AppHelper.getDefaultQuickApps(context)
        )
        updateSettings(defaults)
    }

    private fun loadSettings(): BallSettings {
        val isEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
        val shapeId = prefs.getString(KEY_FLOATING_SHAPE, FloatingShape.PILL_BAR.id) ?: FloatingShape.PILL_BAR.id
        val menuStyleId = prefs.getString(KEY_MENU_STYLE, MenuStyle.GRID_PANEL.id) ?: MenuStyle.GRID_PANEL.id
        val radialCenterShapeId = prefs.getString(KEY_RADIAL_CENTER_SHAPE, FloatingShape.PILL_BAR.id) ?: FloatingShape.PILL_BAR.id
        val radialAccentColor = prefs.getLong(KEY_RADIAL_ACCENT_COLOR, 0xFF3B82F6L)
        val size = prefs.getInt(KEY_BALL_SIZE, 54)
        val opacity = prefs.getFloat(KEY_BALL_OPACITY, 0.85f)
        val edgeSnapping = prefs.getBoolean(KEY_EDGE_SNAPPING, false)
        val autoFade = prefs.getBoolean(KEY_AUTO_FADE, true)
        val idleOpacity = prefs.getFloat(KEY_IDLE_OPACITY, 0.35f)
        val haptic = prefs.getBoolean(KEY_HAPTIC, true)
        val intensityId = prefs.getString(KEY_HAPTIC_INTENSITY, HapticIntensity.MEDIUM.id) ?: HapticIntensity.MEDIUM.id
        val actionHaptics = prefs.getBoolean(KEY_ACTION_HAPTICS, true)
        val dragHaptics = prefs.getBoolean(KEY_DRAG_HAPTICS, true)
        val styleId = prefs.getString(KEY_BALL_STYLE, BallStyle.CYBER_CYAN.id) ?: BallStyle.CYBER_CYAN.id
        val themeMode = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        val startOnBoot = prefs.getBoolean(KEY_START_ON_BOOT, false)
        val actionsStr = prefs.getString(KEY_ACTIONS, null)
        val quickAppsStr = prefs.getString(KEY_QUICK_APPS, null)
        val posX = prefs.getInt(KEY_POS_X, -1)
        val posY = prefs.getInt(KEY_POS_Y, -1)

        val actions = if (!actionsStr.isNullOrEmpty()) {
            actionsStr.split(",").mapNotNull { id ->
                try {
                    ActionType.fromId(id.trim())
                } catch (e: Exception) {
                    null
                }
            }.ifEmpty { ActionType.defaultShortcuts() }
        } else {
            ActionType.defaultShortcuts()
        }

        val customIconsStr = prefs.getString(KEY_CUSTOM_ICONS, null)
        val customIcons = if (!customIconsStr.isNullOrEmpty()) {
            customIconsStr.split(";").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
            }.toMap()
        } else {
            emptyMap()
        }

        val quickApps = if (quickAppsStr != null) {
            quickAppsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            AppHelper.getDefaultQuickApps(context)
        }

        return BallSettings(
            isServiceEnabled = isEnabled,
            floatingShape = FloatingShape.fromId(shapeId),
            menuStyle = MenuStyle.fromId(menuStyleId),
            radialCenterShape = FloatingShape.fromId(radialCenterShapeId),
            radialAccentColor = radialAccentColor,
            ballSizeDp = size,
            ballOpacity = opacity,
            edgeSnapping = edgeSnapping,
            autoFadeOnIdle = autoFade,
            idleFadeOpacity = idleOpacity,
            hapticFeedback = haptic,
            hapticIntensity = HapticIntensity.fromId(intensityId),
            actionHapticsEnabled = actionHaptics,
            dragHapticsEnabled = dragHaptics,
            actions = actions,
            customIcons = customIcons,
            quickApps = quickApps,
            ballStyle = BallStyle.fromId(styleId),
            themeMode = themeMode,
            startOnBoot = startOnBoot,
            lastPosX = posX,
            lastPosY = posY
        )
    }

    companion object {
        private const val PREFS_NAME = "assistx_settings"
        private const val KEY_SERVICE_ENABLED = "key_service_enabled"
        private const val KEY_FLOATING_SHAPE = "key_floating_shape"
        private const val KEY_MENU_STYLE = "key_menu_style"
        private const val KEY_RADIAL_CENTER_SHAPE = "key_radial_center_shape"
        private const val KEY_RADIAL_ACCENT_COLOR = "key_radial_accent_color"
        private const val KEY_BALL_SIZE = "key_ball_size"
        private const val KEY_BALL_OPACITY = "key_ball_opacity"
        private const val KEY_EDGE_SNAPPING = "key_edge_snapping"
        private const val KEY_AUTO_FADE = "key_auto_fade"
        private const val KEY_IDLE_OPACITY = "key_idle_opacity"
        private const val KEY_HAPTIC = "key_haptic"
        private const val KEY_HAPTIC_INTENSITY = "key_haptic_intensity"
        private const val KEY_ACTION_HAPTICS = "key_action_haptics"
        private const val KEY_DRAG_HAPTICS = "key_drag_haptics"
        private const val KEY_BALL_STYLE = "key_ball_style"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_START_ON_BOOT = "key_start_on_boot"
        private const val KEY_ACTIONS = "key_actions"
        private const val KEY_CUSTOM_ICONS = "key_custom_icons"
        private const val KEY_QUICK_APPS = "key_quick_apps"
        private const val KEY_POS_X = "key_pos_x"
        private const val KEY_POS_Y = "key_pos_y"

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
