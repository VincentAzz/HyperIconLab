package com.capybara.hypericonlab.modules.icon.ui.page.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.liquidglass.appBarBlurEffect
import com.capybara.hypericonlab.core.designsystem.liquidglass.getMaterial3AppBarColor
import com.capybara.hypericonlab.core.designsystem.liquidglass.rememberMaterial3BlurBackdrop
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.AssetUpdateCheckState
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsAssetFacade
import com.capybara.hypericonlab.modules.icon.domain.usecase.InitializationCoordinator
import com.capybara.hypericonlab.modules.icon.viewmodel.IconViewModel
import com.capybara.hypericonlab.modules.settings.ui.page.settings.SettingsViewModel
import com.capybara.hypericonlab.modules.settings.ui.page.settings.component.InitializationCard
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.blur.layerBackdrop

private object HomePageDefaults {
    val ContentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    val KeepInitializationCardVisibleForDebug = true
}

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    themeViewModel: SettingsViewModel = koinViewModel(),
    iconViewModel: IconViewModel = koinViewModel(),
    outerPadding: PaddingValues = PaddingValues(0.dp),
    windowInsetsSides: WindowInsetsSides? = null
) {
    val themeState by themeViewModel.state.collectAsStateWithLifecycle()
    val initializationState by iconViewModel.initializationState.collectAsStateWithLifecycle()
    val assetsFacade = koinInject<LawniconsAssetFacade>()
    val initializationCoordinator = koinInject<InitializationCoordinator>()
    val assetCheckState by assetsFacade.assetCheckState.collectAsStateWithLifecycle()
    val assetUpdateState by initializationCoordinator.assetUpdateState.collectAsStateWithLifecycle()
    val hasAssetUpdate = assetUpdateState != null ||
            assetCheckState is AssetUpdateCheckState.Available
    var completedCardVisible by rememberSaveable { mutableStateOf(true) }
    val latestInitializationState by rememberUpdatedState(initializationState)

    LaunchedEffect(initializationState.isCompleted) {
        if (!initializationState.isCompleted) {
            completedCardVisible = true
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (latestInitializationState.isCompleted) {
                completedCardVisible = false
            }
        }
    }

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
        Box(
            modifier = modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                if (HomePageDefaults.KeepInitializationCardVisibleForDebug ||
                    hasAssetUpdate || !initializationState.isCompleted || completedCardVisible
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(HomePageDefaults.ContentPadding)
                    ) {
                        InitializationCard(
                            state = initializationState,
                            onStart = iconViewModel::startInitialization,
                            onRetry = iconViewModel::startInitialization,
                            assetCheckState = assetCheckState,
                            assetUpdateState = assetUpdateState,
                            onAssetUpdate = { initializationCoordinator.startManualAssetUpdate() }
                        )
                    }
                }
            }
        }
    }
}
