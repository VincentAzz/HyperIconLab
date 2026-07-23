package com.capybara.hypericonlab.modules.settings.ui.page.settings.component

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomSheet
import com.capybara.hypericonlab.core.designsystem.component.SheetTitle
import com.capybara.hypericonlab.core.designsystem.symbol.check
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.AppTheme
import com.capybara.hypericonlab.core.designsystem.theme.ExtraLargeRadius
import com.capybara.hypericonlab.core.designsystem.theme.MiuixThemeBridge
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.core.image.SvgProcessor
import com.capybara.hypericonlab.core.mapper.IconMapperEntry
import com.capybara.hypericonlab.core.mapper.IconMapperProcessor
import com.capybara.hypericonlab.core.utils.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

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
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    // 加载全部 mapper 条目与 svgs 目录
    var allEntries by remember { mutableStateOf<List<IconMapperEntry>>(emptyList()) }
    var svgDir by remember { mutableStateOf<File?>(null) }
    var loadState by remember { mutableStateOf(LawniconsLoadState.LOADING) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // 查找解压后的 svgs 目录（lawnicons.zip 运行时解压到 filesDir/lawnicons/）
            val base = File(context.filesDir, LawniconsSheetConstants.LAWNICONS_DIR)
            val dir = ZipUtils.findDirRecursive(base, LawniconsSheetConstants.SVGS_DIR)
            if (dir == null) {
                loadState = LawniconsLoadState.ERROR
                return@withContext
            }
            svgDir = dir
            val entries = try {
                context.assets
                    .open("${LawniconsSheetConstants.MAPPER_ASSET_DIR}/${LawniconsSheetConstants.FULL_MAPPER_FILE}")
                    .use { IconMapperProcessor.parseIconMapperEntries(it) }
            } catch (_: Exception) {
                emptyList()
            }
            allEntries = entries
            loadState =
                if (entries.isEmpty()) LawniconsLoadState.ERROR else LawniconsLoadState.READY
        }
    }

    // 搜索过滤：按应用名或包名模糊匹配
    var searchQuery by remember { mutableStateOf("") }
    val filteredEntries by remember(allEntries) {
        derivedStateOf {
            val q = searchQuery.trim()
            if (q.isEmpty()) allEntries
            else allEntries.filter {
                it.name.contains(q, ignoreCase = true) ||
                        it.packageName.contains(q, ignoreCase = true)
            }
        }
    }

    // SVG 渲染颜色：深色主题用白色，浅色主题用黑色（保持原色）
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
        // Header：居中标题 + 左侧关闭按钮 + 右侧确定按钮（与 MaskPickerSheet 风格一致）
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
                // 右侧确定按钮：浏览场景无选择行为，点击即关闭 sheet
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
            }
        )

        MiuixThemeBridge {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "搜索应用名或包名",
                singleLine = true,
                textStyle = MiuixTheme.textStyles.main.copy(
                    color = MiuixTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 数量提示文本（位于卡片外部）
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
                                "加载中...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(LawniconsSheetConstants.COLUMNS),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = 32.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .navigationBarsPadding()
                            ) {
                                items(
                                    filteredEntries,
                                    key = { "${it.packageName}_${it.drawable}" }
                                ) { entry ->
                                    SvgIconItem(
                                        svgFile = File(dir, "${entry.drawable}.svg"),
                                        fgColorHex = fgColorHex,
                                        modifier = Modifier.size(LawniconsSheetConstants.ICON_CELL_SIZE)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单个 SVG 图标渲染项，LruCache 缓存
 */
@Composable
private fun SvgIconItem(
    svgFile: File,
    fgColorHex: String,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val cacheKey = "${svgFile.name}:$fgColorHex"

    LaunchedEffect(svgFile, fgColorHex) {
        // 命中缓存直接使用
        svgBitmapCache.get(cacheKey)?.let {
            bitmap = it
            return@LaunchedEffect
        }
        // 缓存未命中则 IO 线程渲染
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

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(LawniconsSheetConstants.ICON_DISPLAY_SIZE)
            )
        }
    }
}

private enum class LawniconsLoadState { LOADING, READY, ERROR }

private object LawniconsSheetConstants {
    // 资源目录
    const val LAWNICONS_DIR = "lawnicons"
    const val SVGS_DIR = "svgs"
    const val MAPPER_ASSET_DIR = "icon_mapper"
    const val FULL_MAPPER_FILE = "icon_mapper.xml"

    // 渲染颜色（9 位 ARGB，SvgProcessor 会截取后 6 位作为替换色）
    const val DARK_FG_COLOR = "#FFFFFFFF"
    const val LIGHT_FG_COLOR = "#FF000000"

    // 网格布局
    const val COLUMNS = 4
    val ICON_CELL_SIZE = 56.dp
    val ICON_DISPLAY_SIZE = 40.dp

    // SVG 渲染尺寸（cell 56dp，按 2x 密度渲染 96px 足够清晰）
    const val SVG_RENDER_SIZE = 96

    // LruCache 占用最大内存的比例
    const val CACHE_FRACTION = 16
}
