package com.capybara.hypericonlab.modules.build.domain.packaging

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class IconPackApkAssemblerTest {
    private val rootDir = Files.createTempDirectory("iconpack-assembler-test").toFile()
    private val assembler = IconPackApkAssembler()

    @After
    fun tearDown() {
        rootDir.deleteRecursively()
    }

    @Test
    fun assemble_replacesSlotRemovesSignatureAndKeepsStoredEntry() {
        val template = createTemplate("slot_0001")
        val iconsZip = createIconsZip()
        val output = File(rootDir, "assembled.apk")

        val result = template.inputStream().use { templateInput ->
            assembler.assemble(
                templateApk = templateInput,
                renderedIconsZip = iconsZip,
                slotMapping = ByteArrayInputStream(SLOT_MAPPING.toByteArray()),
                outputApk = output
            )
        }

        assertEquals(1, result.replacedIconCount)
        assertEquals(1, result.templateSlotCount)
        ZipFile(output).use { archive ->
            val slotEntry = archive.getEntry("res/drawable/slot_0001.png")
            assertEquals(ZipEntry.STORED, slotEntry.method)
            assertArrayEquals(PNG_CONTENT, archive.getInputStream(slotEntry).readBytes())
            assertNull(archive.getEntry("META-INF/OLD.SF"))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun assemble_rejectsTemplateWithoutMappedSlotAndDeletesTemporaryOutput() {
        val template = createTemplate("slot_9999")
        val output = File(rootDir, "assembled.apk")

        try {
            template.inputStream().use { templateInput ->
                assembler.assemble(
                    templateApk = templateInput,
                    renderedIconsZip = createIconsZip(),
                    slotMapping = ByteArrayInputStream(SLOT_MAPPING.toByteArray()),
                    outputApk = output
                )
            }
        } finally {
            assertFalse(output.exists())
            assertFalse(File(rootDir, "assembled.apk.assembling").exists())
        }
    }

    private fun createTemplate(slotName: String): File {
        val template = File(rootDir, "template-$slotName.apk")
        ZipOutputStream(template.outputStream()).use { output ->
            REQUIRED_ENTRIES.forEach { name -> writeEntry(output, name, name.toByteArray()) }
            writeEntry(
                output,
                "res/drawable/$slotName.png",
                PLACEHOLDER_CONTENT,
                stored = true
            )
            writeEntry(output, "META-INF/OLD.SF", "old-signature".toByteArray())
        }
        return template
    }

    private fun createIconsZip(): File {
        val iconsZip = File(rootDir, "icons.zip")
        ZipOutputStream(iconsZip.outputStream()).use { output ->
            writeEntry(output, "icons/com.example.app.png", PNG_CONTENT)
        }
        return iconsZip
    }

    private fun writeEntry(
        output: ZipOutputStream,
        name: String,
        content: ByteArray,
        stored: Boolean = false
    ) {
        val entry = ZipEntry(name)
        if (stored) {
            entry.method = ZipEntry.STORED
            entry.size = content.size.toLong()
            entry.compressedSize = content.size.toLong()
            entry.crc = CRC32().apply { update(content) }.value
        }
        output.putNextEntry(entry)
        output.write(content)
        output.closeEntry()
    }

    private companion object {
        const val SLOT_MAPPING = "{\"com.example.app\":\"slot_0001\"}"
        val PNG_CONTENT = byteArrayOf(
            0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3
        )
        val PLACEHOLDER_CONTENT = PNG_CONTENT.copyOfRange(0, 8)
        val REQUIRED_ENTRIES = setOf(
            "AndroidManifest.xml",
            "resources.arsc",
            "classes.dex",
            "res/xml/appfilter.xml",
            "res/xml/drawable.xml",
            "res/xml/preview_icons.xml"
        )
    }
}
