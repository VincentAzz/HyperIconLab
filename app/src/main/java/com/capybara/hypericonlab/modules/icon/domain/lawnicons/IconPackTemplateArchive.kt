package com.capybara.hypericonlab.modules.icon.domain.lawnicons

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream

// 模板索引模型：与 CI 生成的 iconpack-templates-<version>.json 对应。
@Serializable
data class IconPackTemplateIndex(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("resource_version") val resourceVersion: String,
    @SerialName("lawnicons_commit") val lawniconsCommit: String,
    val templates: Map<String, IconPackTemplateInfo>
)

@Serializable
data class IconPackTemplateInfo(
    val filename: String,
    @SerialName("mapper_file") val mapperFile: String,
    @SerialName("application_id") val applicationId: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    val sha256: String
)

// 负责安全解压并验证模板 ZIP；不接触网络和激活状态。
class IconPackTemplateArchive {
    private val json = Json { ignoreUnknownKeys = true }

    fun extractAndValidate(
        archiveFile: File,
        targetDir: File,
        expectedVersion: String,
        expectedCommit: String
    ): IconPackTemplateIndex {
        require(!targetDir.exists()) { "模板暂存目录已存在" }
        targetDir.mkdirs()
        try {
            extractArchive(archiveFile, targetDir, expectedVersion)
            return validateDirectory(targetDir, expectedVersion, expectedCommit)
        } catch (e: Exception) {
            targetDir.deleteRecursively()
            throw e
        }
    }

    fun validateDirectory(
        directory: File,
        expectedVersion: String,
        expectedCommit: String
    ): IconPackTemplateIndex {
        val indexFile = File(directory, indexFileName(expectedVersion))
        require(indexFile.isFile) { "模板索引不存在" }
        val index = json.decodeFromString<IconPackTemplateIndex>(indexFile.readText())
        require(index.schemaVersion == ArchiveConstants.SCHEMA_VERSION) {
            "不支持的模板 schema: ${index.schemaVersion}"
        }
        require(index.resourceVersion == expectedVersion) { "模板资源版本不一致" }
        require(expectedCommit.isNotBlank() && index.lawniconsCommit == expectedCommit) {
            "模板 Lawnicons commit 不一致"
        }
        require(index.templates.keys == ArchiveConstants.TEMPLATE_IDS) { "模板集合不完整" }

        index.templates.forEach { (iconSetId, template) ->
            val expectedName = templateFileName(iconSetId, expectedVersion)
            require(template.filename == expectedName) { "$iconSetId 模板文件名不一致" }
            require(File(template.filename).name == template.filename) { "模板文件名包含路径" }
            val apkFile = File(directory, template.filename)
            require(apkFile.isFile) { "$iconSetId 模板不存在" }
            require(apkFile.length() == template.sizeBytes) { "$iconSetId 模板大小不一致" }
            require(computeSha256(apkFile).equals(template.sha256, ignoreCase = true)) {
                "$iconSetId 模板 SHA-256 不一致"
            }
        }
        return index
    }

    private fun extractArchive(archiveFile: File, targetDir: File, version: String) {
        val expectedEntries = buildSet {
            add(indexFileName(version))
            ArchiveConstants.TEMPLATE_IDS.forEach { add(templateFileName(it, version)) }
        }
        val extractedEntries = mutableSetOf<String>()
        var extractedBytes = 0L

        ZipInputStream(archiveFile.inputStream().buffered()).use { zipInput ->
            var entry = zipInput.nextEntry
            while (entry != null) {
                val name = entry.name
                require(!entry.isDirectory && name in expectedEntries) { "模板 ZIP 包含非法条目: $name" }
                require(extractedEntries.add(name)) { "模板 ZIP 包含重复条目: $name" }
                val outputFile = File(targetDir, name)
                outputFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(ArchiveConstants.BUFFER_SIZE)
                    var read: Int
                    while (zipInput.read(buffer).also { read = it } != -1) {
                        extractedBytes += read
                        require(extractedBytes <= ArchiveConstants.MAX_EXTRACTED_BYTES) {
                            "模板 ZIP 解压大小超过限制"
                        }
                        output.write(buffer, 0, read)
                    }
                }
                zipInput.closeEntry()
                entry = zipInput.nextEntry
            }
        }
        require(extractedEntries == expectedEntries) { "模板 ZIP 条目不完整" }
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance(ArchiveConstants.SHA256_ALGORITHM)
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(ArchiveConstants.BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { ArchiveConstants.HEX_FORMAT.format(it) }
    }

    private fun indexFileName(version: String) = "iconpack-templates-$version.json"

    private fun templateFileName(iconSetId: String, version: String) =
        "iconpack-template-$iconSetId-$version.apk"

    private object ArchiveConstants {
        const val SCHEMA_VERSION = 1
        const val BUFFER_SIZE = 8192
        const val MAX_EXTRACTED_BYTES = 128L * 1024L * 1024L
        const val SHA256_ALGORITHM = "SHA-256"
        const val HEX_FORMAT = "%02x"
        val TEMPLATE_IDS = setOf("full", "filtered", "preview", "test")
    }
}
