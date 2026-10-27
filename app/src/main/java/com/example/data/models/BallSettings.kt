package com.example.data.models

import com.example.haptics.HapticIntensity

data class BallSettings(
    val isServiceEnabled: Boolean = false,
    val floatingShape: FloatingShape = FloatingShape.PILL_BAR,
    val menuStyle: MenuStyle = MenuStyle.GRID_PANEL,
    val radialCenterShape: FloatingShape = FloatingShape.PILL_BAR,
    val radialAccentColor: Long = 0xFF3B82F6L,
    val ballSizeDp: Int = 54,
    val ballOpacity: Float = 0.85f,
    val edgeSnapping: Boolean = false, // false allows free movement anywhere on the entire screen!
    val autoFadeOnIdle: Boolean = true,
    val idleFadeOpacity: Float = 0.35f,
    val hapticFeedback: Boolean = true, // Master Haptics switch
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val actionHapticsEnabled: Boolean = true,
    val dragHapticsEnabled: Boolean = true,
    val actions: List<ActionType> = ActionType.defaultShortcuts(),
    val customIcons: Map<String, String> = emptyMap(),
    val quickApps: List<String> = emptyList(),
    val ballStyle: BallStyle = BallStyle.GRAPHITE_BLUE,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val startOnBoot: Boolean = false,
    val lastPosX: Int = -1,
    val lastPosY: Int = -1
) {
    val hapticsEnabled: Boolean get() = hapticFeedback
}
