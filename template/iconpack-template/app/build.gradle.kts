plugins {
    id("com.android.application")
}

private object TemplateBuildConfig {
    const val NAMESPACE = "com.capybara.hypericonlab.iconpack"
    const val APPLICATION_ID_PREFIX = "com.capybara.hypericonlab.generated.iconpack"
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 35
    const val MIN_SDK = 26
    const val VERSION_CODE = 1
    const val DEFAULT_MAPPER_ID = "test"
    const val DEFAULT_RESOURCE_VERSION = "prototype"
}

val templateMapperId = providers.gradleProperty("templateMapperId")
    .getOrElse(TemplateBuildConfig.DEFAULT_MAPPER_ID)
val templateResourceVersion = providers.gradleProperty("templateResourceVersion")
    .getOrElse(TemplateBuildConfig.DEFAULT_RESOURCE_VERSION)
val templateApplicationId =
    "${TemplateBuildConfig.APPLICATION_ID_PREFIX}.$templateMapperId"

android {
    namespace = TemplateBuildConfig.NAMESPACE
    compileSdk = TemplateBuildConfig.COMPILE_SDK

    defaultConfig {
        applicationId = templateApplicationId
        minSdk = TemplateBuildConfig.MIN_SDK
        targetSdk = TemplateBuildConfig.TARGET_SDK
        versionCode = TemplateBuildConfig.VERSION_CODE
        versionName = templateResourceVersion
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = null
        }
    }

    sourceSets {
        // 资源由外部 Python 脚本在构建前生成，使用静态路径避免 Provider 来源歧义。
        getByName("main").res.directories.add(
            layout.projectDirectory.dir("build/generated/iconpack/$templateMapperId/res")
                .asFile.absolutePath
        )
    }

    androidResources {
        // 槽位必须保持独立资源路径，避免相同占位图被 AAPT2 合并。
        additionalParameters += "--no-resource-deduping"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
