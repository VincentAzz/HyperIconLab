package com.capybara.hypericonlab.core.designsystem.navigation

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.R
import com.capybara.hypericonlab.core.designsystem.liquidglass.rememberMaterial3BlurBackdrop
import com.capybara.hypericonlab.core.designsystem.symbol.color_lens
import com.capybara.hypericonlab.core.designsystem.symbol.home
import com.capybara.hypericonlab.core.designsystem.symbol.settings
import com.capybara.hypericonlab.core.designsystem.symbol.task_alt
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.util.LocalWindowLayoutInfo
import com.capybara.hypericonlab.modules.settings.domain.model.ThemeState
import com.capybara.hypericonlab.modules.settings.ui.page.settings.SettingsSharedViewModel
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

val LocalThemeState = staticCompositionLocalOf { ThemeState() }

// 通知 Intent extras key：用于从通知 PendingIntent 传递目标 tab 索引到 Compose 层
const val EXTRA_TAB_INDEX = "extra_tab_index"

// 任务 tab 索引（与下方 tabs 列表顺序对应：home=0, custom=1, task=2, settings=3）
const val TAB_INDEX_TASK = 2

// 由 MainActivity 在 onCreate / onNewIntent 中更新，MainScreen 收集后调用 animateToPage 切换 tab
// 使用 compositionLocalOf（非 static）以保证值变化能触发下游重组
val LocalPendingTab = compositionLocalOf<Int?> { null }

@Immutable
data class NavigationTab(
    val icon: ImageVector,
    val label: String
)

@Composable
fun MainScreen(
    uiState: ThemeState,
    sharedViewModel: SettingsSharedViewModel
) {
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
    val useBlur = uiState.useBlur
    val useFloatingBottomBar = uiState.useFloatingBottomBar
    val useFloatingBottomBarBlur = uiState.useFloatingBottomBarBlur
    val m3Backdrop = rememberMaterial3BlurBackdrop(useBlur)
    val floatingBackdrop = rememberLayerBackdrop()

    val configCount = 0
    val homeLabel = stringResource(id = R.string.home)
    val configLabel = stringResource(R.string.custom)
    val taskLabel = stringResource(R.string.task)
    val preferredLabel = stringResource(R.string.preferred)

    val tabs = remember(homeLabel, configLabel, taskLabel, preferredLabel) {
        listOf(
            NavigationTab(
                icon = AppMaterialSymbols.home,
                label = homeLabel
            ),
            NavigationTab(
                icon = AppMaterialSymbols.color_lens,
                label = configLabel
            ),
            NavigationTab(
                icon = AppMaterialSymbols.task_alt,
                label = taskLabel
            ),
            NavigationTab(
                icon = AppMaterialSymbols.settings,
                label = preferredLabel
            )
        )
    }

    val pagerState = rememberPagerState(
        initialPage = sharedState.lastMainPageIndex,
        pageCount = { tabs.size }
    )
    val mainPagerState = rememberMainPagerState(pagerState)
    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(settledPage) {
        mainPagerState.syncPage()
        if (sharedState.lastMainPageIndex != settledPage) {
            sharedViewModel.updateLastMainPageIndex(settledPage)
        }
    }

    // 监听通知 PendingIntent 传入的 tab 切换请求（点击构建通知 → 跳转到任务 tab）
    val pendingTab = LocalPendingTab.current
    LaunchedEffect(pendingTab) {
        // pendingTab 非 null 且在有效范围内时切换；null 表示无请求
        pendingTab?.let { target ->
            if (target in tabs.indices && target != settledPage) {
                mainPagerState.animateToPage(target)
            }
        }
    }

    MainScreenBackHandler(
        mainPagerState = mainPagerState,
    )

    val layoutInfo = LocalWindowLayoutInfo.current
    val isMedium = layoutInfo.isMediumPortrait

    CompositionLocalProvider(
        LocalThemeState provides uiState
    ) {
        MainScreenScaffold(
            configCount = configCount,
            mainPagerState = mainPagerState,
            tabs = tabs,
            useBlur = useBlur,
            useFloatingBottomBar = useFloatingBottomBar,
            useFloatingBottomBarBlur = useFloatingBottomBarBlur,
            m3Backdrop = m3Backdrop,
            floatingBackdrop = floatingBackdrop,
            isMedium = isMedium
        )
    }
}
