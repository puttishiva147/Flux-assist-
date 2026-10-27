package com.example.data.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.R

enum class ActionType(
    val id: String,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val accentColor: Long = 0xFF3B82F6, // Refined system blue accent
    val description: String,
    val requiresAccessibility: Boolean = false,
    val requiresCamera: Boolean = false,
    val requiresWriteSettings: Boolean = false,
    val requiresDeviceAdmin: Boolean = false
) {
    BACK(
        id = "back",
        titleRes = R.string.action_back,
        iconRes = R.drawable.ic_action_back,
        accentColor = 0xFF3B82F6,
        description = "Go back to previous screen in any app",
        requiresAccessibility = true
    ),
    HOME(
        id = "home",
        titleRes = R.string.action_home,
        iconRes = R.drawable.ic_action_home,
        accentColor = 0xFF3B82F6,
        description = "Return to Android Home screen"
    ),
    RECENTS(
        id = "recents",
        titleRes = R.string.action_recents,
        iconRes = R.drawable.ic_action_recents,
        accentColor = 0xFF3B82F6,
        description = "Open recent apps overview",
        requiresAccessibility = true
    ),
    SCREENSHOT(
        id = "screenshot",
        titleRes = R.string.action_screenshot,
        iconRes = R.drawable.ic_action_screenshot,
        accentColor = 0xFF3B82F6,
        description = "Capture clean screenshot immediately",
        requiresAccessibility = true
    ),
    LOCK_SCREEN(
        id = "lock_screen",
        titleRes = R.string.action_lock_screen,
        iconRes = R.drawable.ic_action_lock,
        accentColor = 0xFF3B82F6,
        description = "Lock device screen instantly",
        requiresAccessibility = true
    ),
    VOLUME_UP(
        id = "volume_up",
        titleRes = R.string.action_volume_up,
        iconRes = R.drawable.ic_action_volume_up,
        accentColor = 0xFF3B82F6,
        description = "Increase media & system volume"
    ),
    VOLUME_DOWN(
        id = "volume_down",
        titleRes = R.string.action_volume_down,
        iconRes = R.drawable.ic_action_volume_down,
        accentColor = 0xFF3B82F6,
        description = "Decrease media & system volume"
    ),
    BRIGHTNESS(
        id = "brightness",
        titleRes = R.string.action_brightness,
        iconRes = R.drawable.ic_action_brightness,
        accentColor = 0xFF3B82F6,
        description = "Adjust screen brightness slider",
        requiresWriteSettings = false
    ),
    FLASHLIGHT(
        id = "flashlight",
        titleRes = R.string.action_flashlight,
        iconRes = R.drawable.ic_action_flashlight,
        accentColor = 0xFF3B82F6,
        description = "Toggle rear LED flashlight",
        requiresCamera = true
    ),
    SETTINGS(
        id = "settings",
        titleRes = R.string.action_settings,
        iconRes = R.drawable.ic_action_settings,
        accentColor = 0xFF3B82F6,
        description = "Open AssistX settings & preferences"
    ),
    NOTIFICATIONS(
        id = "notifications",
        titleRes = R.string.action_notifications,
        iconRes = R.drawable.ic_action_notifications,
        accentColor = 0xFF3B82F6,
        description = "Pull down system notification shade",
        requiresAccessibility = true
    ),
    QUICK_SETTINGS(
        id = "quick_settings",
        titleRes = R.string.action_quick_settings,
        iconRes = R.drawable.ic_action_quick_settings,
        accentColor = 0xFF3B82F6,
        description = "Pull down quick settings panel",
        requiresAccessibility = true
    ),
    FREQUENT_APP(
        id = "frequent_app",
        titleRes = R.string.action_frequent_app,
        iconRes = R.drawable.ic_action_frequent,
        accentColor = 0xFF8B5CF6,
        description = "Dynamically launches your most frequently used application"
    );

    companion object {
        fun fromId(id: String): ActionType {
            return entries.find { it.id == id } ?: HOME
        }

        fun defaultShortcuts(): List<ActionType> {
            return listOf(
                BACK,
                HOME,
                RECENTS,
                SCREENSHOT,
                LOCK_SCREEN,
                FLASHLIGHT,
                VOLUME_UP,
                VOLUME_DOWN
            )
        }
    }
}
