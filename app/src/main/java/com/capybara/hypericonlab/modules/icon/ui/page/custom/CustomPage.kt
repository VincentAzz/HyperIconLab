package com.capybara.hypericonlab.modules.icon.ui.page.custom

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRow
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRowAlignment
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRowWidthMode
import com.capybara.hypericonlab.core.designsystem.liquidglass.appBarBlurEffect
import com.capybara.hypericonlab.core.designsystem.liquidglass.getMaterial3AppBarColor
import com.capybara.hypericonlab.core.designsystem.liquidglass.rememberMaterial3BlurBackdrop
import com.capybara.hypericonlab.core.designsystem.theme.CornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.isSmootherRoundedCornersEnabled
import com.capybara.hypericonlab.core.designsystem.theme.kyantUnevenRoundedShape
import com.capybara.hypericonlab.modules.build.domain.model.ProductType
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.IconPackTemplateState
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsAssetFacade
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.ResourceSource
import com.capybara.hypericonlab.modules.icon.domain.model.IconSetInfo
import com.capybara.hypericonlab.modules.icon.domain.model.toPrettyString
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.BuildOptionSheet
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.FullScreenPreviewSheet
import com.capybara.hypericonlab.modules.icon.ui.page.custom.sections.PreviewSection
import com.capybara.hypericonlab.modules.icon.ui.page.custom.tabs.BackgroundTab
import com.capybara.hypericonlab.modules.icon.ui.page.custom.tabs.BorderTab
import com.capybara.hypericonlab.modules.icon.ui.page.custom.tabs.ForegroundTab
import com.capybara.hypericonlab.modules.icon.viewmodel.IconViewModel
import com.capybara.hypericonlab.modules.settings.ui.page.settings.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomPage(
    modifier: Modifier = Modifier,
    viewModel: IconViewModel = koinViewModel(),
    themeViewModel: SettingsViewModel = koinViewModel(),
    outerPadding: PaddingValues = PaddingValues(0.dp),
    windowInsetsSides: WindowInsetsSides? = null,
) {
    val storePreviewBitmap by viewModel.storePreviewBitmap.collectAsStateWithLifecycle()
    val mainPreviewBitmap by viewModel.mainPreviewBitmap.collectAsStateWithLifecycle()
    val isPreviewLoading by viewModel.isPreviewLoading.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val themeState by themeViewModel.state.collectAsStateWithLifecycle()
    val availableIconSets by viewModel.availableIconSets.collectAsStateWithLifecycle()
    val assetsFacade = koinInject<LawniconsAssetFacade>()
    val lawniconsVersion by assetsFacade.currentVersion.collectAsStateWithLifecycle()
    val templateState by assetsFacade.templateState.collectAsStateWithLifecycle()
    val apkEnabled = lawniconsVersion.source == ResourceSource.REMOTE &&
            (templateState as? IconPackTemplateState.Available)?.version == lawniconsVersion.version

    var showFullScreenPreview by remember { mutableStateOf(false) }
    // 全屏预览 sheet 打开时快照一次当前 IconBuildConfig 文本，避免 sheet 期间配置变化导致参数与预览图不一致
    var previewConfigText by remember { mutableStateOf("") }
    var showBuildSheet by remember { mutableStateOf(false) }

    // Snackbar：提交构建任务后提示用户去任务 tab 查看进度
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.updateWallpaperFromUri(uri)
            }
        }
    )

    // 权限申请 launcher（方案 C：首次提交任务兜底申请）
    // 每次申请一个权限，系统会保持已授予状态；多权限按 missing 列表顺序逐个申请
    var pendingPermissionQueue by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingBuildArgs by remember {
        mutableStateOf<Pair<ProductType, IconSetInfo>?>(
            null
        )
    }

    // 用于在 launcher 回调内引用自身（递归申请下一个权限）
    val permissionLauncherRef =
        remember { mutableStateOf<androidx.activity.result.ActivityResultLauncher<String>?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // 申请完成后尝试消费队列中的下一个权限
        val next = pendingPermissionQueue.drop(1)
        pendingPermissionQueue = next
        if (next.isNotEmpty()) {
            permissionLauncherRef.value?.launch(next.first())
        } else {
            // 全部权限处理完成，提交缓存的构建任务
            pendingBuildArgs?.let { (productType, iconSet) ->
                val submitted = viewModel.submitBuildTask(productType, iconSet.id, iconSet.label)
                if (submitted != null) {
                    // 弹出 Snackbar 提示用户去任务 tab 查看进度
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "已提交构建任务，可在任务 tab 中查看构建进度",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
            pendingBuildArgs = null
        }
    }
    // 启动时绑定引用
    androidx.compose.runtime.LaunchedEffect(Unit) {
        permissionLauncherRef.value = permissionLauncher
    }

    val scrollBehavior =
        TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val backdrop = rememberMaterial3BlurBackdrop(themeState.useBlur)

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = windowInsetsSides?.let { ScaffoldDefaults.contentWindowInsets.only(it) }
            ?: ScaffoldDefaults.contentWindowInsets,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = outerPadding.calculateBottomPadding() + 8.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        topBar = {
            Box {
                TopAppBar(
                    modifier = Modifier.appBarBlurEffect(
                        backdrop = backdrop,
                        useProgressiveBlur = themeState.useProgressiveBlurTopAppBar
                    ),
                    windowInsets = windowInsetsSides?.let { TopAppBarDefaults.windowInsets.only(it) }
                        ?: TopAppBarDefaults.windowInsets,
                    title = {},
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backdrop.getMaterial3AppBarColor(),
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                    )
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .windowInsetsPadding(
                            (windowInsetsSides?.let { TopAppBarDefaults.windowInsets.only(it) }
                                ?: TopAppBarDefaults.windowInsets)
                                .only(WindowInsetsSides.Top)
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    FloatingTabRow(
                        tabs = listOf("前景", "背景", "边框"),
                        selectedIndex = selectedTab,
                        onSelected = { viewModel.updateConfig { c -> c.copy(selectedTab = it) } },
                        indicatorPadding = 4.dp,
                        containerColor = if (themeState.useTabRowTransparentBackground) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHighest,
                        alignment = if (themeState.useTabRowCenterAlignment) FloatingTabRowAlignment.CENTER else FloatingTabRowAlignment.START,
                        widthMode = if (themeState.useTabRowFillWidth) FloatingTabRowWidthMode.FILL else FloatingTabRowWidthMode.WRAP_CONTENT,
                    )
                }
            }
        }
    )
    { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                .padding(
                    start = paddingValues.calculateStartPadding(androidx.compose.ui.platform.LocalLayoutDirection.current) + outerPadding.calculateStartPadding(
                        androidx.compose.ui.platform.LocalLayoutDirection.current
                    ),
                    top = paddingValues.calculateTopPadding(),
                    end = paddingValues.calculateEndPadding(androidx.compose.ui.platform.LocalLayoutDirection.current) + outerPadding.calculateEndPadding(
                        androidx.compose.ui.platform.LocalLayoutDirection.current
                    ),
                    bottom = 0.dp
                )
        ) {
            // 固定预览区（包含预览图与下方按钮组：壁纸/刷新/全屏 + 构建/保存预设）
            PreviewSection(
                bitmap = storePreviewBitmap,
                isLoading = isPreviewLoading,
                onPickWallpaper = {
                    try {
                        wallpaperLauncher.launch("image/*")
                    } catch (_: Exception) {
                    }
                },
                onRefresh = { viewModel.refreshPreview() },
                onExpand = {
                    viewModel.generatePreview(isLive = false)
                    // 打开 sheet 时快照一次当前 config，sheet 期间不刷新
                    previewConfigText = viewModel.getCurrentBuildConfig().toPrettyString()
                    showFullScreenPreview = true
                },
                onBuild = { showBuildSheet = true },
                onSavePreset = { /* TODO 预设功能 */ },
                savePresetEnabled = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 可滚动内容：左右内边距提到外层使 clip 圆角与卡片对齐，
            // 顶部使用与卡片一致的圆角，避免滚动时卡片顶部被平直截断
            val smootherRoundedCornersEnabled = isSmootherRoundedCornersEnabled()
            val scrollAreaTopShape = remember(smootherRoundedCornersEnabled) {
                kyantUnevenRoundedShape(
                    topStart = CornerRadius,
                    topEnd = CornerRadius,
                    bottomEnd = 0.dp,
                    bottomStart = 0.dp,
                    enabled = smootherRoundedCornersEnabled
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .clip(scrollAreaTopShape)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = outerPadding.calculateBottomPadding())
            ) {
                // Tab 内容
                AnimatedContent(
                    targetState = selectedTab,
                    label = "tab_content",
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    }
                ) { targetTab ->
                    when (targetTab) {
                        0 -> ForegroundTab(
                            viewModel,
                            backdrop,
                            useLiquidGlass = themeState.useLiquidGlassBottomSheet,
                            liquidGlassBlurRadius = themeState.liquidGlassBlurRadius.dp
                        )

                        1 -> BackgroundTab(
                            viewModel,
                            backdrop,
                            useLiquidGlass = themeState.useLiquidGlassBottomSheet,
                            liquidGlassBlurRadius = themeState.liquidGlassBlurRadius.dp
                        )

                        2 -> BorderTab(
                            viewModel = viewModel,
                            onGoToBackgroundTab = {
                                viewModel.updateConfig { c -> c.copy(selectedTab = 1) }
                            }
                        )
                    }
                }
            }
        }
    }

    FullScreenPreviewSheet(
        show = showFullScreenPreview,
        bitmap = mainPreviewBitmap,
        configText = previewConfigText,
        onDismiss = { showFullScreenPreview = false },
        backdrop = backdrop,
        useLiquidGlass = themeState.useLiquidGlassBottomSheet,
        liquidGlassBlurRadius = themeState.liquidGlassBlurRadius.dp
    )

    // 构建选项 Sheet
    if (showBuildSheet) {
        BuildOptionSheet(
            onDismiss = { showBuildSheet = false },
            iconSets = availableIconSets,
            apkEnabled = apkEnabled,
            onConfirm = { productType, iconSet ->
                showBuildSheet = false
                val missing = viewModel.buildPermissionsMissing()
                if (missing.isEmpty()) {
                    val submitted =
                        viewModel.submitBuildTask(productType, iconSet.id, iconSet.label)
                    if (submitted != null) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "已提交构建任务，可在任务 tab 中查看构建进度",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                } else {
                    pendingBuildArgs = productType to iconSet
                    pendingPermissionQueue = missing
                    permissionLauncher.launch(missing.first())
                }
            },
            backdrop = backdrop,
            useLiquidGlass = themeState.useLiquidGlassBottomSheet,
            liquidGlassBlurRadius = themeState.liquidGlassBlurRadius.dp
        )
    }
}
