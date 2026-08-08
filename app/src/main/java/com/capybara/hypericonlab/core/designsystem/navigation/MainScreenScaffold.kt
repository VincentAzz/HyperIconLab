package com.capybara.hypericonlab.core.designsystem.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capybara.hypericonlab.core.designsystem.component.FloatingBarCompactItem
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomBar
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomBarCompact
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomBarDefaults
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomBarItem
import com.capybara.hypericonlab.core.designsystem.component.NotifyBadge
import com.capybara.hypericonlab.core.designsystem.liquidglass.LiquidGlassEngine
import com.capybara.hypericonlab.core.designsystem.liquidglass.kyant.backdrops.LocalKyantBackdrop
import com.capybara.hypericonlab.core.designsystem.liquidglass.material3BlurEffect
import com.capybara.hypericonlab.core.designsystem.theme.isSmootherRoundedCornersEnabled
import com.capybara.hypericonlab.modules.icon.ui.page.custom.CustomPage
import com.capybara.hypericonlab.modules.icon.ui.page.home.HomePage
import com.capybara.hypericonlab.modules.icon.ui.page.task.TaskPage
import com.capybara.hypericonlab.modules.settings.ui.page.settings.SettingsPage
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.capybara.hypericonlab.core.designsystem.liquidglass.kyant.backdrops.LayerBackdrop as KyantLayerBackdrop
import com.capybara.hypericonlab.core.designsystem.liquidglass.kyant.backdrops.layerBackdrop as kyantLayerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreenScaffold(
    configCount: Int,
    hasAssetUpdate: Boolean,
    mainPagerState: MainPagerState,
    tabs: List<NavigationTab>,
    useBlur: Boolean,
    useFloatingBottomBar: Boolean,
    useFloatingBottomBarBlur: Boolean,
    m3Backdrop: LayerBackdrop?,
    floatingBackdrop: LayerBackdrop,
    kyantBackdrop: KyantLayerBackdrop,
    isMedium: Boolean
) {
    val navigationWindowInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(),
        bottomBar = {
            if (useFloatingBottomBar) {
                FloatingBottomBarSlot(
                    mainPagerState = mainPagerState,
                    tabs = tabs,
                    configCount = configCount,
                    hasAssetUpdate = hasAssetUpdate,
                    backdrop = floatingBackdrop,
                    m3Backdrop = m3Backdrop,
                    useBlur = useBlur,
                    useFloatingBottomBarBlur = useFloatingBottomBarBlur,
                    kyantBackdrop = kyantBackdrop
                )
            } else {
                StandardRowNavigation(
                    modifier = Modifier.material3BlurEffect(m3Backdrop),
                    windowInsets = navigationWindowInsets,
                    tabs = tabs,
                    currentPage = mainPagerState.pagerState.targetPage,
                    onPageChanged = { mainPagerState.animateToPage(it) },
                    configCount = configCount,
                    hasAssetUpdate = hasAssetUpdate,
                    containerColor = if (useBlur) Color.Transparent else BottomAppBarDefaults.containerColor,
                    isMedium = isMedium
                )
            }
        }
    ) { paddingValues ->
        CompositionLocalProvider(
            LocalKyantBackdrop provides if (
                LocalThemeState.current.liquidGlassEngine == LiquidGlassEngine.KYANT
            ) {
                kyantBackdrop
            } else {
                null
            }
        ) {
            MainPagerContent(
                modifier = Modifier.fillMaxSize(),
                mainPagerState = mainPagerState,
                tabs = tabs,
                useBlur = useBlur,
                useFloatingBottomBar = useFloatingBottomBar,
                m3Backdrop = m3Backdrop,
                floatingBackdrop = floatingBackdrop,
                kyantBackdrop = kyantBackdrop,
                outerPadding = paddingValues
            )
        }
    }
}

