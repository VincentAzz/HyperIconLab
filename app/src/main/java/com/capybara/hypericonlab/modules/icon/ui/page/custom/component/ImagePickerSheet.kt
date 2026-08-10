package com.capybara.hypericonlab.modules.icon.ui.page.custom.component

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomSheet
import com.capybara.hypericonlab.core.designsystem.component.SheetTitle
import com.capybara.hypericonlab.core.designsystem.symbol.add
import com.capybara.hypericonlab.core.designsystem.symbol.arrow_downward
import com.capybara.hypericonlab.core.designsystem.symbol.check
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.CornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.modules.render.image.BgImageDir
import com.capybara.hypericonlab.modules.render.image.BgImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.blur.LayerBackdrop

/**
 * 背景图片选择 Sheet。
 *
 * 参考 MaskPickerSheet 结构，分为两个 section：
 * - 从相册选取：首项为加号按钮，后续为自选图片池（扫描 filesDir 持久化目录）
 * - 预设：assets 中的预设图片，直接展示（圆角矩形描边框选）
 *
 * 自选图片池与选中状态分离：
 * - 相册选取：仅入池，不自动选中（保留先前的选取结果）
 * - 点击图片：切换选中/非选中态（自选与预设一致），最多 5 个
 * - 长按自选图片：立即删除磁盘文件并从池中移除；长按不删除预设
 * - 自选图片持久化于 filesDir，应用重启后仍保留
 *
 * @param bgImageDir 图片目录类型（STATIC 或 FILLING）
 * @param selectedImages 已选图片引用列表
 * @param onImagesConfirmed 确认回调，返回新的选中引用列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerSheet(
    onDismiss: () -> Unit,
    title: String,
    bgImageDir: BgImageDir,
    selectedImages: List<String>,
    onImagesConfirmed: (List<String>) -> Unit,
    horizontalPadding: Dp = 8.dp,
    bottomPadding: Dp = 4.dp,
    cornerRadius: Dp? = null,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
) {
    val context = LocalContext.current
    var currentSelection by remember { mutableStateOf(selectedImages) }
    var customPool by remember { mutableStateOf<List<String>>(emptyList()) }
    var presetAssets by remember { mutableStateOf<List<String>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    val isAtTop by remember {
        derivedStateOf {
            (gridState.firstVisibleItemIndex == 0) && (gridState.firstVisibleItemScrollOffset == 0)
        }
    }
    val showScrollHint by remember {
        derivedStateOf {
            isAtTop && gridState.canScrollForward
        }
    }

    // 加载预设图片列表 + 扫描自选图片池
    LaunchedEffect(Unit) {
        presetAssets = withContext(Dispatchers.IO) {
            BgImageLoader.listPresetAssets(context, bgImageDir)
        }
        customPool = withContext(Dispatchers.IO) {
            BgImageLoader.listCustomFiles(context, bgImageDir)
        }
        // 过滤掉选中列表中已不存在的自选图片引用
        val validCustomRefs = customPool.toSet()
        currentSelection = currentSelection.filter { ref ->
            !ref.startsWith("file:") || ref in validCustomRefs
        }
    }

    // 相册选取 launcher：仅入池，不自动选中
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val ref = withContext(Dispatchers.IO) {
                    BgImageLoader.saveFromUri(context, uri, bgImageDir)
                }
                if (ref != null) {
                    customPool = customPool + ref
                }
            }
        }
    }

    FloatingBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        horizontalPadding = horizontalPadding,
        bottomPadding = bottomPadding,
        cornerRadius = cornerRadius,
        backdrop = backdrop,
        useLiquidGlass = useLiquidGlass,
    ) {
        CenterAlignedTopAppBar(
            title = { SheetTitle(title) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            navigationIcon = {
                Surface(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            AppMaterialSymbols.close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            },
            actions = {
                val enabled = currentSelection.isNotEmpty()
                Surface(
                    onClick = {
                        if (enabled) {
                            coroutineScope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onImagesConfirmed(currentSelection)
                                }
                            }
                        }
                    },
                    enabled = enabled,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            AppMaterialSymbols.check,
                            contentDescription = "确定",
                            modifier = Modifier
                                .size(24.dp)
                                .alpha(if (enabled) 1f else 0.38f),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "最多5个，多选图片将加入随机池进行生成",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(modifier = Modifier.weight(1f, fill = false)) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Section: 从相册选取
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            "从相册选取 (${customPool.size})，长按删除",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 加号按钮
                    item {
                        AddImageButton(
                            enabled = true,
                            onClick = { imagePicker.launch("image/*") }
                        )
                    }

                    // 自选图片项：点击切换选中态，长按删除磁盘文件
                    items(
                        items = customPool,
                        key = { it }
                    ) { ref ->
                        val isSelected = ref in currentSelection
                        ImageGridItem(
                            ref = ref,
                            isSelected = isSelected,
                            showName = false,
                            clipImage = bgImageDir == BgImageDir.FILLING,
                            onShortClick = {
                                // 点击切换选中态（与预设一致）
                                currentSelection = if (isSelected) {
                                    currentSelection - ref
                                } else {
                                    if (currentSelection.size < 5) currentSelection + ref else currentSelection
                                }
                            },
                            onLongClick = {
                                // 长按删除：立即清理磁盘文件，从池和选中列表移除
                                BgImageLoader.deleteCustomFile(context, ref)
                                customPool = customPool - ref
                                currentSelection = currentSelection - ref
                            }
                        )
                    }

                    // Section: 预设
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            "预设 (${presetAssets.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(
                        items = presetAssets,
                        key = { it }
                    ) { ref ->
                        val isSelected = ref in currentSelection
                        ImageGridItem(
                            ref = ref,
                            isSelected = isSelected,
                            showName = true,
                            clipImage = bgImageDir == BgImageDir.FILLING,
                            onShortClick = {
                                currentSelection = if (isSelected) {
                                    currentSelection - ref
                                } else {
                                    if (currentSelection.size < 5) currentSelection + ref else currentSelection
                                }
                            },
                            onLongClick = {
                                currentSelection = if (isSelected) {
                                    currentSelection - ref
                                } else {
                                    if (currentSelection.size < 5) currentSelection + ref else currentSelection
                                }
                            }
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollHint,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = AppMaterialSymbols.arrow_downward,
                                contentDescription = "向下滚动",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun AddImageButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val cornerShape = rememberKyantRoundedRectangleShape(CornerRadius)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(
            enabled = enabled,
            onClick = onClick,
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        )
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(cornerShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppMaterialSymbols.add,
                contentDescription = "选择图片",
                modifier = Modifier
                    .size(24.dp)
                    .alpha(if (enabled) 1f else 0.38f),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ImageGridItem(
    ref: String,
    isSelected: Boolean,
    showName: Boolean,
    clipImage: Boolean,
    onShortClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(ref) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(ref) {
        bitmap = withContext(Dispatchers.IO) {
            BgImageLoader.loadScaled(context, ref, 256)
        }
    }

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp
    val cornerShape = rememberKyantRoundedRectangleShape(CornerRadius)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = onShortClick,
                onLongClick = onLongClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(cornerShape)
                .border(borderWidth, borderColor, cornerShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = ref,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (clipImage) Modifier.clip(cornerShape) else Modifier)
                )
            }
        }
        if (showName) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = BgImageLoader.refToDisplayName(ref),
                fontSize = 10.sp,
                lineHeight = 16.sp,
                fontFamily = GoogleSansCodeFontFamily,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
