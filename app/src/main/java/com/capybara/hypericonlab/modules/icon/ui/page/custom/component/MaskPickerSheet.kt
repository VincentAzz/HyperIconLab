package com.capybara.hypericonlab.modules.icon.ui.page.custom.component

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomSheet
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRow
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRowAlignment
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRowWidthMode
import com.capybara.hypericonlab.core.designsystem.config.CornerRadius
import com.capybara.hypericonlab.core.designsystem.config.ExtraLargeRadius
import com.capybara.hypericonlab.core.designsystem.config.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.core.designsystem.font.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.core.designsystem.symbol.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.symbol.arrow_downward
import com.capybara.hypericonlab.core.designsystem.symbol.check
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.modules.render.image.CustomMaskGenerator
import com.capybara.hypericonlab.modules.render.image.MaskAssetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import kotlin.math.absoluteValue

private val MaskDisplayNames = mapOf(
    "hyperos3" to "HyperOS 3\n(0.281, 0.6)",
    "ios27" to "iOS 27\n(0.258, 0.6)",
    "coloros16" to "ColorOS 16\n(0.223, 0.6)",
    "oneui" to "OneUI",
    "m3_Ghost_ish" to "ghost"

)

private val MaskDisplayOrder = listOf(
    "hyperos3",
    "ios27",
    "coloros16",
    "oneui",
    "m3_round",
    "squircle",
    "super_squircle"
)

private fun Sequence<MaskEntry>.sortedByDisplayOrder(): Sequence<MaskEntry> = sortedWith(
    compareBy({
        MaskDisplayOrder.indexOf(it.name).let { i -> if (i < 0) Int.MAX_VALUE else i }
    }, { it.name })
)

// 形状条目
private data class MaskEntry(val name: String, val isCommon: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaskPickerSheet(
    onDismiss: () -> Unit,
    selectedMasks: List<String>,
    onMasksConfirmed: (List<String>) -> Unit,
    horizontalPadding: Dp = 8.dp,
    bottomPadding: Dp = 4.dp,
    cornerRadius: Dp? = null,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
) {
    val context = LocalContext.current
    // 检测初始是否为自定义形状模式
    val initialIsCustom = selectedMasks.any { CustomMaskGenerator.isCustomMask(it) }
    val initialParams = parseCustomMaskParams(selectedMasks)

    var currentSelection by remember { mutableStateOf(if (initialIsCustom) emptyList() else selectedMasks) }
    var allMasks by remember { mutableStateOf<List<MaskEntry>>(emptyList()) }
    var customCornerRadius by remember { mutableStateOf(initialParams.first) }
    var customSmoothCorner by remember { mutableStateOf(initialParams.second) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val pagerState =
        rememberPagerState(pageCount = { 2 }, initialPage = if (initialIsCustom) 1 else 0)

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

    LaunchedEffect(Unit) {
        val masks = withContext(Dispatchers.IO) {
            context.assets.list("masks")?.asSequence()?.filter { it.endsWith(".png") }
                ?.map { filename ->
                    val withoutExt = filename.removeSuffix(".png")
                    val isCommon = withoutExt.endsWith("_common")
                    val name = withoutExt.removePrefix("mask_").removeSuffix("_common")
                        .removeSuffix("_512")
                    MaskEntry(name = name, isCommon = isCommon)
                }?.sortedByDisplayOrder()?.toList() ?: emptyList()
        }
        allMasks = masks
    }

    val commonMasks = allMasks.filter { it.isCommon }
    val otherMasks = allMasks.filter { !it.isCommon }

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
        // Header
        CenterAlignedTopAppBar(
            title = {
                FloatingTabRow(
                    tabs = listOf("预设", "自定义"),
                    selectedIndex = pagerState.currentPage,
                    onSelected = { index ->
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    barHeight = 40.dp,
                    alignment = FloatingTabRowAlignment.CENTER,
                    widthMode = FloatingTabRowWidthMode.WRAP_CONTENT,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                    indicatorColor = MaterialTheme.colorScheme.primary
                )
            },
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
                val enabled =
                    if (pagerState.currentPage == 0) currentSelection.isNotEmpty() else true
                Surface(
                    onClick = {
                        if (enabled) {
                            val result = if (pagerState.currentPage == 0) {
                                currentSelection
                            } else {
                                listOf(
                                    CustomMaskGenerator.encode(
                                        customCornerRadius,
                                        customSmoothCorner
                                    )
                                )
                            }
                            coroutineScope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onMasksConfirmed(result)
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
            })

        // 禁用 overscroll effect：避免滑动力度大时到达边界页触发回弹抖动
        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 8.dp
            ) { page ->
                val pageOffset = (
                        (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        ).absoluteValue
                val fadeAlpha = (1f - pageOffset * 0.4f).coerceIn(0.75f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.alpha = fadeAlpha }
                ) {
                    when (page) {
                        0 -> PresetMaskPage(
                            gridState = gridState,
                            commonMasks = commonMasks,
                            otherMasks = otherMasks,
                            currentSelection = currentSelection,
                            showScrollHint = showScrollHint,
                            onMaskClick = { maskName ->
                                currentSelection = if (maskName in currentSelection) {
                                    currentSelection - maskName
                                } else {
                                    if (currentSelection.size < 5) currentSelection + maskName else currentSelection
                                }
                            }
                        )

                        1 -> CustomMaskTab(
                            cornerRadius = customCornerRadius,
                            smoothCorner = customSmoothCorner,
                            onCornerRadiusChange = { customCornerRadius = it },
                            onSmoothCornerChange = { customSmoothCorner = it }
                        )
                    }
                }
            }
        }
    }
}

// 预设形状页：常用形状 + 其他形状的网格列表
@Composable
private fun PresetMaskPage(
    gridState: LazyGridState,
    commonMasks: List<MaskEntry>,
    otherMasks: List<MaskEntry>,
    currentSelection: List<String>,
    showScrollHint: Boolean,
    onMaskClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "最多5个，多选形状将加入随机池进行生成",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item(span = { GridItemSpan(4) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "常用形状 (${commonMasks.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "(圆角比例, 平滑圆角比例)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(commonMasks) { entry ->
                    MaskItem(
                        name = entry.name,
                        isSelected = entry.name in currentSelection,
                        onClick = { onMaskClick(entry.name) }
                    )
                }

                item(span = { GridItemSpan(4) }) {
                    Text(
                        "其他形状 (${otherMasks.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(otherMasks) { entry ->
                    MaskItem(
                        name = entry.name,
                        isSelected = entry.name in currentSelection,
                        onClick = { onMaskClick(entry.name) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MaskItem(
    name: String, isSelected: Boolean, onClick: () -> Unit
) {
    val context = LocalContext.current

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(name) {
        withContext(Dispatchers.IO) {
            bitmap = MaskAssetLoader.loadBitmap(context, name)
        }
    }

    // Animate shape and colors
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) CornerRadius else ExtraLargeRadius,
        animationSpec = tween(durationMillis = 400),
        label = "cornerRadius"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }, animationSpec = tween(durationMillis = 400), label = "containerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }, animationSpec = tween(durationMillis = 400), label = "contentColor"
    )

    val shape = rememberKyantRoundedRectangleShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = onClick,
                indication = null,
                interactionSource = interactionSource
            )
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(shape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = name,
                    modifier = Modifier.size(36.dp),
                    colorFilter = ColorFilter.tint(contentColor)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = MaskDisplayNames[name] ?: name.replace("m3_", "").replace("_", " "),
            fontSize = 10.sp,
            lineHeight = 16.sp,
            fontFamily = GoogleSansCodeFontFamily,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
