package com.capybara.hypericonlab.modules.icon.domain.model

import kotlinx.serialization.Serializable

/**
 * 构建任务状态。
 * - PENDING：已提交，等待执行
 * - RUNNING：执行中
 * - SUCCESS：成功，进入已完成列表
 * - FAILED：失败，进入已完成列表，可重试
 * - CANCELLED：被用户取消，不进入已完成列表（直接从队列移除）
 */
@Serializable
enum class BuildTaskStatus { PENDING, RUNNING, SUCCESS, FAILED, CANCELLED }

/**
 * 产物类型。
 * - ZIP_ICONS：zip 仅图标（首个支持，直接打包 PNG，无目录结构）
 * - MTZ：HyperOS 3 模板（待模板就绪后启用）
 * - ZIP_MAGISK：Magisk 模块（后续支持）
 * - APK：图标包 apk（后续支持）
 *
 * @param label UI 文案
 * @param ext 工件文件扩展名
 * @param enabled 当前是否在 UI 中可选
 */
@Serializable
enum class ProductType(
    val label: String,
    val ext: String,
    val mimeType: String,
    val enabled: Boolean
) {
    ZIP_ICONS("zip (仅图标)", "zip", "application/zip", true),
    MTZ("mtz (HyperOS 3)", "mtz", "application/octet-stream", false),
    ZIP_MAGISK("zip (Magisk 模块)", "zip", "application/zip", false),
    APK("apk (图标包)", "apk", "application/vnd.android.package-archive", true)
}

/**
 * 构建任务数据模型。
 *
 * taskId 规则：yyyyMMdd_HHmmss_<iconSetId>（时间戳在前便于在文件管理器中按时间排序）。
 * 同秒内提交多个任务时由调用方追加 _2、_3 后缀。
 * 重试任务使用重试时间点替换原 taskId。
 *
 * @param taskId 任务唯一标识
 * @param config 完整构建配置快照（用于实际执行）
 * @param configSnapshot UI 样式 chips 展示与重试所需的配置快照
 * @param productType 产物类型
 * @param iconSetId 图标集 id（对应 assets/icon_mapper/<id>.xml）
 * @param iconSetLabel 图标集展示名（如"测试集"/"常用集"）
 * @param iconCount 图标数量（解析 mapper 后得到）
 * @param wallpaperUri 壁纸引用，用于失败重试时重新解析颜色
 * @param submittedAt 提交时间戳
 * @param startedAt 实际开始执行时间戳
 * @param finishedAt 完成时间戳（成功/失败/取消均会写入）
 * @param durationMs 实际执行耗时（毫秒），仅成功/失败时记录
 * @param status 当前状态
 * @param progress 进度 0~1
 * @param currentPackage 当前正在处理的包名（用于通知与详情显示）
 * @param errorMessage 失败时的错误信息
 * @param artifactPath 导出后的目录 URI（Documents/HyperIconLabArtifacts/<taskId>/）
 */
@Serializable
data class BuildTask(
    val taskId: String,
    val config: IconBuildConfig,
    val configSnapshot: IconConfigState,
    val productType: ProductType,
    val iconSetId: String,
    val iconSetLabel: String,
    val iconCount: Int,
    val wallpaperUri: String? = null,
    val submittedAt: Long,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val durationMs: Long? = null,
    val status: BuildTaskStatus = BuildTaskStatus.PENDING,
    val progress: Float = 0f,
    val currentPackage: String? = null,
    val errorMessage: String? = null,
    val artifactPath: String? = null
) {
    companion object {
        // 任务 ID 时间戳格式（文件管理器按时间排序友好）
        const val TASK_ID_DATE_FORMAT = "yyyyMMdd_HHmmss"

        // 判断是否为终态（进入已完成列表）
        fun isTerminalStatus(status: BuildTaskStatus): Boolean =
            status == BuildTaskStatus.SUCCESS || status == BuildTaskStatus.FAILED
    }
}
