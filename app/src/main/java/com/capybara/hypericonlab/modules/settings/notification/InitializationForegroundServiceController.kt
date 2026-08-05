package com.capybara.hypericonlab.modules.settings.notification

import android.content.Context
import androidx.core.content.ContextCompat
import com.capybara.hypericonlab.modules.icon.domain.repository.InitializationServiceController

class InitializationForegroundServiceController(
    private val context: Context
) : InitializationServiceController {
    override fun start() {
        ContextCompat.startForegroundService(
            context,
            InitializationForegroundService.createIntent(context)
        )
    }

    override fun stop() {
        context.stopService(InitializationForegroundService.createIntent(context))
    }
}
