package com.capybara.hypericonlab.core.designsystem.color.material

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
    MIUIX_DEFAULT_LIGHT,
    MIUIX_DEFAULT_DARK,
    MIUIX_DEFAULT_SYSTEM;

    val usesMiuixDefaultPalette: Boolean
        get() = this == MIUIX_DEFAULT_LIGHT ||
                this == MIUIX_DEFAULT_DARK ||
                this == MIUIX_DEFAULT_SYSTEM

    fun resolveDark(systemDark: Boolean): Boolean = when (this) {
        LIGHT, MIUIX_DEFAULT_LIGHT -> false
        DARK, MIUIX_DEFAULT_DARK -> true
        SYSTEM, MIUIX_DEFAULT_SYSTEM -> systemDark
    }

    companion object {
        fun fromValueOrDefault(value: String) = entries.find { it.name == value } ?: SYSTEM
    }
}