package com.capybara.hypericonlab.modules.icon.domain.usecase

import android.content.Context
import com.capybara.hypericonlab.core.mapper.IconMapperProcessor
import com.capybara.hypericonlab.core.utils.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ManageResourcesUseCase(private val context: Context) {

    private val filesDir = context.filesDir
    private val lawniconsBase = File(filesDir, "lawnicons")
    private val mapperBase = File(filesDir, "icon_mapper")

    suspend fun checkMapperExists(): Boolean = withContext(Dispatchers.IO) {
        val file = ZipUtils.findFileRecursive(mapperBase, "icon_mapper.xml")
        file != null && file.exists()
    }

    suspend fun performUnzip(onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        ZipUtils.unzip(context.assets.open("lawnicons.zip"), lawniconsBase) { p ->
            onProgress(p * 0.5f)
        }
        ZipUtils.unzip(context.assets.open("icon_mapper.zip"), mapperBase) { p ->
            onProgress(0.5f + (p * 0.5f))
        }
    }

    suspend fun generateMapper(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val appfilter = ZipUtils.findFileRecursive(lawniconsBase, "appfilter.xml")
            val altMapper = ZipUtils.findFileRecursive(mapperBase, "icon_mapper_alt.xml")
            val target = File(mapperBase, "icon_mapper/icon_mapper.xml")

            if (appfilter != null) {
                target.parentFile?.mkdirs()
                IconMapperProcessor.convertIconMapper(appfilter, target, altMapper)
                Result.success(Unit)
            } else {
                Result.failure(Exception("未找到 appfilter.xml"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
