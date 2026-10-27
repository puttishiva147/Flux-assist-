package com.example.data.models

enum class MenuStyle(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val description: String
) {
    GRID_PANEL(
        id = "grid_panel",
        displayName = "Glass Grid Panel",
        subtitle = "Structured Rectangular Card",
        description = "Structured glass panel with quick launch dock and multi-column control grid"
    ),
    RADIAL_ORBIT(
        id = "radial_orbit",
        displayName = "Radial Orbit",
        subtitle = "Trigonometric Circular Menu",
        description = "Circular orbital layout expanding around central button using trigonometry angles"
    );

    companion object {
        fun fromId(id: String): MenuStyle {
            return entries.find { it.id == id } ?: GRID_PANEL
        }
    }
}
