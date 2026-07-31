package com.capybara.hypericonlab.modules.icon.domain.lawnicons

import android.content.Context
import com.capybara.hypericonlab.core.utils.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

// 云端资源下载服务：负责下载 zip、校验 sha256、解压到版本目录
// 下载文件存于 cacheDir，失败时自动清理，避免污染 filesDir
class LawniconsDownloadService(
    private val context: Context
) {

    // 下载 zip 到 cacheDir，返回缓存文件，失败返回 null
    // onProgress 回调 0~1 的下载进度
    suspend fun download(
        url: String,
        expectedSize: Long,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, DownloadConstants.CACHE_FILE_NAME)
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = DownloadConstants.METHOD_GET
                connectTimeout = DownloadConstants.CONNECT_TIMEOUT_MS
                readTimeout = DownloadConstants.READ_TIMEOUT_MS
                setRequestProperty(
                    DownloadConstants.HEADER_USER_AGENT,
                    DownloadConstants.USER_AGENT
                )
                setRequestProperty(DownloadConstants.HEADER_ACCEPT, DownloadConstants.ACCEPT_BINARY)
            }
            connection.connect()
            if (connection.responseCode != DownloadConstants.HTTP_OK) {
                return@withContext null
            }
            // 优先用 expectedSize，无则用响应头 content-length
            val total = expectedSize.takeIf { it > 0 }
                ?: connection.contentLengthLong.takeIf { it > 0 } ?: -1L
            cacheFile.outputStream().use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(DownloadConstants.BUFFER_SIZE)
                    var read: Int
                    var downloaded = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            onProgress((downloaded.toFloat() / total).coerceIn(0f, 0.99f))
                        }
                    }
                    output.flush()
                }
            }
            onProgress(1.0f)
            cacheFile
        } catch (_: Exception) {
            cacheFile.delete()
            null
        }
    }

    // 校验文件 sha256，expected 为空时跳过校验直接通过
    fun verifySha256(file: File, expected: String): Boolean {
        if (expected.isBlank()) return true
        val actual = MessageDigest.getInstance(DownloadConstants.SHA256_ALGORITHM)
            .digest(file.readBytes())
            .joinToString("") { DownloadConstants.HEX_FORMAT.format(it) }
        return actual.equals(expected, ignoreCase = true)
    }

    // 解压 zip 到目标目录，失败时清理半成品
    // onProgress 回调 0~1 的解压进度
    suspend fun extract(
        zipFile: File,
        targetDir: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            targetDir.mkdirs()
            ZipUtils.unzip(zipFile.inputStream(), targetDir, onProgress)
            true
        } catch (_: Exception) {
            targetDir.deleteRecursively()
            false
        }
    }

    // 清理下载缓存文件
    fun cleanupCache() {
        File(context.cacheDir, DownloadConstants.CACHE_FILE_NAME).delete()
    }

    private object DownloadConstants {
        const val CACHE_FILE_NAME = "lawnicons_update.zip"
        const val METHOD_GET = "GET"
        const val CONNECT_TIMEOUT_MS = 15000
        const val READ_TIMEOUT_MS = 60000
        const val USER_AGENT = "HyperIconLab-Android"
        const val ACCEPT_BINARY = "application/octet-stream"
        const val HEADER_USER_AGENT = "User-Agent"
        const val HEADER_ACCEPT = "Accept"
        const val HTTP_OK = 200
        const val BUFFER_SIZE = 8192
        const val SHA256_ALGORITHM = "SHA-256"
        const val HEX_FORMAT = "%02x"
    }
}
