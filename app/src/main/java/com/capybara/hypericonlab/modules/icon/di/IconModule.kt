package com.capybara.hypericonlab.modules.icon.di

import com.capybara.hypericonlab.core.notification.BuildNotificationManager
import com.capybara.hypericonlab.modules.icon.data.BuildArtifactWriter
import com.capybara.hypericonlab.modules.icon.data.local.BuildTaskStore
import com.capybara.hypericonlab.modules.icon.domain.usecase.BuildTaskExecutor
import com.capybara.hypericonlab.modules.icon.domain.usecase.BuildTaskManager
import com.capybara.hypericonlab.modules.icon.domain.usecase.GeneratePreviewUseCase
import com.capybara.hypericonlab.modules.icon.domain.usecase.IconPipelineUseCase
import com.capybara.hypericonlab.modules.icon.domain.usecase.ManageResourcesUseCase
import com.capybara.hypericonlab.modules.icon.viewmodel.IconViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val iconModule = module {
    factory { ManageResourcesUseCase(get()) }
    factory { GeneratePreviewUseCase(get()) }
    factory { IconPipelineUseCase(get()) }
    factory { BuildArtifactWriter(get()) }
    factory { BuildTaskStore(get()) }
    factory { BuildTaskExecutor(get(), get(), get(), get()) }
    single { BuildTaskManager(get(), get(), get()) }
    factory { BuildNotificationManager(get()) }
    viewModelOf(::IconViewModel)
}
