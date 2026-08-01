# HyperIconLab 图标包模板

这是独立于主 App 的最小 Android 工程，用于在 CI 中将 mapper、appfilter 和稳定槽位编译为图标包 APK 模板。

当前阶段只支持 test mapper 原型。

## 生成 test 资源

```powershell
python scripts/lawnicons-pipeline/template_generator.py `
  --bundle external/lawnicons/lawnicons_20260731.zip `
  --mapper-id test `
  --project-dir template/iconpack-template
```

## 编译未签名模板

```powershell
.\gradlew.bat -p template/iconpack-template :app:assembleRelease
```

## 校验模板并生成索引

```powershell
$env:ANDROID_HOME = "<Android SDK 路径>"
python scripts/lawnicons-pipeline/template_validator.py `
  --bundle external/lawnicons/lawnicons_20260731.zip `
  --apk template/iconpack-template/app/build/outputs/apk/release/app-release-unsigned.apk `
  --mapper-id test `
  --expected-application-id com.capybara.hypericonlab.generated.iconpack.test `
  --output-index template/iconpack-template/app/build/validated/iconpack-templates-20260731.json
```

校验器直接读取 APK ZIP、`resources.arsc` 和二进制 XML，并调用 `apksigner verify` 确认模板未签名。

生成资源和 APK 位于 `template/iconpack-template/app/build/`，不会提交到 Git。
