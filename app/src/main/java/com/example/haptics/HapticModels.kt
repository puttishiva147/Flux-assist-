package com.example.haptics

enum class HapticType {
    TINY,       // Selection, touch down, slider tick threshold
    LIGHT,      // Button tap, release, menu open
    MEDIUM,     // Confirmation, drop snap, menu close, lock screen
    LONG_PRESS, // Long-press threshold trigger
    SUCCESS,    // Operation completed successfully
    WARNING,    // Action requires attention
    ERROR       // Action failed or permission missing
}

enum class HapticIntensity(val id: String, val displayName: String, val multiplier: Float) {
    LIGHT("light", "Light", 0.6f),
    MEDIUM("medium", "Medium", 1.0f),
    STRONG("strong", "Strong", 1.4f);

    companion object {
        fun fromId(id: String): HapticIntensity {
            return entries.find { it.id == id } ?: MEDIUM
        }
    }
}
