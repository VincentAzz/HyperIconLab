package com.capybara.hypericonlab.modules.icon.ui.page.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.liquidglass.appBarBlurEffect
import com.capybara.hypericonlab.core.designsystem.liquidglass.getMaterial3AppBarColor
import com.capybara.hypericonlab.core.designsystem.liquidglass.rememberMaterial3BlurBackdrop
import com.capybara.hypericonlab.modules.settings.ui.page.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 主页：已清空为空白页，保留 Scaffold + TopAppBar 框架，后续作为预设页面容器。
 *
 * 已移除内容：
 * - 状态文本、生成预览图按钮、生成测试包按钮、生成常用包按钮
 * - 日志 IconButton 与 LogSheet 调用（LogSheet 组件文件保留备用）
 *
 * @param outerPadding 外层 Scaffold 提供的 padding
 * @param windowInsetsSides 可选窗口Insets侧边过滤
 */
@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    themeViewModel: SettingsViewModel = koinViewModel(),
    outerPadding: PaddingValues = PaddingValues(0.dp),
    windowInsetsSides: WindowInsetsSides? = null
) {
    val themeState by themeViewModel.state.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val backdrop = rememberMaterial3BlurBackdrop(themeState.useBlur)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = windowInsetsSides?.let { ScaffoldDefaults.contentWindowInsets.only(it) }
            ?: ScaffoldDefaults.contentWindowInsets,
        topBar = {
            TopAppBar(
                modifier = Modifier.appBarBlurEffect(
                    backdrop = backdrop,
                    useProgressiveBlur = themeState.useProgressiveBlurTopAppBar
                ),
                title = { Text("主页") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backdrop.getMaterial3AppBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                )
            )
        }
    ) { paddingValues ->
        // 空白内容区，后续接入预设卡片
        Box(
            modifier = modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                .padding(
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current) + outerPadding.calculateStartPadding(
                        LocalLayoutDirection.current
                    ),
                    top = paddingValues.calculateTopPadding(),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current) + outerPadding.calculateEndPadding(
                        LocalLayoutDirection.current
                    ),
                    bottom = outerPadding.calculateBottomPadding()
                )
        )
    }
}
