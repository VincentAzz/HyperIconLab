package com.capybara.hypericonlab.core.designsystem.theme

enum class FloatingBottomBarCompactType(
    val displayName: String
) {
    MIXED_ICON("混合-图标"),
    MIXED_TEXT("混合-文本"),
    ICON_ONLY("仅图标"),
    TEXT_ONLY("仅文本");

    companion object {
        fun fromValueOrDefault(value: String) =
            entries.find { it.name == value } ?: MIXED_ICON
    }
}