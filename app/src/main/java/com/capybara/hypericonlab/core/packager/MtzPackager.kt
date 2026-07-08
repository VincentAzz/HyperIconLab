package com.capybara.hypericonlab.core.packager

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// 早期mtz打包器
object MtzPackager {
    fun pack(bitmaps: Map<String, Bitmap>, outputFile: File) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
            bitmaps.forEach { (packageName, bitmap) ->
                val entry = ZipEntry("icons/$packageName.png")
                zipOut.putNextEntry(entry)

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, zipOut)

                zipOut.closeEntry()
            }
        }
    }
}