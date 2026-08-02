# HyperIconLab Icon Pack Template Build & Run Script
# This script automates the process of generating resources, building the template,
# assembling the icons, and signing the APK.

$ErrorActionPreference = "Stop"

$ProjectRoot = "D:\_AndroidStudioTemp\HyperIconLab-main"
$JavaHome = "D:\_Dev\_JetBrainsIDE\Android Studio\jbr"
$AndroidHome = "D:\_Dev\_AndroidStudioSDK"
$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $AndroidHome
$env:GRADLE_USER_HOME = "$ProjectRoot\.gradle-template"

$TemplateDir = "$ProjectRoot\template\iconpack-template"
$LawniconsBundle = "$ProjectRoot\external\lawnicons\lawnicons_20260731.zip"
$TestPngDir = "$ProjectRoot\external\icons-test-png"
$WorkDir = "$ProjectRoot\external\iconpack-template-ui-work-20260802"
$RenderedIcons = "$WorkDir\rendered-icons.zip"
$KeyStore = "$WorkDir\ui-test-signing.p12"
$OutputApk = "$ProjectRoot\external\iconpack-template-ui-test.apk"

# 1. Clean Template Project
Write-Host "--- 1. Cleaning Template Project ---" -ForegroundColor Cyan
Set-Location $ProjectRoot
.\gradlew.bat -p $TemplateDir :app:clean --console=plain

# 2. Generate Resources via Python
Write-Host "--- 2. Generating Resources via Python ---" -ForegroundColor Cyan
if (!(Test-Path $WorkDir)) { New-Item -ItemType Directory -Path $WorkDir }

python scripts/lawnicons-pipeline/template_generator.py `
  --bundle $LawniconsBundle `
  --mapper-id test `
  --project-dir $TemplateDir

# 3. Build Template APK (Unsigned)
Write-Host "--- 3. Building Template APK (Unsigned) ---" -ForegroundColor Cyan
# 移除 --no-daemon 以允许使用 gradle.properties 中定义的 2GB 内存
# 移除显式的内存限制，让 Gradle 自动读取模板项目的配置
.\gradlew.bat -p $TemplateDir :app:assembleRelease `
  -PtemplateMapperId=test `
  -PtemplateResourceVersion=20260731 `
  --console=plain
if ($LASTEXITCODE -ne 0) { throw "模板 APK 编译失败，Gradle exit code: $LASTEXITCODE" }

$UnsignedApk = "$TemplateDir\app\build\outputs\apk\release\app-release-unsigned.apk"
if (!(Test-Path $UnsignedApk)) { throw "模板 APK 编译成功但未生成 unsigned APK: $UnsignedApk" }

# 4. Package Test PNGs
Write-Host "--- 4. Packaging Test PNGs ---" -ForegroundColor Cyan
python scripts/lawnicons-pipeline/package_png_icons.py `
  --source-dir $TestPngDir `
  --bundle $LawniconsBundle `
  --mapper-id test `
  --output $RenderedIcons
if ($LASTEXITCODE -ne 0) { throw "PNG 打包失败" }

# 5. Assemble & Sign APK
Write-Host "--- 5. Assembling & Signing APK ---" -ForegroundColor Cyan
if (!(Test-Path $KeyStore)) {
    Write-Host "Creating temporary test certificate..."
    & "$JavaHome\bin\keytool.exe" `
      -genkeypair `
      -alias hypericonlab-ui-test `
      -keyalg RSA `
      -keysize 2048 `
      -validity 3650 `
      -storetype PKCS12 `
      -keystore $KeyStore `
      -storepass HyperIconLabUiTest2026 `
      -keypass HyperIconLabUiTest2026 `
      -dname 'CN=HyperIconLab UI Test, O=Local Test, C=CN'
}

$RunnerArgs = "$UnsignedApk $LawniconsBundle $RenderedIcons $KeyStore HyperIconLabUiTest2026 $OutputApk"

.\gradlew.bat `
  -p external/iconpack-template-ui-test-src `
  --offline `
  --no-daemon `
  --max-workers=1 `
  run `
  "--args=$RunnerArgs" `
  --console=plain
if ($LASTEXITCODE -ne 0) { throw "APK 装配或签名失败，Gradle exit code: $LASTEXITCODE" }

# 6. Verify Output
Write-Host "--- 6. Verifying Output ---" -ForegroundColor Cyan
if (!(Test-Path $OutputApk)) { throw "最终 APK 不存在: $OutputApk" }
Write-Host "`nDone! APK 已生成，请手动安装: $OutputApk" -ForegroundColor Green
