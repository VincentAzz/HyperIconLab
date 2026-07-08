package com.capybara.hypericonlab.modules.icon.ui.page.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.liquidglass.getMaterial3AppBarColor
import com.capybara.hypericonlab.core.designsystem.liquidglass.material3BlurEffect
import com.capybara.hypericonlab.core.designsystem.liquidglass.rememberMaterial3BlurBackdrop
import com.capybara.hypericonlab.core.designsystem.symbol.history
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.modules.icon.ui.page.custom.IconViewModel
import com.capybara.hypericonlab.modules.icon.ui.page.home.component.LogSheet
import com.capybara.hypericonlab.modules.settings.ui.page.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    viewModel: IconViewModel = koinViewModel(),
    themeViewModel: SettingsViewModel = koinViewModel(),
    outerPadding: PaddingValues = PaddingValues(0.dp),
    windowInsetsSides: WindowInsetsSides? = null
) {
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val mapperExists by viewModel.mapperExists.collectAsStateWithLifecycle()
    val statusText by viewModel.statusText.collectAsStateWithLifecycle()
    val themeState by themeViewModel.state.collectAsStateWithLifecycle()

    var showLogs by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val backdrop = rememberMaterial3BlurBackdrop(themeState.useBlur)

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = windowInsetsSides?.let { ScaffoldDefaults.contentWindowInsets.only(it) }
            ?: ScaffoldDefaults.contentWindowInsets,
        topBar = {
            TopAppBar(
                modifier = Modifier.material3BlurEffect(backdrop),
                title = { Text("主页") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backdrop.getMaterial3AppBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                ),
                actions = {
                    IconButton(onClick = { showLogs = true }) {
                        Icon(AppMaterialSymbols.history, contentDescription = "日志")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 状态文本
            Text(
                text = "系统状态: $statusText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 生成预览图按钮
            Button(
                onClick = { viewModel.generatePreview() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRunning && mapperExists,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) { Text("生成预览图") }

            Spacer(modifier = Modifier.height(16.dp))

            // 生成测试包
            Button(
                onClick = { viewModel.runPipeline("icon_mapper_test.xml") },
                enabled = !isRunning && mapperExists,
                modifier = Modifier.fillMaxWidth()
            ) { Text("生成测试包 (icon_mapper_test)") }

            Spacer(modifier = Modifier.height(16.dp))

            // 生成常用包
            Button(
                onClick = { viewModel.runPipeline("icon_mapper_filtered.xml") },
                enabled = !isRunning && mapperExists,
                modifier = Modifier.fillMaxWidth()
            ) { Text("生成常用包 (icon_mapper_filtered)") }

            Spacer(modifier = Modifier.height(200.dp))
        }
    }

    if (showLogs) {
        LogSheet(
            viewModel = viewModel,
            onDismiss = { showLogs = false },
            backdrop = backdrop,
            useLiquidGlass = themeState.useLiquidGlassBottomSheet,
            liquidGlassBlurRadius = themeState.liquidGlassBlurRadius.dp
        )
    }
}
