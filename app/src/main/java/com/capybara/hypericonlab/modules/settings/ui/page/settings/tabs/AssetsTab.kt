package com.capybara.hypericonlab.modules.settings.ui.page.settings.tabs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.AssetUpdateCheckState
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.IconPackTemplateState
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsAssetFacade
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.ResourceSource
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTask
import com.capybara.hypericonlab.modules.icon.domain.repository.AssetUpdateCheckTrigger
import com.capybara.hypericonlab.modules.icon.domain.usecase.AppM3PreprocessManager
import com.capybara.hypericonlab.modules.icon.domain.usecase.InitializationCoordinator
import com.capybara.hypericonlab.modules.settings.domain.repository.AppSettingsRepository
import com.capybara.hypericonlab.modules.settings.ui.page.settings.component.DownloadMode
import com.capybara.hypericonlab.modules.settings.ui.page.settings.component.InitializationCard
import com.capybara.hypericonlab.modules.settings.ui.page.settings.sections.AssetCleanupSection
import com.capybara.hypericonlab.modules.settings.ui.page.settings.sections.LawniconsOverviewSection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop

private object AssetsTabDefaults {
    val InitializationHorizontalPadding = 16.dp
    const val CooldownTickerIntervalMs = 60_000L
}

@Composable
fun AssetsTab(
    paddingValues: PaddingValues,
    outerPadding: PaddingValues,
    backdrop: LayerBackdrop?,
    onBrowseLawnicons: () -> Unit,
    onSwitchSource: () -> Unit,
    onChooseDownloadMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val assetsFacade = koinInject<LawniconsAssetFacade>()
    val initializationCoordinator = koinInject<InitializationCoordinator>()
    val appM3PreprocessManager = koinInject<AppM3PreprocessManager>()
    val appSettingsRepository = koinInject<AppSettingsRepository>()
    val scope = rememberCoroutineScope()

    // 当前版本信息（从 Facade 观察，来源切换后自动更新）
    val version by assetsFacade.currentVersion.collectAsStateWithLifecycle()
    val initializationState by initializationCoordinator.state.collectAsStateWithLifecycle()
    val assetCheckState by assetsFacade.assetCheckState.collectAsStateWithLifecycle()
    val assetUpdateRunning by initializationCoordinator.assetUpdateRunning.collectAsStateWithLifecycle()
    val assetUpdateState by initializationCoordinator.assetUpdateState.collectAsStateWithLifecycle()
    val lastManualAssetCheckAt by assetsFacade.lastManualAssetCheckAt.collectAsStateWithLifecycle()
    val templateState by assetsFacade.templateState.collectAsStateWithLifecycle()
    val cacheAvailable by appM3PreprocessManager.cacheAvailable.collectAsStateWithLifecycle()
    val hasDownloadedAssets = assetsFacade.getDownloadedVersions().isNotEmpty()
    // 下载代理开关
    val useDownloadProxy by appSettingsRepository.useDownloadProxy.collectAsStateWithLifecycle()
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(lastManualAssetCheckAt, assetCheckState, assetUpdateRunning) {
        while (true) {
            currentTime = System.currentTimeMillis()
            if (assetsFacade.assetCheckCooldownRemainingMs(
                    AssetUpdateCheckTrigger.MANUAL,
                    currentTime
                ) == 0L
            ) {
                break
            }
            delay(AssetsTabDefaults.CooldownTickerIntervalMs)
        }
    }
    val canCheckAssets = assetsFacade.canCheckForAssetUpdates(
        trigger = AssetUpdateCheckTrigger.MANUAL,
        now = currentTime
    )
    val isSimulatedAssetUpdateTriggered =
        (assetCheckState as? AssetUpdateCheckState.Available)?.isSimulated == true

    // 版本号 + 来源统一显示：日期 (commit) - 内置/云端
    val versionDate = version.version.substringBefore("-")
    val commitShort = version.lawniconsCommit.take(7)
    val sourceLabel = when (version.source) {
        ResourceSource.REMOTE -> "云端"
        ResourceSource.ASSETS -> "内置"
    }
    val versionText = "$versionDate ($commitShort) - $sourceLabel"

    // 下载方式显示文本
    val downloadModeText =
        if (useDownloadProxy) DownloadMode.PROXY.label else DownloadMode.DIRECT.label

    val templateVersionText = formatTemplateVersion(version.source, templateState)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        contentPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(layoutDirection) + outerPadding.calculateStartPadding(
                layoutDirection
            ),
            top = paddingValues.calculateTopPadding(),
            end = paddingValues.calculateEndPadding(layoutDirection) + outerPadding.calculateEndPadding(
                layoutDirection
            ),
            bottom = outerPadding.calculateBottomPadding(),
        ),
    ) {
        item(key = "initialization") {
            InitializationCard(
                modifier = Modifier.padding(horizontal = AssetsTabDefaults.InitializationHorizontalPadding),
                state = initializationState,
                onStart = { initializationCoordinator.startInitialization(manualStart = true) },
                onRetry = { initializationCoordinator.startInitialization(manualStart = true) },
                assetCheckState = assetCheckState,
                assetUpdateState = assetUpdateState,
                onAssetUpdate = { initializationCoordinator.startManualAssetUpdate() }
            )
        }

        item(key = "lawnicons") {
            LawniconsOverviewSection(
                versionText = versionText,
                iconCountText = "${version.svgCount} 图标, ${version.mapperCount} 映射",
                templateVersionText = templateVersionText,
                assetUpdateState = assetCheckState,
                assetUpdateRunning = assetUpdateRunning,
                canCheckAssetUpdates = canCheckAssets,
                downloadModeText = downloadModeText,
                onChooseDownloadMode = onChooseDownloadMode,
                onSwitchSource = onSwitchSource,
                onBrowseLawnicons = onBrowseLawnicons,
                onCheckAssetUpdates = {
                    scope.launch {
                        assetsFacade.checkForAssetUpdates(AssetUpdateCheckTrigger.MANUAL)
                    }
                },
                onUpdateAssets = { initializationCoordinator.startManualAssetUpdate() }
            )
        }

        // 清除资产缓存：删除所有云端下载资源并切回本地（调试用，上线前移除）
        item(key = "clearCache") {
            AssetCleanupSection(
                hasDownloadedAssets = hasDownloadedAssets,
                cacheAvailable = cacheAvailable,
                isAssetUpdateRunning = assetUpdateRunning,
                isSimulatedAssetUpdateTriggered = isSimulatedAssetUpdateTriggered,
                onClearAssets = { showResetDialog = true },
                onClearColorCache = { showClearCacheDialog = true },
                onSimulateAssetUpdate = assetsFacade::simulateAssetUpdate
            )
        }

        item(key = "navPadding") {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("清除颜色映射缓存？") },
            text = { Text("清除后不会自动开始生成，需要在资产页手动点击开始。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        appM3PreprocessManager.clearCache()
                        initializationCoordinator.resetForManualInitialization(
                            invalidatedTasks = setOf(InitializationTask.APP_M3_CACHE)
                        )
                    }
                ) { Text("清除") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("取消") }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("清除已下载资产？") },
            text = { Text("将删除云端 Lawnicons 与 APK 模板，颜色映射缓存会保留。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        scope.launch {
                            assetsFacade.clearCloudAssets()
                            assetsFacade.resetUpdateState()
                            initializationCoordinator.resetForManualInitialization(
                                invalidatedTasks = setOf(
                                    InitializationTask.LAWNICONS,
                                    InitializationTask.APK_TEMPLATE
                                )
                            )
                        }
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("取消") }
            }
        )
    }
}

private fun formatTemplateVersion(
    source: ResourceSource,
    state: IconPackTemplateState
): String = if (source == ResourceSource.ASSETS) {
    "内置版本不支持"
} else {
    when (state) {
        is IconPackTemplateState.Available -> state.version
        IconPackTemplateState.Idle -> "未准备"
        IconPackTemplateState.Checking,
        is IconPackTemplateState.Downloading -> "准备中"

        IconPackTemplateState.Unavailable,
        IconPackTemplateState.Failed -> "不可用"
    }
}
