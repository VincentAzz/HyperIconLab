package com.capybara.hypericonlab.modules.settings.ui.page.settings.component

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomSheet
import com.capybara.hypericonlab.core.designsystem.component.SheetTitle
import com.capybara.hypericonlab.core.designsystem.symbol.check
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.core.designsystem.symbol.search
import com.capybara.hypericonlab.core.designsystem.symbol.search_off
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.AppTheme
import com.capybara.hypericonlab.core.designsystem.theme.ExtraLargeRadius
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.core.designsystem.theme.MiuixThemeBridge
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.core.image.SvgProcessor
import com.capybara.hypericonlab.core.mapper.IconMapperEntry
import com.capybara.hypericonlab.core.mapper.IconMapperProcessor
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsResourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

private val svgBitmapCache: LruCache<String, Bitmap> by lazy {
    val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    object : LruCache<String, Bitmap>(maxMemory / LawniconsSheetConstants.CACHE_FRACTION) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawniconsBrowserSheet(
    onDismiss: () -> Unit,
    horizontalPadding: Dp = 8.dp,
    bottomPadding: Dp = 4.dp,
    cornerRadius: Dp = ExtraLargeRadius,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    liquidGlassBlurRadius: Dp = 24.dp,
) {
    val resourceManager = koinInject<LawniconsResourceManager>()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // 加载全部 mapper 条目与 svgs 目录
    var allEntries by remember { mutableStateOf<List<IconMapperEntry>>(emptyList()) }
    var svgDir by remember { mutableStateOf<File?>(null) }
    var loadState by remember { mutableStateOf(LawniconsLoadState.LOADING) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // 通过 resourceManager 获取当前激活资源的 svgs 目录与 mapper
            val provider = resourceManager.getProvider()
            val dir = provider.getSvgDir()
            if (dir == null) {
                loadState = LawniconsLoadState.ERROR
                return@withContext
            }
            svgDir = dir
            val entries = try {
                provider.openIconMapper(LawniconsSheetConstants.FULL_MAPPER_FILE)
                    .use { IconMapperProcessor.parseIconMapperEntries(it) }
            } catch (_: Exception) {
                emptyList()
            }
            allEntries = entries
            loadState =
                if (entries.isEmpty()) LawniconsLoadState.ERROR else LawniconsLoadState.READY
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var selectedEntry by remember { mutableStateOf<IconMapperEntry?>(null) }
    var displayedEntry by remember { mutableStateOf<IconMapperEntry?>(null) }

    var needsPadding by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current
    val isScrolling by remember { derivedStateOf { gridState.isScrollInProgress } }

    val filteredEntries by remember(allEntries) {
        derivedStateOf {
            val q = searchQuery.trim()
            if (q.isEmpty()) allEntries
            else allEntries.filter {
                it.name.contains(q, ignoreCase = true) || it.packageName.contains(
                    q,
                    ignoreCase = true
                )
            }
        }
    }


    val isDark = AppTheme.isDark
    val fgColorHex = if (isDark) {
        LawniconsSheetConstants.DARK_FG_COLOR
    } else {
        LawniconsSheetConstants.LIGHT_FG_COLOR
    }

    FloatingBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        horizontalPadding = horizontalPadding,
        bottomPadding = bottomPadding,
        cornerRadius = cornerRadius,
        backdrop = backdrop,
        useLiquidGlass = useLiquidGlass,
        liquidGlassBlurRadius = liquidGlassBlurRadius,
    ) {
        CenterAlignedTopAppBar(
            title = { SheetTitle("浏览原始图标") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            navigationIcon = {
                Surface(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
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
                Surface(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    },
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
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            })

        AnimatedVisibility(visible = isSearchActive) {
            MiuixThemeBridge {
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        selectedEntry = null
                        needsPadding = false
                    },
                    label = "搜索应用名或包名",
                    singleLine = true,
                    textStyle = MiuixTheme.textStyles.main.copy(
                        color = MiuixTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .focusRequester(focusRequester)
                )
            }
        }

        Text(
            text = "${filteredEntries.size} 个图标",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp, 0.dp, 8.dp, 8.dp),
            shape = rememberKyantRoundedRectangleShape(ExtraLargeRadius - 8.dp),
            color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.8f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (loadState) {
                    LawniconsLoadState.LOADING -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "加载中...", color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LawniconsLoadState.ERROR -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "资源未就绪，请重启应用后重试",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LawniconsLoadState.READY -> {
                        val dir = svgDir
                        if (filteredEntries.isEmpty() || dir == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "未找到匹配的图标",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            val gridTopPadding by animateDpAsState(
                                targetValue = if (needsPadding && selectedEntry != null) LawniconsSheetConstants.GRID_TOP_PADDING + LawniconsSheetConstants.BANNER_HEIGHT + LawniconsSheetConstants.BANNER_BOTTOM_GAP
                                else LawniconsSheetConstants.GRID_TOP_PADDING,
                                label = "gridTopPadding"
                            )
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(LawniconsSheetConstants.COLUMNS),
                                contentPadding = PaddingValues(
                                    start = 16.dp, end = 16.dp, top = gridTopPadding, bottom = 32.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .navigationBarsPadding()
                            ) {
                                items(
                                    filteredEntries,
                                    key = { "${it.packageName}_${it.drawable}" }) { entry ->
                                    val isSelected = selectedEntry?.let {
                                        it.packageName == entry.packageName && it.drawable == entry.drawable
                                    } ?: false
                                    SvgIconItem(
                                        svgFile = File(dir, "${entry.drawable}.svg"),
                                        fgColorHex = fgColorHex,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (isSelected) {
                                                selectedEntry = null
                                                needsPadding = false
                                            } else {
                                                val index = filteredEntries.indexOfFirst {
                                                    it.packageName == entry.packageName && it.drawable == entry.drawable
                                                }
                                                needsPadding = if (index >= 0) {
                                                    val itemInfo =
                                                        gridState.layoutInfo.visibleItemsInfo.find { it.index == index }
                                                    if (itemInfo != null) {
                                                        val thresholdPx = with(density) {
                                                            (LawniconsSheetConstants.BANNER_HEIGHT + LawniconsSheetConstants.BANNER_BOTTOM_GAP + LawniconsSheetConstants.ICON_CELL_SIZE / 2).roundToPx()
                                                        }
                                                        itemInfo.offset.y < thresholdPx
                                                    } else false
                                                } else false
                                                displayedEntry = entry
                                                selectedEntry = entry
                                            }
                                        },
                                        modifier = Modifier.size(LawniconsSheetConstants.ICON_CELL_SIZE)
                                    )
                                }
                            }
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedEntry != null,
                    enter = slideInVertically { -it },
                    exit = slideOutVertically { -it },
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    displayedEntry?.let { entry ->
                        svgDir?.let { dir ->
                            IconInfoBanner(
                                entry = entry,
                                svgDir = dir,
                                fgColorHex = fgColorHex,
                            )
                        }
                    }
                }

                // 搜索 FAB：右下角，滚动时隐藏
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isScrolling,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .navigationBarsPadding()
                ) {
                    FloatingActionButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (isSearchActive) {
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(200.milliseconds)
                                    focusRequester.requestFocus()
                                }
                            } else {
                                searchQuery = ""
                                keyboardController?.hide()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) AppMaterialSymbols.search_off else AppMaterialSymbols.search,
                            contentDescription = if (isSearchActive) "取消搜索" else "搜索",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun SvgIconItem(
    svgFile: File,
    fgColorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val cacheKey = "${svgFile.name}:$fgColorHex"

    LaunchedEffect(svgFile, fgColorHex) {
        svgBitmapCache.get(cacheKey)?.let {
            bitmap = it
            return@LaunchedEffect
        }
        if (svgFile.exists()) {
            val rendered = withContext(Dispatchers.IO) {
                SvgProcessor.processSvgFile(
                    svgFile = svgFile,
                    fgColorHex = fgColorHex,
                    iconSize = LawniconsSheetConstants.SVG_RENDER_SIZE
                )
            }
            if (rendered != null) {
                svgBitmapCache.put(cacheKey, rendered)
                bitmap = rendered
            }
        }
    }

    val itemShape = rememberKyantRoundedRectangleShape(LawniconsSheetConstants.ITEM_CORNER_RADIUS)

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
        else Color.Transparent,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "selectionColor"
    )

    Box(
        modifier = modifier
            .clip(itemShape)
            .background(backgroundColor)
            .clickable(onClick = onClick), contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(LawniconsSheetConstants.ICON_DISPLAY_SIZE)
            )
        }
    }
}


@Composable
private fun IconInfoBanner(
    entry: IconMapperEntry, svgDir: File, fgColorHex: String, modifier: Modifier = Modifier
) {
    val bannerShape = RoundedCornerShape(
        topStart = LawniconsSheetConstants.BANNER_TOP_RADIUS,
        topEnd = LawniconsSheetConstants.BANNER_TOP_RADIUS,
        bottomEnd = 0.dp,
        bottomStart = 0.dp
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(bannerShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InfoCardIcon(
            svgFile = File(svgDir, "${entry.drawable}.svg"), fgColorHex = fgColorHex
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.drawable,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = GoogleSansCodeFontFamily,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = entry.packageName,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = GoogleSansCodeFontFamily,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}


@Composable
private fun InfoCardIcon(
    svgFile: File, fgColorHex: String
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val cacheKey = "${svgFile.name}:$fgColorHex"

    LaunchedEffect(svgFile, fgColorHex) {
        svgBitmapCache.get(cacheKey)?.let {
            bitmap = it
            return@LaunchedEffect
        }
        if (svgFile.exists()) {
            val rendered = withContext(Dispatchers.IO) {
                SvgProcessor.processSvgFile(
                    svgFile = svgFile,
                    fgColorHex = fgColorHex,
                    iconSize = LawniconsSheetConstants.SVG_RENDER_SIZE
                )
            }
            if (rendered != null) {
                svgBitmapCache.put(cacheKey, rendered)
                bitmap = rendered
            }
        }
    }

    Box(
        modifier = Modifier.size(LawniconsSheetConstants.INFO_ICON_DISPLAY_SIZE),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(LawniconsSheetConstants.INFO_ICON_DISPLAY_SIZE)
            )
        }
    }
}

private enum class LawniconsLoadState { LOADING, READY, ERROR }

private object LawniconsSheetConstants {
    const val FULL_MAPPER_FILE = "icon_mapper.xml"

    const val DARK_FG_COLOR = "#FFFFFFFF"
    const val LIGHT_FG_COLOR = "#FF000000"

    // 网格布局
    const val COLUMNS = 4
    val ICON_CELL_SIZE = 56.dp
    val ICON_DISPLAY_SIZE = 40.dp
    val GRID_TOP_PADDING = 16.dp

    // 信息横幅与选中项
    val INFO_ICON_DISPLAY_SIZE = 48.dp
    val ITEM_CORNER_RADIUS = 16.dp
    val BANNER_TOP_RADIUS = ExtraLargeRadius - 8.dp
    val BANNER_HEIGHT = 72.dp

    // 横幅底部与首行图标的间距
    val BANNER_BOTTOM_GAP = 8.dp

    const val SVG_RENDER_SIZE = 96

    const val CACHE_FRACTION = 16
}
