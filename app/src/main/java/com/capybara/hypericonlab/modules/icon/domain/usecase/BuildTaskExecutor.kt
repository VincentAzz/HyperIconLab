package com.capybara.hypericonlab.modules.icon.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.capybara.hypericonlab.core.mapper.IconMapperProcessor
import com.capybara.hypericonlab.core.utils.ZipUtils
import com.capybara.hypericonlab.modules.icon.data.BuildArtifactWriter
import com.capybara.hypericonlab.modules.icon.data.local.BuildTaskStore
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTask
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTaskStatus
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
 * 1. 从 assets 直读 mapper（icon_mapper/<iconSetId>.xml），解析为 Map
 * 2. 从 filesDir/lawnicons 查找 svgs 目录
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
    private val taskStore: BuildTaskStore
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

        // 临时工件文件（位于 filesDir/build_temp/，完成后删除）
        val tempDir = File(context.filesDir, ExecutorConfig.TEMP_DIRNAME).apply { mkdirs() }
        val tempArtifact = File(tempDir, "${task.taskId}.${task.productType.ext}")

        // 执行结果（success 或 failure 之一），由 Flow 事件设置
        var result: BuildTask? = null

        try {
            // 1. 从 assets 直读 mapper
            val mapperMap = context.assets
                .open("${ExecutorConfig.MAPPER_ASSET_DIR}/${task.iconSetId}.xml")
                .use { stream -> IconMapperProcessor.parseIconMapper(stream) }

            // 2. 查找 svgs 目录
            val lawniconsBase = File(context.filesDir, ExecutorConfig.LAWNICONS_DIRNAME)
            val svgDir = ZipUtils.findDirRecursive(lawniconsBase, ExecutorConfig.SVGS_DIRNAME)
                ?: throw IllegalStateException("未找到 svgs 目录，请先解压资源")

            // 3. 加载上层 mask bitmaps
            val maskBitmaps = task.config.masks.mapNotNull { loadMask(it) }
            // 3.1 加载下层 mask bitmaps（仅双层启用时）
            val maskBitmaps2 = if (task.config.dualLayerEnabled) {
                task.config.selectedMasks2.mapNotNull { loadMask(it) }
            } else emptyList()

            // 4. 调用流水线，写入临时文件
            pipeline.executeWithFiles(
                config = task.config,
                iconMap = mapperMap,
                svgDir = svgDir,
                maskBitmaps = maskBitmaps,
                outputFile = tempArtifact,
                appColorSchemes = appColorSchemes,
                maskBitmaps2 = maskBitmaps2
            ).collect { state ->
                // 协作式取消检查：在每个进度事件时响应取消
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
                        // 5. 导出工件与预览图到公共 Documents
                        val artifactName =
                            "${ExecutorConfig.DEFAULT_ARTIFACT_BASENAME}.${task.productType.ext}"
                        val exportedPath = artifactWriter.export(
                            taskId = task.taskId,
                            artifactFile = tempArtifact,
                            storePreview = storePreview,
                            mainPreview = mainPreview,
                            artifactName = artifactName
                        )
                        if (exportedPath == null) {
                            throw IllegalStateException("工件导出失败（可能缺少存储权限）")
                        }

                        // 6. 裁切缩略图并持久化预览图
                        val thumbnail = cropThumbnail(storePreview)
                        taskStore.saveThumbnail(task.taskId, thumbnail)
                        taskStore.savePreview(task.taskId, storePreview)
                        if (thumbnail != storePreview) thumbnail.recycle()

                        val finishedAt = System.currentTimeMillis()
                        val successTask = current.copy(
                            status = BuildTaskStatus.SUCCESS,
                            progress = 1f,
                            currentPackage = null,
                            finishedAt = finishedAt,
                            durationMs = finishedAt - startedAt,
                            artifactPath = exportedPath
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
            // 协程取消：重新抛出，保持取消语义，由 BuildTaskManager 设置 CANCELLED 状态
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
        }
    }

    // 从 assets 加载单个 mask bitmap，失败返回 null
    private fun loadMask(name: String): Bitmap? = try {
        context.assets.open("${ExecutorConfig.MASKS_DIRNAME}/${ExecutorConfig.MASK_FILE_PREFIX}${name}${ExecutorConfig.MASK_FILE_SUFFIX}")
            .use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        Timber.tag(TAG).w(e, "Mask not found: $name")
        null
    }

    // 从 store preview 裁切左上 2 图标区域作为缩略图（540×320 = 1080×640 的 1/2×1/2）
    private fun cropThumbnail(storePreview: Bitmap): Bitmap {
        val w = minOf(ExecutorConfig.THUMBNAIL_WIDTH, storePreview.width)
        val h = minOf(ExecutorConfig.THUMBNAIL_HEIGHT, storePreview.height)
        return Bitmap.createBitmap(storePreview, 0, 0, w, h)
    }

    companion object {
        private const val TAG = "BuildTaskExecutor"

        // 执行流程关键参数集中声明，便于调参
        private object ExecutorConfig {
            // assets 中 mapper 文件所在目录
            const val MAPPER_ASSET_DIR = "icon_mapper"

            // filesDir 中 lawnicons 解压目录名
            const val LAWNICONS_DIRNAME = "lawnicons"

            // lawnicons 下的 svg 目录名
            const val SVGS_DIRNAME = "svgs"

            // assets 中 mask 文件所在目录
            const val MASKS_DIRNAME = "masks"

            // mask 文件名前缀与后缀（与 IconViewModel 保持一致）
            const val MASK_FILE_PREFIX = "mask_"
            const val MASK_FILE_SUFFIX = "_512.png"

            // 临时工件目录名（位于 filesDir 根下）
            const val TEMP_DIRNAME = "build_temp"

            // 工件文件基础名（不含扩展名，扩展名由 ProductType.ext 决定）
            const val DEFAULT_ARTIFACT_BASENAME = "icons"

            // 缩略图尺寸：store preview (1080×640) 的左上 1/2×1/2
            const val THUMBNAIL_WIDTH = 540
            const val THUMBNAIL_HEIGHT = 320
        }
    }
}
