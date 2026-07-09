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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRow
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRowAlignment
import com.capybara.hypericonlab.core.designsystem.component.FloatingTabRowWidthMode
import com.capybara.hypericonlab.core.designsystem.liquidglass.appBarBlurEffect
import com.capybara.hypericonlab.core.designsystem.liquidglass.getMaterial3AppBarColor
import com.capybara.hypericonlab.core.designsystem.liquidglass.rememberMaterial3BlurBackdrop
import com.capybara.hypericonlab.modules.icon.ui.page.custom.tabs.BackgroundTab
import com.capybara.hypericonlab.modules.icon.ui.page.custom.tabs.BorderTab
import com.capybara.hypericonlab.modules.icon.ui.page.custom.tabs.ForegroundTab
import com.capybara.hypericonlab.modules.icon.ui.page.custom.sections.FullScreenPreview
import com.capybara.hypericonlab.modules.icon.ui.page.custom.sections.PreviewSection
import com.capybara.hypericonlab.modules.settings.ui.page.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
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
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val themeState by themeViewModel.state.collectAsStateWithLifecycle()

    var showFullScreenPreview by remember { mutableStateOf(false) }

    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.updateWallpaperFromUri(uri)
            }
        }
    )

    val scrollBehavior =
        TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val backdrop = rememberMaterial3BlurBackdrop(themeState.useBlur)

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = windowInsetsSides?.let { ScaffoldDefaults.contentWindowInsets.only(it) }
            ?: ScaffoldDefaults.contentWindowInsets,
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
            // 固定预览区
            PreviewSection(
                bitmap = storePreviewBitmap,
                onPickWallpaper = {
                    try {
                        wallpaperLauncher.launch("image/*")
                    } catch (_: Exception) {
                    }
                },
                onRefresh = { viewModel.refreshPreview() },
                onExpand = {
                    viewModel.generatePreview(isLive = false)
                    showFullScreenPreview = true
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 可滚动内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
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

                        2 -> BorderTab()
                    }
                }
            }
        }
    }

    FullScreenPreview(
        show = showFullScreenPreview,
        bitmap = mainPreviewBitmap,
        onDismiss = { showFullScreenPreview = false }
    )
}
