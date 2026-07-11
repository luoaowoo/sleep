# SleepSnore 睡眠鼾声记录

本项目是一个本地优先的 Android 睡眠鼾声记录应用，用于夜间检测鼾声片段、保存可回放音频、生成睡眠报告、周总结和 AI 分析。

## 主要功能

- 睡眠录音：前台麦克风服务持续监听，只保存疑似鼾声片段，避免整晚录音占用过大。
- 片段回放：保存 WAV 音频片段，回放页展示时间、峰值 dB、主频 Hz、AI 类型标签和时长。
- 本地分析：基于能量、过零率、主频估算和规则评分生成 SnoreScore。
- 睡眠报告：展示 AHI 疑似估算、长静音统计、时间线、类型分布和高风险片段明细。
- 一周总结：首页聚合最近 7 晚记录，生成本地周总结；可选接入自己的 DeepSeek API 生成更自然的 AI 分析。
- 小米手环睡眠辅助：通过 Mi Fitness/小米运动健康或 Zepp Life 同步到 Health Connect；睡前开启前台鼾声检测，睡醒后用同步到的睡眠结束记录自动停录/校准。
- 个性化设置：支持 RGB 主题色环、Material You 动态色、字号、圆角、紧凑布局、录音阈值和片段时长。
- 数据导出：支持 CSV 导出与系统分享。

## 小米手环 / Health Connect

当前版本不依赖民间小米手环私有 BLE 协议，也不承诺存在可公开使用的实时小米睡眠 API。可落地链路是：

1. 在 Mi Fitness/小米运动健康或 Zepp Life 中开启 Health Connect 睡眠同步。
2. 在本应用设置页授权 Health Connect 睡眠读取和后台读取。
3. 睡前点击「睡前开启前台检测」，由可见前台麦克风服务整晚检测鼾声。
4. 睡醒后等待小米伴侣 App 将睡眠结束记录同步到 Health Connect，本应用自动停止/校准；如果长期未同步，手环来源前台检测会在 16 小时后安全截断结算。

这是为了遵守 Android 对麦克风 while-in-use 权限和前台服务的限制：后台 Worker/开机广播不可靠、也不应在用户睡着后再强行启动麦克风。

## DeepSeek 接入

在 `设置 -> AI 分析` 中填写：

- `DeepSeek API Key`
- `Base URL`，默认 `https://api.deepseek.com/v1/chat/completions`
- `模型名`，默认 `deepseek-chat`
- 自定义分析信息，例如年龄、身高体重、是否饮酒、睡姿习惯等

未配置 API 时，应用仍会生成本地规则周总结；配置后点击首页「DeepSeek 分析」会联网生成周总结。

## 隐私与医学边界

- 音频与分析数据默认保存在本机应用私有目录。
- DeepSeek 分析仅在用户主动配置 API 并点击生成时联网发送周摘要数据，不会上传原始音频。
- AHI 与呼吸暂停相关信息由鼾声片段和静音间隔估算，只用于筛查和趋势观察。
- 本应用不能替代医生诊断或多导睡眠监测（PSG）。若出现明显憋醒、白天嗜睡、疑似呼吸暂停等情况，请咨询睡眠医学专业人员。

## 本地构建

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:lintDebug :app:lintRelease :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:compileDebugAndroidTestKotlin --no-daemon --console=plain
```

常用产物：

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
- Release APK：`app/build/outputs/apk/release/`

### 构建排障

- 若 Android Studio 复用旧 Gradle daemon 后出现 `Java heap space` / GC thrashing，先执行 `.\gradlew.bat --stop` 再重建；项目已在 `gradle.properties` 中把 Gradle 堆配置为 4GiB。
- 若 KAPT/配置缓存偶发输出目录错误，优先用 `--no-configuration-cache` 重跑；频繁复现时再考虑清理构建缓存。
- `*.hprof` 堆转储已被 `.gitignore` 忽略，如 OOM 后生成大文件可安全删除。

## 测试与质量门禁

- JVM 单测：音频分析、鼾声评分、导出、主题、ViewModel。
- AndroidTest 编译：Room、设置页 Compose 测试。
- GitHub Actions：推送到 `master` 后执行 lint、单测、debug/release 构建，并上传 APK artifact。
