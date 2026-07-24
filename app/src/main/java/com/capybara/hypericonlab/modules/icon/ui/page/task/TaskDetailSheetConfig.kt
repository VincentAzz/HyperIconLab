package com.capybara.hypericonlab.modules.icon.ui.page.task

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

// 任务详情 Sheet 布局与尺寸常量，集中声明便于调参
object TaskDetailSheetConfig {
    // 主体内容水平内边距
    val CONTENT_HORIZONTAL_PADDING = 16.dp

    // 主体内容垂直内边距
    val CONTENT_VERTICAL_PADDING = 8.dp

    // section 间距
    val SECTION_SPACING = 12.dp

    // 内容区底部留白：避免最后一个 item 紧贴底部按钮组
    val CONTENT_BOTTOM_SPACING = 8.dp

    // 预览图高度（保持 store preview 1080×640 的 5:3 长宽比近似）
    val PREVIEW_HEIGHT = 200.dp

    // 卡片容器色透明度（与 LogSheet 一致，让 sheet 模糊透出）
    const val CARD_ALPHA = 0.8f

    // 进度卡片内容内边距
    val PROGRESS_CONTENT_PADDING = PaddingValues(16.dp)

    // 进度条上方间距
    val PROGRESS_BAR_TOP_SPACING = 8.dp

    // 进度条百分比文本上方间距
    val PROGRESS_TEXT_TOP_SPACING = 4.dp

    // 进度条高度
    val PROGRESS_HEIGHT = 4.dp

    // 错误信息卡片内容内边距
    val ERROR_CONTENT_PADDING = PaddingValues(16.dp)

    // 底部操作区按钮组内边距
    val BOTTOM_ACTION_PADDING = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

    // 底部按钮间距
    val BOTTOM_BUTTON_SPACING = 12.dp

    // 底部按钮高度（Material3 Button 默认 36dp，此处显式指定避免被压缩）
    val BUTTON_HEIGHT = 48.dp

    // 按钮内图标尺寸
    val BUTTON_ICON_SIZE = 18.dp

    // 按钮内图标与文本间距
    val BUTTON_ICON_TEXT_SPACING = 8.dp

    // Header 圆形按钮容器尺寸
    val HEADER_ICON_SIZE = 40.dp

    // Header 圆形按钮内部图标尺寸
    val HEADER_ICON_INNER_SIZE = 24.dp

    // Header 关闭按钮左侧 padding
    val HEADER_ICON_LEADING_PADDING = 12.dp

    // Header 确认按钮右侧 padding
    val HEADER_ICON_TRAILING_PADDING = 12.dp
}
