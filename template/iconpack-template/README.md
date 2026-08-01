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

生成资源和 APK 位于 `template/iconpack-template/app/build/`，不会提交到 Git。
