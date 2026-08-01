package com.capybara.hypericonlab.modules.icon.domain.iconpack

import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

// 使用外部样本执行真实模板装配；未配置环境变量时在普通 CI 中跳过。
class IconPackApkAssemblerLocalIntegrationTest {
    @Test
    fun assemble_producesRealUnsignedApk() {
        val templateApk = System.getenv(IntegrationConfig.TEMPLATE_ENV)?.let(::File)
        val bundleZip = System.getenv(IntegrationConfig.BUNDLE_ENV)?.let(::File)
        val outputApk = System.getenv(IntegrationConfig.OUTPUT_ENV)?.let(::File)
        assumeTrue(templateApk?.isFile == true && bundleZip?.isFile == true && outputApk != null)
        val resolvedTemplate = requireNotNull(templateApk)
        val resolvedBundle = requireNotNull(bundleZip)
        val resolvedOutput = requireNotNull(outputApk)

        val slotMappingBytes = ZipFile(resolvedBundle).use { archive ->
            archive.getInputStream(archive.getEntry(IntegrationConfig.SLOT_MAPPING_ENTRY))
                .readBytes()
        }
        val slotMapping = Json.decodeFromString<Map<String, String>>(
            slotMappingBytes.toString(Charsets.UTF_8)
        )
        val templateSlots = ZipFile(resolvedTemplate).use { archive ->
            archive.entries().asSequence()
                .map { it.name }
                .mapNotNull { IntegrationConfig.SLOT_ENTRY_PATTERN.matchEntire(it) }
                .map { it.groupValues[1] }
                .sorted()
                .toList()
        }
        require(templateSlots.size >= 2) { "真实模板至少需要两个槽位" }
        val targetSlot = templateSlots.first()
        val donorSlot = templateSlots[1]
        val targetPackage = slotMapping.entries.first { it.value == targetSlot }.key
        val donorPng = ZipFile(resolvedTemplate).use { archive ->
            archive.getInputStream(
                archive.getEntry("res/drawable/$donorSlot.png")
            ).readBytes()
        }
        val iconsZip = File(resolvedOutput.parentFile, "step6-rendered-icons.zip")
        ZipOutputStream(iconsZip.outputStream()).use { output ->
            output.putNextEntry(ZipEntry("icons/$targetPackage.png"))
            output.write(donorPng)
            output.closeEntry()
        }

        val result = resolvedTemplate.inputStream().use { templateInput ->
            IconPackApkAssembler().assemble(
                templateApk = templateInput,
                renderedIconsZip = iconsZip,
                slotMapping = ByteArrayInputStream(slotMappingBytes),
                outputApk = resolvedOutput
            )
        }

        assertEquals(1, result.replacedIconCount)
        ZipFile(resolvedOutput).use { archive ->
            val replacedPng = archive.getInputStream(
                archive.getEntry("res/drawable/$targetSlot.png")
            ).readBytes()
            assertArrayEquals(donorPng, replacedPng)
        }
        iconsZip.delete()
    }

    private object IntegrationConfig {
        const val TEMPLATE_ENV = "ICONPACK_TEST_TEMPLATE"
        const val BUNDLE_ENV = "ICONPACK_TEST_BUNDLE"
        const val OUTPUT_ENV = "ICONPACK_TEST_OUTPUT"
        const val SLOT_MAPPING_ENTRY = "slot_mapping.json"
        val SLOT_ENTRY_PATTERN = Regex("^res/drawable/(slot_[0-9]+)\\.png$")
    }
}
