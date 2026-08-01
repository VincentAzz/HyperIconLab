plugins {
    id("com.android.application")
}

private object TemplateBuildConfig {
    const val NAMESPACE = "com.capybara.hypericonlab.iconpack"
    const val APPLICATION_ID = "com.capybara.hypericonlab.generated.iconpack.test"
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 35
    const val MIN_SDK = 26
    const val VERSION_CODE = 1
    const val VERSION_NAME = "test-prototype"
    const val GENERATED_RES_PATH = "generated/iconpack/test/res"
}

android {
    namespace = TemplateBuildConfig.NAMESPACE
    compileSdk = TemplateBuildConfig.COMPILE_SDK

    defaultConfig {
        applicationId = TemplateBuildConfig.APPLICATION_ID
        minSdk = TemplateBuildConfig.MIN_SDK
        targetSdk = TemplateBuildConfig.TARGET_SDK
        versionCode = TemplateBuildConfig.VERSION_CODE
        versionName = TemplateBuildConfig.VERSION_NAME
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
            layout.projectDirectory.dir("build/${TemplateBuildConfig.GENERATED_RES_PATH}")
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
