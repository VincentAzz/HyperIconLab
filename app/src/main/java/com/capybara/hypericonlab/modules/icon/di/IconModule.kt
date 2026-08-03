package com.capybara.hypericonlab.modules.icon.di

import com.capybara.hypericonlab.modules.build.data.BuildArtifactWriter
import com.capybara.hypericonlab.modules.build.data.local.BuildTaskStore
import com.capybara.hypericonlab.modules.build.domain.packaging.ApkInstallFacade
import com.capybara.hypericonlab.modules.build.domain.packaging.ApkInstaller
import com.capybara.hypericonlab.modules.build.domain.packaging.IconPackApkAssembler
import com.capybara.hypericonlab.modules.build.domain.packaging.IconPackApkBuildService
import com.capybara.hypericonlab.modules.build.domain.packaging.IconPackApkSigner
import com.capybara.hypericonlab.modules.build.domain.packaging.IconPackSigningKeyManager
import com.capybara.hypericonlab.modules.build.domain.usecase.BuildTaskExecutor
import com.capybara.hypericonlab.modules.build.domain.usecase.BuildTaskManager
import com.capybara.hypericonlab.modules.build.notification.BuildNotificationManager
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.DefaultLawniconsAssetFacade
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.IconPackTemplateArchive
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.IconPackTemplateManager
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsApiService
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsAssetFacade
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsDownloadService
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsResourceManager
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsUpdateManager
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsUpdateNotifier
import com.capybara.hypericonlab.modules.icon.domain.usecase.GeneratePreviewUseCase
import com.capybara.hypericonlab.modules.icon.domain.usecase.IconPipelineUseCase
import com.capybara.hypericonlab.modules.icon.domain.usecase.ManageResourcesUseCase
import com.capybara.hypericonlab.modules.icon.viewmodel.IconViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val iconModule = module {
    single { LawniconsResourceManager(get()) }
    single { LawniconsApiService() }
    single { LawniconsDownloadService(get()) }
    single { IconPackTemplateArchive() }
    single { IconPackTemplateManager(get(), get(), get(), get(), get(), get()) }
    single<LawniconsAssetFacade> { DefaultLawniconsAssetFacade(get(), get(), get()) }
    factory { IconPackApkAssembler() }
    single { IconPackSigningKeyManager() }
    factory { IconPackApkSigner() }
    factory { IconPackApkBuildService(get(), get(), get(), get()) }
    single<ApkInstallFacade> { ApkInstaller(get()) }
    // Notifier 用 single：内部缓存 channelCreated 状态，避免重复创建 NotificationChannel
    single { LawniconsUpdateNotifier(get()) }
    single { LawniconsUpdateManager(get(), get(), get(), get(), get(), get(), get()) }
    factory { ManageResourcesUseCase(get()) }
    factory { GeneratePreviewUseCase(get(), get()) }
    factory { IconPipelineUseCase(get()) }
    factory { BuildArtifactWriter(get()) }
    factory { BuildTaskStore(get()) }
    factory { BuildTaskExecutor(get(), get(), get(), get(), get(), get()) }
    single { BuildTaskManager(get(), get(), get()) }
    factory { BuildNotificationManager(get()) }
    viewModelOf(::IconViewModel)
}
