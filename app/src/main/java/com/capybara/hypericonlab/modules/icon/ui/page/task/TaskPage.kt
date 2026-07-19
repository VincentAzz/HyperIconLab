package com.capybara.hypericonlab.modules.icon.ui.page.task

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.liquidglass.appBarBlurEffect
import com.capybara.hypericonlab.core.designsystem.liquidglass.getMaterial3AppBarColor
import com.capybara.hypericonlab.core.designsystem.liquidglass.rememberMaterial3BlurBackdrop
import com.capybara.hypericonlab.modules.icon.ui.page.custom.IconViewModel
import com.capybara.hypericonlab.modules.settings.ui.page.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

/**
 * 任务页面：展示活动任务与已完成任务列表。
 *
 * 布局：
 * - 顶部 TopAppBar（标题"任务"）
 * - LazyColumn：
 *   - "活动任务" section（PENDING + RUNNING），无活动任务时不显示
 *   - "已完成任务" section（SUCCESS + FAILED），无已完成任务时显示空态文案
 *
 * 卡片点击：触发 [onTaskClick] 回调，由上层打开 TaskDetailSheet。
 *
 * @param onTaskClick 任务卡片点击回调，参数为任务 id
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TaskPage(
    modifier: Modifier = Modifier,
    viewModel: IconViewModel = koinViewModel(),
    themeViewModel: SettingsViewModel = koinViewModel(),
    outerPadding: PaddingValues = PaddingValues(0.dp),
    windowInsetsSides: WindowInsetsSides? = null,
    onTaskClick: (String) -> Unit = {}
) {
    val activeTasks by viewModel.activeBuildTasks.collectAsStateWithLifecycle()
    val finishedTasks by viewModel.finishedBuildTasks.collectAsStateWithLifecycle()
    val themeState by themeViewModel.state.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
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
                windowInsets = windowInsetsSides?.let { TopAppBarDefaults.windowInsets.only(it) }
                    ?: TopAppBarDefaults.windowInsets,
                title = { Text("任务") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backdrop.getMaterial3AppBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                )
            )
        }
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                .padding(
                    start = paddingValues.calculateStartPadding(layoutDirection) + outerPadding.calculateStartPadding(
                        layoutDirection
                    ),
                    top = paddingValues.calculateTopPadding(),
                    end = paddingValues.calculateEndPadding(layoutDirection) + outerPadding.calculateEndPadding(
                        layoutDirection
                    ),
                    bottom = outerPadding.calculateBottomPadding()
                ),
            contentPadding = PaddingValues(
                horizontal = TaskPageConfig.HORIZONTAL_PADDING,
                vertical = TaskPageConfig.VERTICAL_PADDING
            ),
            verticalArrangement = Arrangement.spacedBy(TaskPageConfig.CARD_SPACING)
        ) {
            // 活动任务 section
            if (activeTasks.isNotEmpty()) {
                item {
                    SectionTitle("活动任务 (${activeTasks.size})")
                }
                items(activeTasks, key = { it.taskId }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onTaskClick(task.taskId) }
                    )
                }
            }

            // 已完成任务 section
            if (finishedTasks.isNotEmpty()) {
                item {
                    if (activeTasks.isNotEmpty()) {
                        Spacer(Modifier.height(TaskPageConfig.SECTION_GAP))
                    }
                    SectionTitle("已完成任务 (${finishedTasks.size})")
                }
                items(finishedTasks, key = { it.taskId }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onTaskClick(task.taskId) }
                    )
                }
            }

            // 空态：无任何任务
            if (activeTasks.isEmpty() && finishedTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "暂无任务",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "前往「自定义」页面构建图标包",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// section 标题（与 SegmentedColumn 标题风格一致）
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 4.dp,
            top = 8.dp,
            bottom = 4.dp
        )
    )
}

// 任务页面关键参数集中声明，便于调参
private object TaskPageConfig {
    // 列表水平内边距
    val HORIZONTAL_PADDING = 16.dp

    // 列表垂直内边距
    val VERTICAL_PADDING = 8.dp

    // 卡片间距
    val CARD_SPACING = 12.dp

    // section 间距
    val SECTION_GAP = 16.dp
}
