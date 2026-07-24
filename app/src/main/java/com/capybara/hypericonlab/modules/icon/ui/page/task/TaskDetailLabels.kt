package com.capybara.hypericonlab.modules.icon.ui.page.task

import com.capybara.hypericonlab.modules.icon.domain.model.BuildTaskStatus

// 预览图占位文案
fun previewPlaceholder(status: BuildTaskStatus): String = when (status) {
    BuildTaskStatus.PENDING -> "等待开始..."
    BuildTaskStatus.RUNNING -> "构建中..."
    BuildTaskStatus.FAILED -> "构建失败"
    BuildTaskStatus.CANCELLED -> "已取消"
    BuildTaskStatus.SUCCESS -> "预览图加载中..."
}

// 前景样式标签
fun fgStyleLabel(style: String): String = when (style) {
    "line" -> "线条"
    "sticker" -> "贴纸"
    "glass" -> "玻璃"
    "hollow" -> "镂空"
    else -> style
}

// 背景样式标签
fun bgStyleLabel(style: String): String = when (style) {
    "none" -> "无背景"
    "solid" -> "纯色"
    "img_static" -> "静态图片"
    "img_filling" -> "图片填充"
    else -> style
}

// 颜色来源标签
fun colorSourceLabel(source: String): String = when (source) {
    "wallpaper" -> "壁纸"
    "app" -> "应用"
    "preset" -> "预设"
    "ctc" -> "同色系"
    "custom" -> "自定义"
    "black_white" -> "黑白"
    else -> source
}
