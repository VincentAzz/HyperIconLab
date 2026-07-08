package com.capybara.hypericonlab.modules.settings.data.provider

import android.os.Build
import com.capybara.hypericonlab.modules.settings.domain.provider.SystemEnvProvider
import com.kieronquinn.monetcompat.core.MonetCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

class SystemEnvProviderImpl : SystemEnvProvider {
    override fun getWallpaperColorsFlow(): Flow<List<Int>?> = flow {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val colors = try {
                MonetCompat.getInstance().getAvailableWallpaperColors()
            } catch (e: Exception) {
                Timber.e(e, "Failed to get Monet wallpaper colors")
                null
            }
            emit(colors)
        } else emit(null)
    }.flowOn(Dispatchers.IO)
}