@Composable
private fun FloatingBottomBarSlot(
    mainPagerState: MainPagerState,
    tabs: List<NavigationTab>,
    configCount: Int,
    hasAssetUpdate: Boolean,
    backdrop: LayerBackdrop,
    m3Backdrop: LayerBackdrop?,
    useBlur: Boolean,
    useFloatingBottomBarBlur: Boolean,
    kyantBackdrop: KyantLayerBackdrop
) {
    val themeState = LocalThemeState.current
    val smootherRoundedCornersEnabled = isSmootherRoundedCornersEnabled()

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        key(smootherRoundedCornersEnabled, themeState.liquidGlassEngine) {
            if (themeState.useFloatingBottomBarCompact) {
                FloatingBottomBarCompact(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(
                            bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues()
                                .calculateBottomPadding()
                        ),
                    selectedIndex = { mainPagerState.pagerState.targetPage },
                    onSelected = { index: Int ->
                        mainPagerState.animateToPage(index)
                    },
                    backdrop = backdrop,
                    m3Backdrop = m3Backdrop,
                    tabsCount = tabs.size,
                    isBlurEnabled = useFloatingBottomBarBlur,
                    isStandardBlurEnabled = useBlur,
                    engine = themeState.liquidGlassEngine,
                    kyantBackdrop = kyantBackdrop,
                    colors = FloatingBottomBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                ) {
                    tabs.forEachIndexed { index, tab ->
                        FloatingBarCompactItem(
                        selectedIndex = { mainPagerState.pagerState.targetPage },
                        onClick = {
                            mainPagerState.animateToPage(index)
                        },
                        icon = tab.icon,
                        label = tab.label,
                        type = themeState.floatingBottomBarCompactType,
                        index = index
                        )
                    }
                }
            } else {
                FloatingBottomBar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(
                            bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues()
                                .calculateBottomPadding()
                        ),
                    selectedIndex = { mainPagerState.pagerState.targetPage },
                    onSelected = { index: Int ->
                        mainPagerState.animateToPage(index)
                    },
                    backdrop = backdrop,
                    m3Backdrop = m3Backdrop,
                    tabsCount = tabs.size,
                    isBlurEnabled = useFloatingBottomBarBlur,
                    isStandardBlurEnabled = useBlur,
                    engine = themeState.liquidGlassEngine,
                    kyantBackdrop = kyantBackdrop,
                    colors = FloatingBottomBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    tabs.forEachIndexed { index, tab ->
                        FloatingBottomBarItem(
                            onClick = {
                                mainPagerState.animateToPage(index)
                            },
                            modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                        ) {
                            val showBadge = index == 1 && configCount > 1

                            BadgedBox(
                                badge = {
                                    ConfigBadge(showBadge = showBadge, configCount = configCount)
                                }
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                )
                            }
                            NotifyBadge(
                                showBadge = index == TAB_INDEX_SETTINGS && hasAssetUpdate
                            ) {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainPagerContent(
    modifier: Modifier = Modifier,
    mainPagerState: MainPagerState,
    tabs: List<NavigationTab>,
    useBlur: Boolean,
    useFloatingBottomBar: Boolean,
    m3Backdrop: LayerBackdrop?,
    floatingBackdrop: LayerBackdrop,
    kyantBackdrop: KyantLayerBackdrop,
    outerPadding: PaddingValues
) {
    val themeState = LocalThemeState.current
    HorizontalPager(
        state = mainPagerState.pagerState,
        userScrollEnabled = true,
        modifier = modifier
            .then(if (m3Backdrop != null) Modifier.layerBackdrop(m3Backdrop) else Modifier)
            .then(
                if (
                    themeState.liquidGlassEngine == LiquidGlassEngine.KYANT &&
                    (useFloatingBottomBar || themeState.useLiquidGlassBottomSheet)
                ) {
                    Modifier.kyantLayerBackdrop(kyantBackdrop)
                } else if (useFloatingBottomBar) {
                    Modifier.layerBackdrop(floatingBackdrop)
                } else {
                    Modifier
                }
            )
    ) { page ->
        when (page) {
            0 -> HomePage(
                outerPadding = outerPadding
            )

            1 -> CustomPage(
                outerPadding = outerPadding
            )

            2 -> {
                TaskPage(
                    outerPadding = outerPadding,
                    onTaskClick = { taskId ->
                        // 任务卡片点击回调，详情 sheet 将在步骤 8 实现
                        // 此处暂不处理，留空实现
                    }
                )
            }

            3 -> SettingsPage(
                outerPadding = outerPadding
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StandardRowNavigation(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets,
    tabs: List<NavigationTab>,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    configCount: Int,
    hasAssetUpdate: Boolean,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    isMedium: Boolean = false
) {
    ShortNavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(),
        windowInsets = windowInsets,
        containerColor = containerColor,
        arrangement = if (isMedium) ShortNavigationBarArrangement.Centered else ShortNavigationBarArrangement.EqualWeight
    ) {
        tabs.forEachIndexed { index, navigationData ->
            ShortNavigationBarItem(
                selected = currentPage == index,
                onClick = { onPageChanged(index) },
                iconPosition = if (isMedium) NavigationItemIconPosition.Start else NavigationItemIconPosition.Top,
                icon = {
                    val showBadge = index == 1 && configCount > 1
                    BadgedBox(
                        badge = {
                            ConfigBadge(showBadge = showBadge, configCount = configCount)
                        }
                    ) {
                        Icon(
                            imageVector = navigationData.icon,
                            contentDescription = navigationData.label
                        )
                    }
                },
                label = {
                    NotifyBadge(showBadge = index == TAB_INDEX_SETTINGS && hasAssetUpdate) {
                        Text(text = navigationData.label)
                    }
                }
            )
        }
    }
}

@Composable
private fun ConfigBadge(showBadge: Boolean, configCount: Int) {
    AnimatedVisibility(
        visible = showBadge,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        label = "badge"
    ) {
        Badge(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Text(configCount.toString())
        }
    }
}
