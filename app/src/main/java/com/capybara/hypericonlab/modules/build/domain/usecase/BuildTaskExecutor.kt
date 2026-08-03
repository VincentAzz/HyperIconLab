package com.capybara.hypericonlab.modules.build.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.capybara.hypericonlab.modules.build.data.BuildArtifactWriter
import com.capybara.hypericonlab.modules.build.data.local.BuildTaskStore
import com.capybara.hypericonlab.modules.build.domain.model.BuildTask
import com.capybara.hypericonlab.modules.build.domain.model.BuildTaskStatus
import com.capybara.hypericonlab.modules.build.domain.model.ProductType
import com.capybara.hypericonlab.modules.build.domain.packaging.IconPackApkBuildService
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsAssetFacade
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.mapper.IconMapperProcessor
import com.capybara.hypericonlab.modules.icon.domain.model.IconSetInfo
import com.capybara.hypericonlab.modules.icon.domain.usecase.IconPipelineUseCase
import com.capybara.hypericonlab.modules.render.image.InnerShadowProcessor
import com.capybara.hypericonlab.modules.render.image.MaskAssetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * 构建任务执行器：消费单个 [BuildTask]，调用 [IconPipelineUseCase] 生成图标，
 * 完成后通过 [BuildArtifactWriter] 导出工件与预览图，并持久化缩略图与预览图。
 *
 * 执行流程：
 * 1. 通过 [LawniconsResourceManager] 获取当前激活资源，读取 mapper 并解析为 Map
 * 2. 通过 provider 获取 svgs 目录
 * 3. 从 assets/masks 加载上下层 mask bitmaps
 * 4. 调用 [IconPipelineUseCase.executeWithFiles]，写入临时文件 filesDir/build_temp/<taskId>.<ext>
 * 5. 消费 [IconPipelineUseCase.PipelineProgress] Flow，协作式取消 + 实时回调进度
 * 6. 完成后：导出工件 + 预览图到公共 Documents，裁切缩略图并持久化，删除临时文件
 *
 * 协作式取消：在每个 Processing 事件处理时调用 [ensureActive]，及时响应协程取消。
 *
 * @param onUpdate 进度回调，回调参数为更新后的 [BuildTask] 副本，调用方据此更新 StateFlow
 * @return 终态任务（SUCCESS 或 FAILED）；CANCELLED 状态由调用方在捕获 CancellationException 时设置
 */
