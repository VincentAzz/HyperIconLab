package com.capybara.hypericonlab.modules.icon.di

import com.capybara.hypericonlab.modules.icon.domain.usecase.GeneratePreviewUseCase
import com.capybara.hypericonlab.modules.icon.domain.usecase.IconPipelineUseCase
import com.capybara.hypericonlab.modules.icon.domain.usecase.ManageResourcesUseCase
import com.capybara.hypericonlab.modules.icon.ui.page.custom.IconViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val iconModule = module {
    factory { ManageResourcesUseCase(get()) }
    factory { GeneratePreviewUseCase(get()) }
    factory { IconPipelineUseCase(get()) }
    viewModelOf(::IconViewModel)
}
