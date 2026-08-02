plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
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
val templateIconSetName = when (templateMapperId) {
    "full" -> "完整图标集"
    "filtered" -> "常用图标集"
    "preview" -> "预览图标集"
    "test" -> "测试图标集"
    else -> templateMapperId
}

android {
    namespace = TemplateBuildConfig.NAMESPACE
    compileSdk = TemplateBuildConfig.COMPILE_SDK

    defaultConfig {
        applicationId = templateApplicationId
        minSdk = TemplateBuildConfig.MIN_SDK
        targetSdk = TemplateBuildConfig.TARGET_SDK
        versionCode = TemplateBuildConfig.VERSION_CODE
        versionName = templateResourceVersion
        resValue("string", "icon_set_name", templateIconSetName)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = null
        }
    }

    sourceSets {
        getByName("main").res.directories.add(
            layout.projectDirectory.dir("build/generated/iconpack/$templateMapperId/res")
                .asFile.absolutePath
        )
    }

    androidResources {
        // 槽位必须保持独立资源路径，避免相同占位图被 AAPT2 合并。
        additionalParameters += "--no-resource-deduping"
    }

    packaging {
        jniLibs {
            // 装配器保持压缩方式；压缩原生库可避免替换槽位后破坏 16 KiB 页面对齐。
            useLegacyPackaging = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.materialKolor)
}
