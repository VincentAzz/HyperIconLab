package com.capybara.hypericonlab.modules.icon.ui.page.custom.sections

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.symbol.category
import com.capybara.hypericonlab.core.designsystem.symbol.check
import com.capybara.hypericonlab.core.designsystem.symbol.open_in_full
import com.capybara.hypericonlab.core.designsystem.symbol.refresh
import com.capybara.hypericonlab.core.designsystem.symbol.wallpaper
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.CardCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.PreviewCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape


@Composable
fun PreviewSection(
    bitmap: Bitmap?,
    onPickWallpaper: () -> Unit,
    onRefresh: () -> Unit,
    onExpand: () -> Unit,
    onBuild: () -> Unit,
    onSavePreset: () -> Unit,
    savePresetEnabled: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PreviewSectionConfig.CARD_OUTER_PADDING),
        shape = rememberKyantRoundedRectangleShape(CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        Column {
            // 预览图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PreviewSectionConfig.PREVIEW_INNER_PADDING)
                    .aspectRatio(PreviewSectionConfig.PREVIEW_ASPECT_RATIO)
                    .clip(rememberKyantRoundedRectangleShape(PreviewCornerRadius))
                    .background(Color.Gray)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "预览",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            // 按钮区：左侧（壁纸/刷新/全屏）+ Spacer + 右侧（构建/保存预设）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PreviewSectionConfig.BUTTON_ROW_PADDING),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧组：壁纸 + 刷新 + 全屏
                Row(horizontalArrangement = Arrangement.spacedBy(PreviewSectionConfig.BUTTON_SPACING)) {
                    CircleIconButton(
                        icon = AppMaterialSymbols.wallpaper,
                        contentDescription = "更换壁纸",
                        onClick = onPickWallpaper
                    )
                    CircleIconButton(
                        icon = AppMaterialSymbols.refresh,
                        contentDescription = "刷新",
                        onClick = onRefresh
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 右侧组：构建 + 保存预设
                Row(horizontalArrangement = Arrangement.spacedBy(PreviewSectionConfig.BUTTON_SPACING)) {
                    CircleIconButton(
                        icon = AppMaterialSymbols.open_in_full,
                        contentDescription = "全屏",
                        onClick = onExpand
                    )
                    CircleIconButton(
                        icon = AppMaterialSymbols.category,
                        contentDescription = "保存到预设",
                        onClick = onSavePreset,
                        enabled = savePresetEnabled
                    )
                    CircleIconButton(
                        icon = AppMaterialSymbols.check,
                        contentDescription = "构建",
                        onClick = onBuild
                    )
                }
            }
        }
    }
}

/**
 * 圆形仅图标按钮：36dp 圆形 + secondaryContainer 0.75 透明度背景，与预览区按钮风格统一。
 */
@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val backgroundAlpha =
        if (enabled) PreviewSectionConfig.BUTTON_BG_ALPHA else PreviewSectionConfig.BUTTON_BG_ALPHA_DISABLED
    val tint = if (enabled) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = PreviewSectionConfig.BUTTON_TINT_ALPHA_DISABLED)
    Box(
        modifier = Modifier
            .size(PreviewSectionConfig.BUTTON_SIZE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = backgroundAlpha))
            .clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(PreviewSectionConfig.BUTTON_ICON_SIZE)
        )
    }
}


private object PreviewSectionConfig {
    // 卡片外边距
    val CARD_OUTER_PADDING = PaddingValues(16.dp, 0.dp, 16.dp, 0.dp)

    // 预览图内边距
    val PREVIEW_INNER_PADDING = 6.dp

    // 预览图长宽比（store preview 1080×640 ≈ 1.6875:1）
    const val PREVIEW_ASPECT_RATIO = 1.6f

    // 按钮行内边距
    val BUTTON_ROW_PADDING = PaddingValues(8.dp, 0.dp, 8.dp, 8.dp)

    // 按钮间距
    val BUTTON_SPACING = 12.dp

    // 按钮尺寸（圆形）
    val BUTTON_SIZE = 36.dp

    // 按钮内部图标尺寸
    val BUTTON_ICON_SIZE = 22.dp

    // 按钮背景透明度
    const val BUTTON_BG_ALPHA = 0.75f

    // 禁用态按钮背景透明度
    const val BUTTON_BG_ALPHA_DISABLED = 0.4f

    // 禁用态图标 tint 透明度
    const val BUTTON_TINT_ALPHA_DISABLED = 0.45f
}