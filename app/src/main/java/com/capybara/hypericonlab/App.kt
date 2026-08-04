package com.capybara.hypericonlab

import android.app.Application
import com.capybara.hypericonlab.modules.icon.di.iconModule
import com.capybara.hypericonlab.modules.icon.domain.usecase.InitializationCoordinator
import com.capybara.hypericonlab.modules.render.image.StickerProcessor
import com.capybara.hypericonlab.modules.settings.di.settingsModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val appScopeModule = module {
    single<CoroutineScope>(named("AppScope")) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appScopeModule, settingsModule, iconModule)
        }

        GlobalContext.get().get<InitializationCoordinator>().startInitialization()

        StickerProcessor.init(this)
    }
}
