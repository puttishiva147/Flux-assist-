package com.example.data.models

enum class BallStyle(
    val id: String,
    val displayName: String,
    val primaryColor: Long,
    val glowColor: Long,
    val innerCoreColor: Long,
    val ringColor: Long,
    val menuBgColor: Long = 0xF214171E,
    val menuBorderColor: Long = 0x403B82F6,
    val glassHighlightColor: Long = 0x1AFFFFFF
) {
    GRAPHITE_BLUE(
        id = "graphite_blue",
        displayName = "Graphite Blue",
        primaryColor = 0xFF3B82F6,
        glowColor = 0x333B82F6,
        innerCoreColor = 0xFF161920,
        ringColor = 0xFF333D4F,
        menuBgColor = 0xF414171E,
        menuBorderColor = 0x4D3B82F6,
        glassHighlightColor = 0x1EFFFFFF
    ),
    TITANIUM_CHARCOAL(
        id = "titanium_charcoal",
        displayName = "Titanium Charcoal",
        primaryColor = 0xFF60A5FA,
        glowColor = 0x2660A5FA,
        innerCoreColor = 0xFF14171D,
        ringColor = 0xFF36404F,
        menuBgColor = 0xF4111318,
        menuBorderColor = 0x3360A5FA,
        glassHighlightColor = 0x18FFFFFF
    ),
    STEEL_MONOCHROME(
        id = "steel_monochrome",
        displayName = "Steel Slate",
        primaryColor = 0xFF94A3B8,
        glowColor = 0x2094A3B8,
        innerCoreColor = 0xFF1A1F29,
        ringColor = 0xFF475569,
        menuBgColor = 0xF4151921,
        menuBorderColor = 0x3394A3B8,
        glassHighlightColor = 0x1AFFFFFF
    ),
    MIDNIGHT_OBSIDIAN(
        id = "midnight_obsidian",
        displayName = "Deep Obsidian",
        primaryColor = 0xFF3B82F6,
        glowColor = 0x223B82F6,
        innerCoreColor = 0xFF0D0F13,
        ringColor = 0xFF232A36,
        menuBgColor = 0xF80A0C0F,
        menuBorderColor = 0x333B82F6,
        glassHighlightColor = 0x12FFFFFF
    ),
    FROSTED_CRYSTAL(
        id = "frosted_crystal",
        displayName = "Frosted Graphite",
        primaryColor = 0xFF38BDF8,
        glowColor = 0x2838BDF8,
        innerCoreColor = 0xFF1E2430,
        ringColor = 0xFF48566D,
        menuBgColor = 0xF4161B24,
        menuBorderColor = 0x3838BDF8,
        glassHighlightColor = 0x20FFFFFF
    ),
    CYBER_CYAN(
        id = "cyber_cyan",
        displayName = "Sapphire Core",
        primaryColor = 0xFF2563EB,
        glowColor = 0x332563EB,
        innerCoreColor = 0xFF151922,
        ringColor = 0xFF313F54,
        menuBgColor = 0xF413161D,
        menuBorderColor = 0x402563EB,
        glassHighlightColor = 0x1AFFFFFF
    );

    companion object {
        fun fromId(id: String): BallStyle {
            return entries.find { it.id == id } ?: GRAPHITE_BLUE
        }
    }
}
