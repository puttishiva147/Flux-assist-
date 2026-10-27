package com.example.data.models

import androidx.annotation.DrawableRes
import com.example.R

data class IconOption(
    val id: String,
    val name: String,
    @DrawableRes val iconRes: Int
)

object ShortcutIconRegistry {
    val AVAILABLE_ICONS = listOf(
        IconOption("default", "Action Default", R.drawable.ic_action_settings),
        IconOption("star", "Star", R.drawable.ic_action_frequent),
        IconOption("bolt", "Lightning", R.drawable.ic_custom_bolt),
        IconOption("rocket", "Rocket", R.drawable.ic_custom_rocket),
        IconOption("flame", "Flame", R.drawable.ic_custom_flame),
        IconOption("shield", "Shield", R.drawable.ic_custom_shield),
        IconOption("diamond", "Diamond", R.drawable.ic_custom_diamond),
        IconOption("heart", "Heart", R.drawable.ic_custom_heart),
        IconOption("crown", "Crown", R.drawable.ic_custom_crown),
        IconOption("sparkle", "Sparkles", R.drawable.ic_custom_sparkle),
        IconOption("target", "Target", R.drawable.ic_custom_target),
        IconOption("sys_home", "Home", R.drawable.ic_action_home),
        IconOption("sys_back", "Back", R.drawable.ic_action_back),
        IconOption("sys_recents", "Recents", R.drawable.ic_action_recents),
        IconOption("sys_screenshot", "Camera", R.drawable.ic_action_screenshot),
        IconOption("sys_lock", "Lock", R.drawable.ic_action_lock),
        IconOption("sys_flashlight", "Torch", R.drawable.ic_action_flashlight),
        IconOption("sys_vol_up", "Vol +", R.drawable.ic_action_volume_up),
        IconOption("sys_vol_down", "Vol -", R.drawable.ic_action_volume_down),
        IconOption("sys_brightness", "Brightness", R.drawable.ic_action_brightness),
        IconOption("sys_notif", "Bell", R.drawable.ic_action_notifications),
        IconOption("sys_quick", "Sliders", R.drawable.ic_action_quick_settings)
    )

    fun getIconRes(customIconId: String?, defaultRes: Int): Int {
        if (customIconId.isNullOrEmpty() || customIconId == "default") return defaultRes
        return AVAILABLE_ICONS.firstOrNull { it.id == customIconId }?.iconRes ?: defaultRes
    }
}
