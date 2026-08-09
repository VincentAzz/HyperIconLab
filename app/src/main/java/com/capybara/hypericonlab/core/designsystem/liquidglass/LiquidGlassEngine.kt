package com.capybara.hypericonlab.core.designsystem.liquidglass

enum class LiquidGlassEngine {
    // miuix-blur
    MIUIX,

    // kyant
    KYANT;

    companion object {
        fun fromValueOrDefault(value: String): LiquidGlassEngine =
            entries.find { it.name == value } ?: KYANT
    }
}
