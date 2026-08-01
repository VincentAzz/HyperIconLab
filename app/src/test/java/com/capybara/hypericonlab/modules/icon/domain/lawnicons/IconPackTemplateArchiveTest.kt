package com.capybara.hypericonlab.modules.icon.domain.lawnicons

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class IconPackTemplateArchiveTest {
    private val rootDir = Files.createTempDirectory("iconpack-template-test").toFile()
    private val archive = IconPackTemplateArchive()

    @After
    fun tearDown() {
        rootDir.deleteRecursively()
    }

    @Test
    fun extractAndValidate_acceptsCompleteArchive() {
        val zipFile = createArchive()
        val targetDir = File(rootDir, "installed")

        val index = archive.extractAndValidate(zipFile, targetDir, VERSION, COMMIT)

        assertEquals(TEMPLATE_IDS, index.templates.keys)
        assertEquals(5, targetDir.listFiles()?.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun extractAndValidate_rejectsUnexpectedEntry() {
        val zipFile = createArchive(extraEntry = "nested/unexpected.txt")

        try {
            archive.extractAndValidate(zipFile, File(rootDir, "installed"), VERSION, COMMIT)
        } finally {
            assertFalse(File(rootDir, "installed").exists())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun extractAndValidate_rejectsMismatchedApkHash() {
        val zipFile = createArchive(corruptIndexHash = true)

        archive.extractAndValidate(zipFile, File(rootDir, "installed"), VERSION, COMMIT)
    }

    private fun createArchive(
        extraEntry: String? = null,
        corruptIndexHash: Boolean = false
    ): File {
        val payloads = TEMPLATE_IDS.associateWith { "apk-$it".toByteArray() }
        val templates = payloads.mapValues { (iconSetId, payload) ->
            IconPackTemplateInfo(
                filename = "iconpack-template-$iconSetId-$VERSION.apk",
                mapperFile = "icon_mapper_$iconSetId.xml",
                applicationId = "com.example.$iconSetId",
                sizeBytes = payload.size.toLong(),
                sha256 = if (corruptIndexHash && iconSetId == "full") {
                    "0".repeat(64)
                } else {
                    sha256(payload)
                }
            )
        }
        val index = IconPackTemplateIndex(
            schemaVersion = 1,
            resourceVersion = VERSION,
            lawniconsCommit = COMMIT,
            templates = templates
        )
        val zipFile = File(rootDir, "templates.zip")
        ZipOutputStream(zipFile.outputStream()).use { output ->
            writeEntry(
                output,
                "iconpack-templates-$VERSION.json",
                Json.encodeToString(index).toByteArray()
            )
            payloads.forEach { (iconSetId, payload) ->
                writeEntry(output, "iconpack-template-$iconSetId-$VERSION.apk", payload)
            }
            extraEntry?.let { writeEntry(output, it, "unexpected".toByteArray()) }
        }
        return zipFile
    }

    private fun writeEntry(output: ZipOutputStream, name: String, content: ByteArray) {
        output.putNextEntry(ZipEntry(name))
        output.write(content)
        output.closeEntry()
    }

    private fun sha256(content: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(content)
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val VERSION = "20260731"
        const val COMMIT = "ba36a38"
        val TEMPLATE_IDS = setOf("full", "filtered", "preview", "test")
    }
}
