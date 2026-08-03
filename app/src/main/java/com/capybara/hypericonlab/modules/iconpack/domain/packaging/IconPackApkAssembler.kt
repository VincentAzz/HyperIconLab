package com.capybara.hypericonlab.modules.iconpack.domain.packaging

import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class IconPackAssemblyResult(
    val outputFile: File,
    val replacedIconCount: Int,
    val templateSlotCount: Int
)

// 使用渲染 PNG 替换模板槽位，输出保持未签名且重新对齐的 APK。
class IconPackApkAssembler {
    private val json = Json { ignoreUnknownKeys = true }

    fun assemble(
        templateApk: InputStream,
        renderedIconsZip: File,
        slotMapping: InputStream,
        outputApk: File
    ): IconPackAssemblyResult {
        val packageSlots = parseSlotMapping(slotMapping)
        val renderedIcons = readRenderedIcons(renderedIconsZip, packageSlots)
        outputApk.parentFile?.mkdirs()
        val temporaryOutput = File(outputApk.parentFile, "${outputApk.name}.assembling")
        temporaryOutput.delete()

        return try {
            val assemblyStats = rewriteTemplate(
                templateApk = templateApk,
                renderedIcons = renderedIcons,
                outputFile = temporaryOutput
            )
            validateOutput(
                apkFile = temporaryOutput,
                expectedSlots = assemblyStats.templateSlots,
                expectedReplacements = renderedIcons.keys
            )
            if (outputApk.exists() && !outputApk.delete()) error("无法覆盖旧 APK 产物")
            if (!temporaryOutput.renameTo(outputApk)) error("无法切换 APK 临时产物")
            IconPackAssemblyResult(
                outputFile = outputApk,
                replacedIconCount = renderedIcons.size,
                templateSlotCount = assemblyStats.templateSlots.size
            )
        } catch (e: Exception) {
            temporaryOutput.delete()
            throw e
        }
    }

    private fun parseSlotMapping(input: InputStream): Map<String, String> {
        val mapping = input.bufferedReader().use { reader ->
            json.decodeFromString<Map<String, String>>(reader.readText())
        }
        require(mapping.isNotEmpty()) { "slot_mapping.json 为空" }
        mapping.forEach { (packageName, slotName) ->
            require(packageName.isNotBlank()) { "slot mapping 包含空包名" }
            require(AssemblerConfig.SLOT_NAME_PATTERN.matches(slotName)) {
                "槽位名称格式无效: $slotName"
            }
        }
        return mapping
    }

    private fun readRenderedIcons(
        iconsZip: File,
        packageSlots: Map<String, String>
    ): Map<String, ByteArray> {
        val renderedIcons = linkedMapOf<String, ByteArray>()
        val seenEntries = mutableSetOf<String>()
        ZipInputStream(iconsZip.inputStream().buffered()).use { zipInput ->
            var entry = zipInput.nextEntry
            while (entry != null) {
                require(!entry.isDirectory && seenEntries.add(entry.name)) {
                    "图标 ZIP 包含目录或重复条目: ${entry.name}"
                }
                val match = AssemblerConfig.ICON_ENTRY_PATTERN.matchEntire(entry.name)
                    ?: error("图标 ZIP 条目格式无效: ${entry.name}")
                val packageName = match.groupValues[1]
                val slotName = packageSlots[packageName]
                    ?: error("包名未分配稳定槽位: $packageName")
                require(slotName !in renderedIcons) { "多个图标映射到同一槽位: $slotName" }
                val png = zipInput.readBoundedBytes(AssemblerConfig.MAX_ICON_BYTES)
                require(png.startsWith(AssemblerConfig.PNG_SIGNATURE)) {
                    "图标不是有效 PNG: $packageName"
                }
                renderedIcons[slotName] = png
                zipInput.closeEntry()
                entry = zipInput.nextEntry
            }
        }
        require(renderedIcons.isNotEmpty()) { "图标 ZIP 不包含可装配 PNG" }
        return renderedIcons
    }