class BuildTaskExecutor(
    private val context: Context,
    private val pipeline: IconPipelineUseCase,
    private val artifactWriter: BuildArtifactWriter,
    private val taskStore: BuildTaskStore,
    private val assetsFacade: LawniconsAssetFacade,
    private val iconPackApkBuildService: IconPackApkBuildService
) {

    suspend fun execute(
        task: BuildTask,
        appColorSchemes: Map<String, Pair<String, String>>,
        storePreview: Bitmap,
        mainPreview: Bitmap,
        onUpdate: (BuildTask) -> Unit
    ): BuildTask = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        var current = task.copy(
            status = BuildTaskStatus.RUNNING,
            startedAt = startedAt,
            progress = 0f
        )
        onUpdate(current)

        val tempDir = File(context.filesDir, ExecutorConfig.TEMP_DIRNAME).apply { mkdirs() }
        val tempArtifact = File(tempDir, "${task.taskId}.${task.productType.ext}")
        val renderedIconsZip = if (task.productType == ProductType.APK) {
            File(tempDir, "${task.taskId}${ExecutorConfig.RENDERED_ICONS_SUFFIX}")
        } else {
            tempArtifact
        }

        var result: BuildTask? = null

        try {
            val provider = assetsFacade.getProvider()
            val mapperFileName = IconSetInfo.mapperFileName(task.iconSetId)
            val mapperMap = provider.openIconMapper(mapperFileName)
                .use { stream -> IconMapperProcessor.parseIconMapper(stream) }

            if (current.iconCount != mapperMap.size) {
                current = current.copy(iconCount = mapperMap.size)
                onUpdate(current)
            }

            val svgDir = provider.getSvgDir()
                ?: throw IllegalStateException("未找到 svgs 目录，请先解压资源")

            val maskBitmaps = task.config.masks.mapNotNull { loadMask(it) }
            val maskBitmaps2 = if (task.config.dualLayerEnabled) {
                task.config.selectedMasks2.mapNotNull { loadMask(it) }
            } else emptyList()

            val innerShadowBitmap =
                if (task.config.innerShadow.enabled && !task.config.dualLayerEnabled) {
                    task.config.innerShadow.styleName?.let { styleName ->
                        task.config.masks.firstOrNull()?.let { shapeName ->
                            loadInnerShadow(
                                shapeName,
                                styleName,
                                task.config.iconSize
                            )?.let { raw ->
                                // 预合并多层阴影为单张阴影层，管线内每个图标只绘制一次
                                val merged = InnerShadowProcessor.mergeShadowLayers(
                                    raw, task.config.innerShadow.intensityLayers
                                )
                                raw.recycle()
                                merged
                            }
                        }
                    }
                } else null

            pipeline.executeWithFiles(
                config = task.config,
                iconMap = mapperMap,
                svgDir = svgDir,
                maskBitmaps = maskBitmaps,
                outputFile = renderedIconsZip,
                appColorSchemes = appColorSchemes,
                maskBitmaps2 = maskBitmaps2,
                innerShadowBitmap = innerShadowBitmap
            ).collect { state ->
                currentCoroutineContext().ensureActive()
                when (state) {
                    is IconPipelineUseCase.PipelineProgress.Processing -> {
                        val progress = if (state.total > 0) {
                            state.current.toFloat() / state.total
                        } else 0f
                        current = current.copy(
                            progress = progress,
                            currentPackage = state.packageName
                        )
                        onUpdate(current)
                    }

                    is IconPipelineUseCase.PipelineProgress.Complete -> {
                        if (task.productType == ProductType.APK) {
                            iconPackApkBuildService.buildSignedApk(
                                iconSetId = task.iconSetId,
                                renderedIconsZip = renderedIconsZip,
                                outputApk = tempArtifact
                            )
                        }
                        val artifactName =
                            "${ExecutorConfig.DEFAULT_ARTIFACT_BASENAME}.${task.productType.ext}"
                        val exported = artifactWriter.export(
                            taskId = task.taskId,
                            artifactFile = tempArtifact,
                            storePreview = storePreview,
                            mainPreview = mainPreview,
                            artifactName = artifactName,
                            productType = task.productType
                        ) ?: run {
                            throw IllegalStateException("工件导出失败（可能缺少存储权限）")
                        }

                        val finishedAt = System.currentTimeMillis()
                        val successTask = current.copy(
                            status = BuildTaskStatus.SUCCESS,
                            progress = 1f,
                            currentPackage = null,
                            finishedAt = finishedAt,
                            durationMs = finishedAt - startedAt,
                            artifactPath = exported.displayPath,
                            artifactUri = if (task.productType == ProductType.APK) {
                                exported.artifactUri?.toString()
                            } else {
                                null
                            }
                        )
                        onUpdate(successTask)
                        result = successTask
                    }

                    is IconPipelineUseCase.PipelineProgress.Error -> {
                        throw IllegalStateException(state.message)
                    }
                }
            }

            // result 为 null 表示 Flow 正常结束但未收到 Complete，按失败处理
            result ?: current.copy(
                status = BuildTaskStatus.FAILED,
                finishedAt = System.currentTimeMillis(),
                durationMs = System.currentTimeMillis() - startedAt,
                errorMessage = "流水线异常结束"
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Build task ${task.taskId} failed")
            val finishedAt = System.currentTimeMillis()
            current.copy(
                status = BuildTaskStatus.FAILED,
                finishedAt = finishedAt,
                durationMs = finishedAt - startedAt,
                errorMessage = e.message ?: e.javaClass.simpleName
            )
        } finally {
            // 清理临时工件文件（无论成功失败或取消）
            tempArtifact.delete()
            if (renderedIconsZip != tempArtifact) renderedIconsZip.delete()
        }
    }

    // 从 assets 加载单个 mask bitmap，自动适配 _common 后缀，失败返回 null
    private fun loadMask(name: String): Bitmap? = MaskAssetLoader.loadBitmap(context, name)

    // 从 assets 加载烘焙内阴影 PNG，失败返回 null
    private fun loadInnerShadow(shapeName: String, styleName: String, targetSize: Int): Bitmap? =
        try {
            context.assets.open("${ExecutorConfig.SHADOW_DIRNAME}/${shapeName}_${styleName}${ExecutorConfig.SHADOW_FILE_SUFFIX}")
                .use { BitmapFactory.decodeStream(it) }
                ?.let { raw ->
                    if (raw.width != targetSize) {
                        val scaled = Bitmap.createScaledBitmap(raw, targetSize, targetSize, true)
                        raw.recycle()
                        scaled
                    } else raw
                }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Inner shadow not found: $shapeName/$styleName")
            null
        }

    // 从 store preview 裁切左上 2 图标区域作为缩略图（540×320 = 1080×640 的 1/2×1/2）
    // public：BuildTaskManager.submit 在提交时即调用以持久化缩略图，任务卡片 PENDING 即可显示
    fun cropThumbnail(storePreview: Bitmap): Bitmap {
        val w = minOf(ExecutorConfig.THUMBNAIL_WIDTH, storePreview.width)
        val h = minOf(ExecutorConfig.THUMBNAIL_HEIGHT, storePreview.height)
        return Bitmap.createBitmap(storePreview, 0, 0, w, h)
    }

    companion object {
        private const val TAG = "BuildTaskExecutor"

        private object ExecutorConfig {
            // assets 中烘焙内阴影文件所在目录（与 IconViewModel 保持一致）
            const val SHADOW_DIRNAME = "shadow_baked"

            // 内阴影文件名后缀（与 IconViewModel 保持一致）
            const val SHADOW_FILE_SUFFIX = "_shadow_512.png"

            // 临时工件目录名（位于 filesDir 根下）
            const val TEMP_DIRNAME = "build_temp"

            // 工件文件基础名（不含扩展名，扩展名由 ProductType.ext 决定）
            const val DEFAULT_ARTIFACT_BASENAME = "icons"

            const val RENDERED_ICONS_SUFFIX = ".rendered-icons.zip"

            // 缩略图尺寸：store preview (1080×640) 的左上 1/2×1/2
            const val THUMBNAIL_WIDTH = 540
            const val THUMBNAIL_HEIGHT = 320
        }
    }
}
