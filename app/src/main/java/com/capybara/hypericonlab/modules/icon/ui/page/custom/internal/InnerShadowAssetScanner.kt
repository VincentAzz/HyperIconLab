package com.capybara.hypericonlab.modules.icon.ui.page.custom.internal

import android.content.Context
import com.capybara.hypericonlab.core.image.InnerShadowAssets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 内阴影资源扫描器
class InnerShadowAssetScanner(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _shadowAssetsMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val shadowAssetsMap: StateFlow<Map<String, List<String>>> = _shadowAssetsMap.asStateFlow()

    fun scan() {
        scope.launch(Dispatchers.IO) {
            _shadowAssetsMap.value = scanInnerShadowAssets()
        }
    }

    private suspend fun scanInnerShadowAssets(): Map<String, List<String>> =
        withContext(Dispatchers.IO) {
            val suffix = InnerShadowAssets.FILE_SUFFIX
            try {
                context.assets.list(InnerShadowAssets.DIR)
                    ?.asSequence()
                    ?.filter { it.endsWith(suffix) }
                    ?.mapNotNull { filename ->
                        // oneui_3d_shadow_512.png → shapeName="oneui", styleName="3d"
                        val core = filename.removeSuffix(suffix)
                        val firstUnderscore = core.indexOf('_')
                        if (firstUnderscore > 0) {
                            val shapeName = core.substring(0, firstUnderscore)
                            val styleName = core.substring(firstUnderscore + 1)
                            shapeName to styleName
                        } else null
                    }
                    ?.groupBy({ it.first }, { it.second })
                    ?.mapValues { (_, styles) -> styles.sorted() }
                    ?: emptyMap()
            } catch (_: Exception) {
                emptyMap()
            }
        }
}
