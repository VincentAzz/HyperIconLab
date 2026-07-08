package com.capybara.hypericonlab.core.utils

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object ZipUtils {
    fun unzip(inputStream: InputStream, targetDir: File, onProgress: (Float) -> Unit) {
        val zipIn = ZipInputStream(inputStream)
        var entry = zipIn.nextEntry

        var count = 0

        while (entry != null) {
            val file = File(targetDir, entry.name)

            if (entry.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { fos ->
                    val bos = BufferedOutputStream(fos)
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (zipIn.read(buffer).also { read = it } != -1) {
                        bos.write(buffer, 0, read)
                    }
                    bos.flush()
                }
            }
            count++
            if (count % 50 == 0) {
                onProgress((count / 3000f).coerceAtMost(0.99f))
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        zipIn.close()
        onProgress(1.0f)
    }

    fun findFileRecursive(dir: File, fileName: String): File? {
        if (!dir.exists() || !dir.isDirectory) return null

        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val found = findFileRecursive(file, fileName)
                if (found != null) return found
            } else if (file.name == fileName) {
                return file
            }
        }
        return null
    }

    fun findDirRecursive(dir: File, dirName: String): File? {
        if (!dir.exists() || !dir.isDirectory) return null

        if (dir.name == dirName) return dir

        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val found = findDirRecursive(file, dirName)
                if (found != null) return found
            }
        }
        return null
    }
}