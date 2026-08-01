package com.capybara.hypericonlab.modules.icon.domain.iconpack

import com.capybara.hypericonlab.modules.icon.domain.lawnicons.IconPackTemplateManager
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsResourceManager
import java.io.File

// 串联模板按需获取、同版本资源读取、APK 装配和签名，不负责 Documents 导出。
class IconPackApkBuildService(
    private val templateManager: IconPackTemplateManager,
    private val resourceManager: LawniconsResourceManager,
    private val assembler: IconPackApkAssembler,
    private val keyManager: IconPackSigningKeyManager,
    private val signer: IconPackApkSigner
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

    suspend fun buildSignedApk(
        iconSetId: String,
        renderedIconsZip: File,
        outputApk: File,
        onTemplateDownloadProgress: (Float) -> Unit = {}
    ): IconPackSigningResult {
        outputApk.parentFile?.mkdirs()
        val unsignedApk = File(outputApk.parentFile, "${outputApk.name}.unsigned")
        unsignedApk.delete()
        return try {
            buildUnsignedApk(
                iconSetId = iconSetId,
                renderedIconsZip = renderedIconsZip,
                outputApk = unsignedApk,
                onTemplateDownloadProgress = onTemplateDownloadProgress
            )
            signer.signAndVerify(
                unsignedApk = unsignedApk,
                outputApk = outputApk,
                identity = keyManager.getOrCreate()
            )
        } finally {
            unsignedApk.delete()
        }
    }
}
