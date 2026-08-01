package com.capybara.hypericonlab.modules.icon.domain.iconpack

import com.capybara.hypericonlab.modules.icon.domain.lawnicons.IconPackTemplateManager
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsResourceManager
import java.io.File

// 串联模板按需获取、同版本资源读取和未签名 APK 装配，不负责最终签名与导出。
class IconPackApkBuildService(
    private val templateManager: IconPackTemplateManager,
    private val resourceManager: LawniconsResourceManager,
    private val assembler: IconPackApkAssembler
) {
    suspend fun buildUnsignedApk(
        iconSetId: String,
        renderedIconsZip: File,
        outputApk: File,
        onTemplateDownloadProgress: (Float) -> Unit = {}
    ): IconPackAssemblyResult {
        check(templateManager.ensureAvailable(onTemplateDownloadProgress)) {
            "当前 Lawnicons 资源版本没有可用的 APK 模板"
        }
        val provider = resourceManager.getProvider()
        val templateApk = provider.openIconPackTemplate(iconSetId)
            ?: error("未找到 $iconSetId APK 模板")
        val slotMapping = provider.openSlotMapping()
            ?: error("当前 Lawnicons 资源缺少 slot_mapping.json")
        return templateApk.use { templateInput ->
            slotMapping.use { mappingInput ->
                assembler.assemble(
                    templateApk = templateInput,
                    renderedIconsZip = renderedIconsZip,
                    slotMapping = mappingInput,
                    outputApk = outputApk
                )
            }
        }
    }
}
