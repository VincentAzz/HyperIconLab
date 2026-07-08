package com.capybara.hypericonlab.modules.settings.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.capybara.hypericonlab.modules.settings.data.local.AppDataStore
import com.capybara.hypericonlab.modules.settings.data.provider.SystemEnvProviderImpl
import com.capybara.hypericonlab.modules.settings.data.provider.ThemeStateProviderImpl
import com.capybara.hypericonlab.modules.settings.data.repository.AppSettingsRepositoryImpl
import com.capybara.hypericonlab.modules.settings.domain.provider.SystemEnvProvider
import com.capybara.hypericonlab.modules.settings.domain.provider.ThemeStateProvider
import com.capybara.hypericonlab.modules.settings.domain.repository.AppSettingsRepository
import com.capybara.hypericonlab.modules.settings.domain.usecase.UpdateSettingUseCase
import com.capybara.hypericonlab.modules.settings.ui.page.settings.SettingsSharedViewModel
import com.capybara.hypericonlab.modules.settings.ui.page.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val APP_SETTINGS_DATASTORE_NAME = "app_settings"

val settingsModule = module {
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create {
            androidContext().preferencesDataStoreFile(APP_SETTINGS_DATASTORE_NAME)
        }
    }

    singleOf(::AppDataStore)

    single<AppSettingsRepository> {
        AppSettingsRepositoryImpl(
            appDataStore = get(),
            appScope = get(named("AppScope"))
        )
    }

    singleOf(::SystemEnvProviderImpl) { bind<SystemEnvProvider>() }

    single<ThemeStateProvider> {
        ThemeStateProviderImpl(
            appSettingsRepo = get<AppSettingsRepository>(),
            systemEnvProvider = get(),
            appScope = get(named("AppScope"))
        )
    }

    factoryOf(::UpdateSettingUseCase)

    viewModelOf(::SettingsSharedViewModel)
    viewModelOf(::SettingsViewModel)
}
