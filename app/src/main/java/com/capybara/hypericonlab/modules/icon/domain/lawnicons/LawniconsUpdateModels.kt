package com.capybara.hypericonlab.modules.icon.domain.lawnicons

// 云端 release 资源信息：由 ApiService 从 GitHub Release + manifest.json 合并解析得出
// 供 UpdateManager 判断版本新旧、下载校验、展示更新日志
data class ReleaseInfo(
    val version: String,          // 版本号，如 "20260731"
    val zipUrl: String,           // lawnicons_<version>.zip 下载地址
    val zipSizeBytes: Long,       // zip 文件大小（字节），用于进度计算
    val sha256: String,           // zip 哈希校验值（可能为空，manifest 未提供时跳过校验）
    val lawniconsCommit: String,  // lawnicons 上游 commit hash
    val generatedAt: String,      // 资源生成时间
    val totalIcons: Int,          // mapper item 总数（唯一 package 数）
    val addedIcons: Int,          // 本次新增图标数
    val removedIcons: Int,        // 本次删除图标数
    val modifiedIcons: Int        // 本次修改图标数
)

// 更新失败原因分类：供 UI 显示针对性文案、通知栏推送
enum class FailureReason {
    RATE_LIMITED,    // GitHub API 限速（403），提示切换网络或稍后重试
    NETWORK_ERROR,   // 网络不可达（UnknownHost/连接失败）
    TIMEOUT,         // 网络超时（connect/read timeout）
    HTTP_ERROR,      // HTTP 非 200 响应（非限速）
    CORRUPTED,       // 文件校验失败（sha256 不匹配）
    PARSE_ERROR,     // 响应解析失败（JSON 异常）
    EXTRACT_FAILED,  // 解压失败
    ACTIVATE_FAILED, // 激活切换失败
    UNKNOWN          // 未知错误
}

// 更新流程异常：携带 FailureReason，供 ApiService/DownloadService 抛出、UpdateManager 捕获
class LawniconsUpdateException(
    val reason: FailureReason,
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)

// 更新流程状态：供 UI 观察当前更新进度与结果
sealed class UpdateState {
    // 空闲
    object Idle : UpdateState()

    // 检查更新中，current 为当前激活版本号
    data class Checking(val current: String) : UpdateState()

    // 下载中，progress 0~1
    data class Downloading(val progress: Float) : UpdateState()

    // 解压中，progress 0~1
    data class Extracting(val progress: Float) : UpdateState()

    // 更新成功，newVersion 为新激活版本号
    data class Success(val newVersion: String) : UpdateState()

    // 更新失败，reason 为失败原因分类
    data class Failed(val reason: FailureReason) : UpdateState()

    // 已是最新版本
    object UpToDate : UpdateState()
}
