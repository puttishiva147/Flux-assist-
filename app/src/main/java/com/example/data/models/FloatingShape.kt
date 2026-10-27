package com.example.data.models

enum class FloatingShape(val id: String, val displayName: String) {
    SPHERE("sphere", "Floating Orb"),
    PILL_BAR("pill_bar", "Floating Bar");

    companion object {
        fun fromId(id: String): FloatingShape {
            return entries.find { it.id == id } ?: PILL_BAR
        }
    }
}
