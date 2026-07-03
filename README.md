# SleepSnore 睡眠鼾声记录

本项目是一个本地优先的 Android 睡眠鼾声记录应用，用于夜间检测鼾声片段、保存可回放音频、生成睡眠报告和规则型 AI 评价。

## 主要功能

- 睡眠录音：前台麦克风服务持续监听，只保存疑似鼾声片段，避免整夜录音占用过大。
- 片段回放：保存 WAV 音频片段，回放页展示时间、峰值 dB、主频 Hz、AI 类型标签和时长。
- 本地分析：基于能量、过零率、Goertzel 主频估算和规则评分生成 SnoreScore。
- 睡眠报告：展示 AHI 疑似估算、长静音统计、时间线、类型分布和高风险片段明细。
- 设置与个性化：支持静音阈值、30 天自动清理、Material You 动态色、浅色/深色/跟随系统。
- 数据导出：支持 CSV 导出与系统分享。

## 隐私与医学边界

- 音频与分析数据默认保存在本机应用私有目录，不上传云端。
- AHI 与呼吸暂停相关信息由鼾声片段间长静音估算，只用于筛查和趋势观察。
- 本应用不能替代医生诊断或多导睡眠监测（PSG）。若出现明显憋醒、白天嗜睡、疑似呼吸暂停等情况，请咨询睡眠医学专业人员。

## 本地构建

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:lintDebug :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:compileDebugAndroidTestKotlin --no-daemon --console=plain
```

常用产物：

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
- Release APK：`app/build/outputs/apk/release/`

## 测试与质量门禁

- JVM 单测：音频分贝边界、主频分析、鼾声评分风险递增。
- AndroidTest 编译：Room 1→2 迁移烟测。
- Lint：Debug 静态检查。
- GitHub Actions：推送到 `master` 后自动执行 lint、单测、debug/release 构建，并上传 APK artifact。

## 当前实现说明

- AI 评价为本地规则引擎，不依赖网络。
- `SnoreClassifier` 预留 TFLite 接入口；加入真实模型资产后可替换当前规则型分类。
- 目前音频片段优先保存为 WAV，换取系统播放器稳定回放；后续可接入合法 Ogg/Opus muxer 压缩体积。
