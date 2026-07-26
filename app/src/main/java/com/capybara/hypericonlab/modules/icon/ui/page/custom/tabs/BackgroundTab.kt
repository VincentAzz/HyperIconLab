package com.capybara.hypericonlab.modules.icon.ui.page.custom.tabs

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.component.BaseItemContainer
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.PrimaryActionButton
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.SliderWidget
import com.capybara.hypericonlab.core.image.BgImageDir
import com.capybara.hypericonlab.core.image.BgImageLoader
import com.capybara.hypericonlab.modules.icon.domain.model.BgLayerUiState
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.ImagePickerSheet
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.MaskPickerSheet
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.MaskThumbnail
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.StyleChip
import com.capybara.hypericonlab.modules.icon.ui.page.custom.sections.ColorSourceSection
import com.capybara.hypericonlab.modules.icon.viewmodel.IconViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackgroundTab(
    viewModel: IconViewModel,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    liquidGlassBlurRadius: Dp = 24.dp
) {
    val style by viewModel.bgStyle.collectAsStateWithLifecycle()
    val selectedMasks by viewModel.selectedMasks.collectAsStateWithLifecycle()
    val selectedStaticImages by viewModel.selectedStaticImages.collectAsStateWithLifecycle()
    val selectedFillingImages by viewModel.selectedFillingImages.collectAsStateWithLifecycle()
    val imageFilling by viewModel.imageFilling.collectAsStateWithLifecycle()
    // 双层背景相关状态
    val dualLayerEnabled by viewModel.dualLayerEnabled.collectAsStateWithLifecycle()
    val dualLayerSizeDiff by viewModel.dualLayerSizeDiff.collectAsStateWithLifecycle()
    val bgLayer2 by viewModel.bgLayer2.collectAsStateWithLifecycle()

    var showMaskPicker by remember { mutableStateOf(false) }
    var showStaticImagePicker by remember { mutableStateOf(false) }
    var showFillingImagePicker by remember { mutableStateOf(false) }
    // 下层背景独立弹窗状态
    var showMaskPicker2 by remember { mutableStateOf(false) }
    var showStaticImagePicker2 by remember { mutableStateOf(false) }
    var showFillingImagePicker2 by remember { mutableStateOf(false) }

    Column {
        // 双层背景开关卡片组
        SegmentedColumn(
            title = "双层背景",
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
        ) {
            item { shape ->
                BaseItemContainer(shape = shape) {
                    BaseWidget(
                        icon = null,
                        iconPlaceholder = false,
                        title = "启用双层背景",
                        trailingContent = {
                            Switch(
                                checked = dualLayerEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.updateConfig { it.copy(dualLayerEnabled = enabled) }
                                }
                            )
                        }
                    )
                }
            }
            // 大小差异滑块：双层启用时显示
            item(
                animatedVisibility = dualLayerEnabled,
                topPadding = ListItemDefaults.SegmentedGap
            ) { shape ->
                SliderWidget(
                    title = "大小差异",
                    value = dualLayerSizeDiff,
                    onValueChange = { v ->
                        viewModel.updateConfig { it.copy(dualLayerSizeDiff = v) }
                    },
                    valueRange = DualLayerUiConstants.SIZE_DIFF_MIN..DualLayerUiConstants.SIZE_DIFF_MAX,
                    steps = DualLayerUiConstants.SIZE_DIFF_STEPS,
                    valueDisplay = "${(dualLayerSizeDiff * 100).toInt()}%",
                    shape = shape
                )
            }
        }

        SegmentedColumn(
            title = if (dualLayerEnabled) "上层背景样式" else "样式",
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
        ) {
            item { shape ->
                BaseItemContainer(shape = shape) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 双层启用时不显示"无背景"chip（联动规则 3.3 已强制切回 solid），静态图片前移填补
                            if (!dualLayerEnabled) {
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
                            } else {
                                StyleChip(
                                    label = "纯色",
                                    selected = style == "solid",
                                    onClick = { viewModel.updateConfig { it.copy(bgStyle = "solid") } },
                                    modifier = Modifier.weight(1f)
                                )
                                StyleChip(
                                    label = "静态图片",
                                    selected = style == "img_static",
                                    onClick = { viewModel.updateConfig { it.copy(bgStyle = "img_static") } },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!dualLayerEnabled) {
                                StyleChip(
                                    label = "静态图片",
                                    selected = style == "img_static",
                                    onClick = { viewModel.updateConfig { it.copy(bgStyle = "img_static") } },
                                    modifier = Modifier.weight(1f)
                                )
                                StyleChip(
                                    label = "图片填充",
                                    selected = style == "img_filling",
                                    onClick = { viewModel.updateConfig { it.copy(bgStyle = "img_filling") } },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                StyleChip(
                                    label = "图片填充",
                                    selected = style == "img_filling",
                                    onClick = { viewModel.updateConfig { it.copy(bgStyle = "img_filling") } },
                                    modifier = Modifier.weight(1f)
                                )
                                // 末位空缺用 Spacer 占位，保持两列对齐
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 形状选择行：纯色和图片填充时显示
            item(
                animatedVisibility = style == "solid" || style == "img_filling",
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
                        PrimaryActionButton(
                            text = "更改",
                            onClick = { showMaskPicker = true }
                        )
                    }
                }
            }

            // 静态图片选择卡片
            item(
                animatedVisibility = style == "img_static",
                topPadding = ListItemDefaults.SegmentedGap,
            ) { shape ->
                BaseItemContainer(shape = shape) {
                    ImageSelectionRow(
                        title = "静态图片",
                        imageRefs = selectedStaticImages,
                        onPickClick = { showStaticImagePicker = true }
                    )
                }
            }

            // 图片填充选择卡片
            item(
                animatedVisibility = style == "img_filling",
                topPadding = ListItemDefaults.SegmentedGap,
            ) { shape ->
                BaseItemContainer(shape = shape) {
                    ImageSelectionRow(
                        title = "图片填充",
                        imageRefs = selectedFillingImages,
                        onPickClick = { showFillingImagePicker = true }
                    )
                }
            }

            // 图片填充：随机旋转开关
            item(
                animatedVisibility = style == "img_filling",
                topPadding = ListItemDefaults.SegmentedGap,
            ) { shape ->
                BaseItemContainer(shape = shape) {
                    BaseWidget(
                        icon = null,
                        iconPlaceholder = false,
                        title = "随机旋转",
                        trailingContent = {
                            Switch(
                                checked = imageFilling.randomRotation,
                                onCheckedChange = { enabled ->
                                    viewModel.updateConfig {
                                        it.copy(imageFilling = it.imageFilling.copy(randomRotation = enabled))
                                    }
                                }
                            )
                        }
                    )
                }
            }

            // 图片填充：缩放方式
            item(
                animatedVisibility = style == "img_filling",
                topPadding = ListItemDefaults.SegmentedGap,
            ) { shape ->
                BaseItemContainer(shape = shape) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
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

        // 上层背景颜色卡片组：纯色时显示，静态图片/图片填充不显示
        if (style == "solid") {
            ColorSourceSection(
                viewModel = viewModel,
                isForeground = false,
                backdrop = backdrop,
                useLiquidGlass = useLiquidGlass,
                liquidGlassBlurRadius = liquidGlassBlurRadius
            )
        }

        // 下层背景卡片组：双层启用时显示
        if (dualLayerEnabled) {
            LowerLayerBackgroundSection(
                viewModel = viewModel,
                bgLayer2 = bgLayer2,
                onPickMask = { showMaskPicker2 = true },
                onPickStaticImage = { showStaticImagePicker2 = true },
                onPickFillingImage = { showFillingImagePicker2 = true },
                backdrop = backdrop,
                useLiquidGlass = useLiquidGlass,
                liquidGlassBlurRadius = liquidGlassBlurRadius
            )
        }
    }

    // 上层弹窗
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
            onImagesConfirmed = { images ->
                viewModel.confirmImageSelection(
                    isStatic = true,
                    images = images,
                    layerIndex = 0
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
            onImagesConfirmed = { images ->
                viewModel.confirmImageSelection(
                    isStatic = false,
                    images = images,
                    layerIndex = 0
                )
                showFillingImagePicker = false
            },
            backdrop = backdrop,
            useLiquidGlass = useLiquidGlass,
            liquidGlassBlurRadius = liquidGlassBlurRadius
        )
    }

    // 下层弹窗
    if (showMaskPicker2) {
        MaskPickerSheet(
            onDismiss = { showMaskPicker2 = false },
            selectedMasks = bgLayer2.selectedMasks,
            onMasksConfirmed = {
                viewModel.updateConfig { c ->
                    c.copy(bgLayer2 = c.bgLayer2.copy(selectedMasks = it))
                }
                showMaskPicker2 = false
            },
            backdrop = backdrop,
            useLiquidGlass = useLiquidGlass,
            liquidGlassBlurRadius = liquidGlassBlurRadius
        )
    }

    if (showStaticImagePicker2) {
        ImagePickerSheet(
            onDismiss = { showStaticImagePicker2 = false },
            title = "选择下层静态图片",
            bgImageDir = BgImageDir.STATIC,
            selectedImages = bgLayer2.selectedStaticImages,
            onImagesConfirmed = { images ->
                viewModel.confirmImageSelection(
                    isStatic = true,
                    images = images,
                    layerIndex = 1
                )
                showStaticImagePicker2 = false
            },
            backdrop = backdrop,
            useLiquidGlass = useLiquidGlass,
            liquidGlassBlurRadius = liquidGlassBlurRadius
        )
    }

    if (showFillingImagePicker2) {
        ImagePickerSheet(
            onDismiss = { showFillingImagePicker2 = false },
            title = "选择下层图片填充",
            bgImageDir = BgImageDir.FILLING,
            selectedImages = bgLayer2.selectedFillingImages,
            onImagesConfirmed = { images ->
                viewModel.confirmImageSelection(
                    isStatic = false,
                    images = images,
                    layerIndex = 1
                )
                showFillingImagePicker2 = false
            },
            backdrop = backdrop,
            useLiquidGlass = useLiquidGlass,
            liquidGlassBlurRadius = liquidGlassBlurRadius
        )
    }
}

/**
 * 下层背景卡片组：样式 SegmentedColumn + 颜色卡片组。
 * 双层启用时显示，与上层卡片组结构一致，但读写 bgLayer2 字段。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LowerLayerBackgroundSection(
    viewModel: IconViewModel,
    bgLayer2: BgLayerUiState,
    onPickMask: () -> Unit,
    onPickStaticImage: () -> Unit,
    onPickFillingImage: () -> Unit,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    liquidGlassBlurRadius: Dp = 24.dp
) {
    val style2 = bgLayer2.style
    val imageFilling2 = bgLayer2.imageFilling

    SegmentedColumn(
        title = "下层背景样式",
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
    ) {
        item { shape ->
            BaseItemContainer(shape = shape) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 下层不显示"无背景"chip（下层无背景无意义），静态图片前移填补
                        StyleChip(
                            label = "纯色",
                            selected = style2 == "solid",
                            onClick = {
                                viewModel.updateConfig {
                                    it.copy(bgLayer2 = it.bgLayer2.copy(style = "solid"))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StyleChip(
                            label = "静态图片",
                            selected = style2 == "img_static",
                            onClick = {
                                viewModel.updateConfig {
                                    it.copy(bgLayer2 = it.bgLayer2.copy(style = "img_static"))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyleChip(
                            label = "图片填充",
                            selected = style2 == "img_filling",
                            onClick = {
                                viewModel.updateConfig {
                                    it.copy(bgLayer2 = it.bgLayer2.copy(style = "img_filling"))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        // 末位空缺用 Spacer 占位，保持两列对齐
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 形状选择行：纯色和图片填充时显示
        item(
            animatedVisibility = style2 == "solid" || style2 == "img_filling",
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
                        bgLayer2.selectedMasks.forEach { mask ->
                            MaskThumbnail(mask = mask)
                        }
                    }
                    PrimaryActionButton(
                        text = "更改",
                        onClick = onPickMask
                    )
                }
            }
        }

        // 静态图片选择卡片
        item(
            animatedVisibility = style2 == "img_static",
            topPadding = ListItemDefaults.SegmentedGap,
        ) { shape ->
            BaseItemContainer(shape = shape) {
                ImageSelectionRow(
                    title = "静态图片",
                    imageRefs = bgLayer2.selectedStaticImages,
                    onPickClick = onPickStaticImage
                )
            }
        }

        // 图片填充选择卡片
        item(
            animatedVisibility = style2 == "img_filling",
            topPadding = ListItemDefaults.SegmentedGap,
        ) { shape ->
            BaseItemContainer(shape = shape) {
                ImageSelectionRow(
                    title = "图片填充",
                    imageRefs = bgLayer2.selectedFillingImages,
                    onPickClick = onPickFillingImage
                )
            }
        }

        // 图片填充：随机旋转开关
        item(
            animatedVisibility = style2 == "img_filling",
            topPadding = ListItemDefaults.SegmentedGap,
        ) { shape ->
            BaseItemContainer(shape = shape) {
                BaseWidget(
                    icon = null,
                    iconPlaceholder = false,
                    title = "随机旋转",
                    trailingContent = {
                        Switch(
                            checked = imageFilling2.randomRotation,
                            onCheckedChange = { enabled ->
                                viewModel.updateConfig {
                                    it.copy(
                                        bgLayer2 = it.bgLayer2.copy(
                                            imageFilling = it.bgLayer2.imageFilling.copy(
                                                randomRotation = enabled
                                            )
                                        )
                                    )
                                }
                            }
                        )
                    }
                )
            }
        }

        // 图片填充：缩放方式
        item(
            animatedVisibility = style2 == "img_filling",
            topPadding = ListItemDefaults.SegmentedGap,
        ) { shape ->
            BaseItemContainer(shape = shape) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    StyleChip(
                        label = "缩放",
                        selected = imageFilling2.scaleMode == "scale",
                        onClick = {
                            viewModel.updateConfig {
                                it.copy(
                                    bgLayer2 = it.bgLayer2.copy(
                                        imageFilling = it.bgLayer2.imageFilling.copy(scaleMode = "scale")
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StyleChip(
                        label = "裁切",
                        selected = imageFilling2.scaleMode == "crop",
                        onClick = {
                            viewModel.updateConfig {
                                it.copy(
                                    bgLayer2 = it.bgLayer2.copy(
                                        imageFilling = it.bgLayer2.imageFilling.copy(scaleMode = "crop")
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // 下层背景颜色卡片组：纯色时显示
    if (style2 == "solid") {
        ColorSourceSection(
            viewModel = viewModel,
            isForeground = false,
            layerIndex = 1,
            backdrop = backdrop,
            useLiquidGlass = useLiquidGlass,
            liquidGlassBlurRadius = liquidGlassBlurRadius
        )
    }
}

/**
 * 双层背景 UI 常量（避免硬编码）。
 */
private object DualLayerUiConstants {
    const val SIZE_DIFF_MIN = 0.0f
    const val SIZE_DIFF_MAX = 0.3f

    // 0.02 步进 → 15 档 → steps = 14
    const val SIZE_DIFF_STEPS = 14
}

/**
 * 图片选择行：左侧标题 + 中间已选缩略图 + 右侧更改按钮（无图标，避免拥挤）。
 */
@Composable
private fun ImageSelectionRow(
    title: String,
    imageRefs: List<String>,
    onPickClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
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
                ImageThumbnail(ref = ref)
            }
        }
        PrimaryActionButton(
            text = "更改",
            onClick = onPickClick
        )
    }
}

/**
 * 已选图片缩略图预览（32dp，圆形裁切）。
 */
@Composable
private fun ImageThumbnail(ref: String) {
    val context = LocalContext.current
    var bitmap by remember(ref) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(ref) {
        bitmap = withContext(Dispatchers.IO) {
            BgImageLoader.loadScaled(context, ref, 128)
        }
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = ref,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )
    }
}
