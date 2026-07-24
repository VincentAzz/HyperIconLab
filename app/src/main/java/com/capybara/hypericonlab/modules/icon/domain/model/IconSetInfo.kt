package com.capybara.hypericonlab.modules.icon.domain.model

// 图标集
data class IconSetInfo(
    val id: String,
    val label: String,
    val iconCount: Int
) {
    companion object {
        val SUPPORTED_SETS = listOf("full", "filtered", "test")

        fun mapperFileName(id: String): String = when (id) {
            "full" -> "icon_mapper.xml"
            "filtered" -> "icon_mapper_filtered.xml"
            "test" -> "icon_mapper_test.xml"
            else -> "icon_mapper_$id.xml"
        }
    }
}