    private fun rewriteTemplate(
        templateApk: InputStream,
        renderedIcons: Map<String, ByteArray>,
        outputFile: File
    ): AssemblyStats {
        val seenEntries = mutableSetOf<String>()
        val templateSlots = mutableSetOf<String>()
        val replacedSlots = mutableSetOf<String>()
        val countingOutput = CountingOutputStream(outputFile.outputStream().buffered())

        ZipInputStream(templateApk.buffered()).use { zipInput ->
            ZipOutputStream(countingOutput).use { zipOutput ->
                var entry = zipInput.nextEntry
                while (entry != null) {
                    val name = entry.name
                    require(seenEntries.add(name)) { "模板 APK 包含重复条目: $name" }
                    if (!isSignatureEntry(name) && !entry.isDirectory) {
                        val slotMatch = AssemblerConfig.SLOT_ENTRY_PATTERN.matchEntire(name)
                        val slotName = slotMatch?.groupValues?.get(1)
                        if (slotName != null) templateSlots.add(slotName)
                        val content = if (slotName != null && slotName in renderedIcons) {
                            replacedSlots.add(slotName)
                            renderedIcons.getValue(slotName)
                        } else {
                            zipInput.readBoundedBytes(AssemblerConfig.MAX_TEMPLATE_ENTRY_BYTES)
                        }
                        writeEntry(zipOutput, countingOutput.count, entry, content)
                    }
                    zipInput.closeEntry()
                    entry = zipInput.nextEntry
                }
            }
        }
        require(replacedSlots == renderedIcons.keys) {
            "模板缺少待替换槽位: ${renderedIcons.keys - replacedSlots}"
        }
        return AssemblyStats(templateSlots, replacedSlots)
    }

    private fun writeEntry(
        output: ZipOutputStream,
        currentOffset: Long,
        source: ZipEntry,
        content: ByteArray
    ) {
        val target = ZipEntry(source.name).apply {
            method = source.method
            time = source.time
            comment = source.comment
        }
        if (target.method == ZipEntry.STORED) {
            val crc = CRC32().apply { update(content) }.value
            target.size = content.size.toLong()
            target.compressedSize = content.size.toLong()
            target.crc = crc
            target.extra = createAlignmentExtra(currentOffset, target.name)
        }
        output.putNextEntry(target)
        output.write(content)
        output.closeEntry()
    }

    private fun createAlignmentExtra(localHeaderOffset: Long, entryName: String): ByteArray {
        val nameSize = entryName.toByteArray(Charsets.UTF_8).size
        val dataSize = (
                (
                        AssemblerConfig.ALIGNMENT -
                                ((localHeaderOffset + AssemblerConfig.LOCAL_HEADER_SIZE + nameSize + 4) %
                                        AssemblerConfig.ALIGNMENT)
                        ) % AssemblerConfig.ALIGNMENT
                ).toInt()
        return ByteArray(4 + dataSize).apply {
            this[0] = (AssemblerConfig.ALIGNMENT_EXTRA_ID and 0xff).toByte()
            this[1] = (AssemblerConfig.ALIGNMENT_EXTRA_ID ushr 8).toByte()
            this[2] = (dataSize and 0xff).toByte()
            this[3] = (dataSize ushr 8).toByte()
        }
    }

    private fun validateOutput(
        apkFile: File,
        expectedSlots: Set<String>,
        expectedReplacements: Set<String>
    ) {
        ZipFile(apkFile).use { archive ->
            val names = archive.entries().asSequence().map { it.name }.toList()
            require(names.size == names.toSet().size) { "装配后 APK 包含重复条目" }
            require(AssemblerConfig.REQUIRED_APK_ENTRIES.all(names::contains)) {
                "装配后 APK 缺少必要结构"
            }
            require(names.none(::isSignatureEntry)) { "装配后 APK 仍包含旧签名" }
            val actualSlots = names.mapNotNull { name ->
                AssemblerConfig.SLOT_ENTRY_PATTERN.matchEntire(name)?.groupValues?.get(1)
            }.toSet()
            require(actualSlots == expectedSlots) { "装配前后模板槽位集合不一致" }
            require(expectedReplacements.all(actualSlots::contains)) { "装配槽位替换不完整" }
        }
        ApkAlignmentValidator.validate(apkFile)
    }

    private fun isSignatureEntry(name: String): Boolean {
        if (!name.startsWith(AssemblerConfig.SIGNATURE_DIR, ignoreCase = true)) return false
        return AssemblerConfig.SIGNATURE_SUFFIXES.any { name.endsWith(it, ignoreCase = true) }
    }

