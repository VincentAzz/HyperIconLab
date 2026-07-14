package com.capybara.hypericonlab.modules.icon.ui.page.custom.tabs

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.component.BaseItemContainer
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.SwitchWidget
import com.capybara.hypericonlab.core.designsystem.symbol.open_in_full
import com.capybara.hypericonlab.core.designsystem.symbol.refresh
import com.capybara.hypericonlab.core.designsystem.symbol.wallpaper
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.image.BgImageDir
import com.capybara.hypericonlab.core.image.BgImageLoader
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.ImagePickerSheet
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.MaskPickerSheet
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.MaskThumbnail
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.StyleChip
import com.capybara.hypericonlab.modules.icon.ui.page.custom.sections.ColorSourceSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackgroundTab(
    viewModel: com.capybara.hypericonlab.modules.icon.ui.page.custom.IconViewModel,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    liquidGlassBlurRadius: Dp = 24.dp
) {
    val style by viewModel.bgStyle.collectAsStateWithLifecycle()
    val selectedMasks by viewModel.selectedMasks.collectAsStateWithLifecycle()
    val selectedStaticImages by viewModel.selectedStaticImages.collectAsStateWithLifecycle()
    val selectedFillingImages by viewModel.selectedFillingImages.collectAsStateWithLifecycle()
    val imageFilling by viewModel.imageFilling.collectAsStateWithLifecycle()

    var showMaskPicker by remember { mutableStateOf(false) }
    var showStaticImagePicker by remember { mutableStateOf(false) }
    var showFillingImagePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SegmentedColumn(
            title = "样式",
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
        ) {
            item { shape ->
                BaseItemContainer(shape = shape) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StyleChip(
                                label = "无背景",
                                selected = style == "none",
                                onClick = { viewModel.updateConfig { it.copy(bgStyle = "none") } },
                                modifier = Modifier.weight(1f)
                            )
                            StyleChip(
                                label = "纯色",
                                selected = style == "solid",
                                onClick = { viewModel.updateConfig { it.copy(bgStyle = "solid") } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StyleChip(
                                label = "静态图片",
                                selected = style == "static",
                                onClick = { viewModel.updateConfig { it.copy(bgStyle = "static") } },
                                modifier = Modifier.weight(1f)
                            )
                            StyleChip(
                                label = "图片填充",
                                selected = style == "image",
                                onClick = { viewModel.updateConfig { it.copy(bgStyle = "image") } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 形状选择行：纯色和图片填充时显示
            item(
                animatedVisibility = style == "solid" || style == "image",
                topPadding = ListItemDefaults.SegmentedGap,
            ) { shape ->
                BaseItemContainer(shape = shape) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "形状",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedMasks.forEach { mask ->
                                MaskThumbnail(mask = mask)
                            }
                        }
                        TextButton(onClick = { showMaskPicker = true }) {
                            Text("更改")
                        }
                    }
                }
            }

            // 静态图片选择卡片
            item(
                animatedVisibility = style == "static",
                topPadding = ListItemDefaults.SegmentedGap,
            ) { shape ->
                BaseItemContainer(shape = shape) {
                    ImageSelectionRow(
                        title = "静态图片",
                        imageRefs = selectedStaticImages,
                        useCircleClip = false,
                        onPickClick = { showStaticImagePicker = true }
                    )
                }
            }

            // 图片填充选择卡片
            item(
                animatedVisibility = style == "image",
                topPadding = ListItemDefaults.SegmentedGap,
            ) { shape ->
                BaseItemContainer(shape = shape) {
                    ImageSelectionRow(
                        title = "图片填充",
                        imageRefs = selectedFillingImages,
                        useCircleClip = true,
                        onPickClick = { showFillingImagePicker = true }
                    )
                }
            }

            // 图片填充：随机旋转开关
            item(
                animatedVisibility = style == "image",
                topPadding = ListItemDefaults.SegmentedGap,
            ) { shape ->
                BaseItemContainer(shape = shape) {
                    SwitchWidget(
                        icon = AppMaterialSymbols.refresh,
                        title = "随机旋转",
                        description = if (imageFilling.randomRotation) "启用" else "禁用",
                        checked = imageFilling.randomRotation,
                        onCheckedChange = { enabled ->
                            viewModel.updateConfig {
                                it.copy(imageFilling = it.imageFilling.copy(randomRotation = enabled))
                            }
                        }
                    )
                }
            }

            // 图片填充：缩放方式
            item(
                animatedVisibility = style == "image",
                topPadding = ListItemDefaults.SegmentedGap,
            ) { shape ->
                BaseItemContainer(shape = shape) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                imageVector = AppMaterialSymbols.open_in_full,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(24.dp),
                                colorFilter = ColorFilter.tint(
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                "缩放方式",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            StyleChip(
                                label = "缩放",
                                selected = imageFilling.scaleMode == "scale",
                                onClick = {
                                    viewModel.updateConfig {
                                        it.copy(imageFilling = it.imageFilling.copy(scaleMode = "scale"))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StyleChip(
                                label = "裁切",
                                selected = imageFilling.scaleMode == "crop",
                                onClick = {
                                    viewModel.updateConfig {
                                        it.copy(imageFilling = it.imageFilling.copy(scaleMode = "crop"))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 颜色卡片组：纯色时显示，静态图片/图片填充不显示
        if (style == "solid") {
            ColorSourceSection(
                viewModel = viewModel,
                isForeground = false,
                backdrop = backdrop,
                useLiquidGlass = useLiquidGlass,
                liquidGlassBlurRadius = liquidGlassBlurRadius
            )
        }
    }

    if (showMaskPicker) {
        MaskPickerSheet(
            onDismiss = { showMaskPicker = false },
            selectedMasks = selectedMasks,
            onMasksConfirmed = {
                viewModel.updateConfig { c -> c.copy(selectedMasks = it) }; showMaskPicker = false
            },
            backdrop = backdrop,
            useLiquidGlass = useLiquidGlass,
            liquidGlassBlurRadius = liquidGlassBlurRadius
        )
    }

    if (showStaticImagePicker) {
        ImagePickerSheet(
            onDismiss = { showStaticImagePicker = false },
            title = "选择静态图片",
            bgImageDir = BgImageDir.STATIC,
            selectedImages = selectedStaticImages,
            onImagesConfirmed = { images, deletedRefs ->
                viewModel.confirmImageSelection(
                    isStatic = true,
                    images = images,
                    deletedRefs = deletedRefs
                )
                showStaticImagePicker = false
            },
            backdrop = backdrop,
            useLiquidGlass = useLiquidGlass,
            liquidGlassBlurRadius = liquidGlassBlurRadius
        )
    }

    if (showFillingImagePicker) {
        ImagePickerSheet(
            onDismiss = { showFillingImagePicker = false },
            title = "选择图片填充",
            bgImageDir = BgImageDir.FILLING,
            selectedImages = selectedFillingImages,
            onImagesConfirmed = { images, deletedRefs ->
                viewModel.confirmImageSelection(
                    isStatic = false,
                    images = images,
                    deletedRefs = deletedRefs
                )
                showFillingImagePicker = false
            },
            backdrop = backdrop,
            useLiquidGlass = useLiquidGlass,
            liquidGlassBlurRadius = liquidGlassBlurRadius
        )
    }
}

/**
 * 图片选择行：左侧标题 + 中间已选缩略图 + 右侧更改按钮。
 *
 * @param useCircleClip true=圆形裁切（图片填充），false=圆角矩形（静态图片）
 */
@Composable
private fun ImageSelectionRow(
    title: String,
    imageRefs: List<String>,
    useCircleClip: Boolean,
    onPickClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Image(
            imageVector = AppMaterialSymbols.wallpaper,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp),
            colorFilter = ColorFilter.tint(
                MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(end = 12.dp)
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            imageRefs.forEach { ref ->
                ImageThumbnail(ref = ref, useCircleClip = useCircleClip)
            }
        }
        TextButton(onClick = onPickClick) {
            Text("更改")
        }
    }
}

/**
 * 已选图片缩略图预览（32dp）。
 */
@Composable
private fun ImageThumbnail(ref: String, useCircleClip: Boolean) {
    val context = LocalContext.current
    var bitmap by remember(ref) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(ref) {
        bitmap = withContext(Dispatchers.IO) {
            BgImageLoader.loadScaled(context, ref, 128)
        }
    }

    val clipShape = if (useCircleClip) CircleShape else RoundedCornerShape(8.dp)
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = ref,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(clipShape)
        )
    }
}