    private fun InputStream.readBoundedBytes(maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(AssemblerConfig.BUFFER_SIZE)
        var total = 0
        var read: Int
        while (read(buffer).also { read = it } != -1) {
            total += read
            require(total <= maxBytes) { "ZIP 条目超过大小限制" }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private data class AssemblyStats(
        val templateSlots: Set<String>,
        val replacedSlots: Set<String>
    )

    private class CountingOutputStream(output: OutputStream) : FilterOutputStream(output) {
        var count: Long = 0
            private set

        override fun write(value: Int) {
            out.write(value)
            count++
        }

        override fun write(buffer: ByteArray, offset: Int, length: Int) {
            out.write(buffer, offset, length)
            count += length
        }
    }

    private object AssemblerConfig {
        const val BUFFER_SIZE = 8192
        const val MAX_ICON_BYTES = 16 * 1024 * 1024
        const val MAX_TEMPLATE_ENTRY_BYTES = 64 * 1024 * 1024
        const val ALIGNMENT = 4L
        const val LOCAL_HEADER_SIZE = 30L
        const val ALIGNMENT_EXTRA_ID = 0xd935
        const val SIGNATURE_DIR = "META-INF/"
        val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        val ICON_ENTRY_PATTERN = Regex("^icons/([^/]+)\\.png$")
        val SLOT_NAME_PATTERN = Regex("^slot_[0-9]+$")
        val SLOT_ENTRY_PATTERN = Regex("^res/drawable/(slot_[0-9]+)\\.png$")
        val SIGNATURE_SUFFIXES = setOf(".RSA", ".DSA", ".EC", ".SF")
        val REQUIRED_APK_ENTRIES = setOf(
            "AndroidManifest.xml",
            "resources.arsc",
            "classes.dex",
            "res/xml/appfilter.xml",
            "res/xml/drawable.xml",
            "res/xml/preview_icons.xml"
        )
    }
}

// 直接解析 ZIP 中央目录，验证所有 STORED 条目的实际数据偏移为 4 字节对齐。
private object ApkAlignmentValidator {
    fun validate(apkFile: File) {
        RandomAccessFile(apkFile, "r").use { file ->
            val centralOffset = findCentralDirectoryOffset(file)
            file.seek(centralOffset)
            while (file.filePointer < file.length()) {
                val signature = file.readLittleEndianInt()
                if (signature == END_OF_CENTRAL_DIRECTORY_SIGNATURE) break
                require(signature == CENTRAL_DIRECTORY_SIGNATURE) { "APK 中央目录结构无效" }
                file.skipBytes(6)
                val method = file.readLittleEndianShort()
                file.skipBytes(16)
                val nameLength = file.readLittleEndianShort()
                val extraLength = file.readLittleEndianShort()
                val commentLength = file.readLittleEndianShort()
                file.skipBytes(8)
                val localHeaderOffset = file.readLittleEndianUnsignedInt()
                file.skipBytes(nameLength + extraLength + commentLength)

                if (method == ZipEntry.STORED) {
                    val returnPosition = file.filePointer
                    file.seek(localHeaderOffset + LOCAL_NAME_LENGTH_OFFSET)
                    val localNameLength = file.readLittleEndianShort()
                    val localExtraLength = file.readLittleEndianShort()
                    val dataOffset = localHeaderOffset + LOCAL_HEADER_SIZE +
                            localNameLength + localExtraLength
                    require(dataOffset % ALIGNMENT == 0L) {
                        "APK 未压缩条目未对齐，localHeaderOffset=$localHeaderOffset"
                    }
                    file.seek(returnPosition)
                }
            }
        }
    }

    private fun findCentralDirectoryOffset(file: RandomAccessFile): Long {
        val searchLength = minOf(file.length(), MAX_EOCD_SEARCH)
        val buffer = ByteArray(searchLength.toInt())
        file.seek(file.length() - searchLength)
        file.readFully(buffer)
        for (index in buffer.size - EOCD_MIN_SIZE downTo 0) {
            if (buffer.readLittleEndianInt(index) == END_OF_CENTRAL_DIRECTORY_SIGNATURE) {
                return buffer.readLittleEndianUnsignedInt(index + CENTRAL_OFFSET_IN_EOCD)
            }
        }
        error("APK 缺少中央目录结束记录")
    }

    private fun RandomAccessFile.readLittleEndianShort(): Int =
        readUnsignedByte() or (readUnsignedByte() shl 8)

    private fun RandomAccessFile.readLittleEndianInt(): Int =
        readLittleEndianShort() or (readLittleEndianShort() shl 16)

    private fun RandomAccessFile.readLittleEndianUnsignedInt(): Long =
        readLittleEndianInt().toLong() and 0xffffffffL

    private fun ByteArray.readLittleEndianInt(offset: Int): Int =
        (this[offset].toInt() and 0xff) or
                ((this[offset + 1].toInt() and 0xff) shl 8) or
                ((this[offset + 2].toInt() and 0xff) shl 16) or
                ((this[offset + 3].toInt() and 0xff) shl 24)

    private fun ByteArray.readLittleEndianUnsignedInt(offset: Int): Long =
        readLittleEndianInt(offset).toLong() and 0xffffffffL

    private const val ALIGNMENT = 4L
    private const val LOCAL_HEADER_SIZE = 30L
    private const val LOCAL_NAME_LENGTH_OFFSET = 26L
    private const val EOCD_MIN_SIZE = 22
    private const val CENTRAL_OFFSET_IN_EOCD = 16
    private const val MAX_EOCD_SEARCH = 65557L
    private const val CENTRAL_DIRECTORY_SIGNATURE = 0x02014b50
    private const val END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054b50
}
