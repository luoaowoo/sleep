# Android 睡眠鼾声录制与分析 — 技术方案研究报告

> 基于 Android SDK 36 (android-36.1) 源码分析，结合行业最佳实践整理

---

## 1. 后台音频录制 API 选型：AudioRecord vs MediaRecorder

### AudioRecord（推荐 ✅）

低层级 PCM 音频采集 API，适合**需要实时分析**的场景。

| 特性 | AudioRecord | MediaRecorder |
|------|-------------|---------------|
| 原始 PCM 访问 | ✅ read(byte[], int, int) | ❌ 不暴露 |
| 实时分析 | ✅ 边录边处理 | ❌ 只能后处理 |
| 内置编码 | ❌ 需自行实现 | ✅ AAC/OPUS/AMR |
| 直出文件 | ❌ 需手写 | ✅ setOutputFile() |
| AudioSource | ✅ UNPROCESSED / VOICE_RECOGNITION | ✅ 同样支持 |
| 鼾声检测适用 | ⭐⭐⭐ 最佳选择 | ⭐ 仅适合存档 |

**推荐方案：** 使用 AudioRecord + UNPROCESSED 源，16kHz/16bit/Mono 采集原始 PCM，在读取线程中实时做能量检测和音频分类，同时异步编码保存。

---

## 2. 锁屏 / 熄屏下后台录音方案

Android 锁屏后录音不会被自动中断，关键在于保证 CPU 不休眠和进程不被杀死。

核心依赖：**Foreground Service** + **PARTIAL_WAKE_LOCK**

- PARTIAL_WAKE_LOCK（值 0x00000001）：仅保持 CPU 运行，不点亮屏幕
- 需声明 WAKE_LOCK 权限
- acquire(30*60*1000) 30分钟超时自动续期

---

## 3. 音频格式推荐与文件大小管理

| 格式 | 参数 | 每小时大小 | 8小时大小 |
|------|------|-----------|-----------|
| PCM/WAV | 16kHz, 16bit, Mono | ~115 MB | ~920 MB |
| PCM/WAV | 8kHz, 16bit, Mono | ~58 MB | ~460 MB |
| AAC-ELD | 32kbps VBR | ~14 MB | ~112 MB |
| OPUS | 16kbps | ~7 MB | ~56 MB |
| AMR-NB | 12kbps | ~5 MB | ~42 MB |

**推荐策略：** 实时分析路径用 PCM Buffer → FFT/能量分析；存档路径用 PCM Buffer → OpusEncoder → .ogg 文件（节省 90%+ 空间）。按 10-30 分钟分段存储。

---

## 4. 开源音频分析 / 鼾声检测库

| 库名 | 类型 | 功能 | 授权 |
|------|------|------|------|
| TarsosDSP | Java DSP 库 | FFT、音高检测、能量计算 | GPL |
| musicg | Java 音频分析 | 哨声/拍手检测、频谱分析 | Apache 2 |
| TensorFlow Lite Audio | ML 推理 | YAMNet 声音分类（521类） | Apache 2 |

---

## 5. 鼾声 vs 静音 vs 环境音检测算法

### 推荐渐进式方案：

```
第1层: RMS Energy → 过滤静音（< 阈值直接丢弃，省去后续计算）
第2层: ZCR + Spectral Centroid → 快速区分鼾声/语音/环境音
第3层: MFCC + 轻量分类器 → 精确鼾声/非鼾声二分类
```

---

## 6. WakeLock 与电池优化

- 用 PARTIAL_WAKE_LOCK，不要用 FULL_WAKE_LOCK
- 请求忽略电池优化（ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS）
- 引导用户加入厂商白名单（小米自启动管理、华为启动管理等）
- 配合充电状态检测：充电或电量 >30% 才启动
- 8小时预估耗电 15-25%

---

## 7. Android API 26+ 前台 Service 要求

### AndroidManifest.xml 配置

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service
    android:name=".service.SleepRecordingService"
    android:foregroundServiceType="microphone"
    android:exported="false" />
```

### 关键 API 版本要求

| Android 版本 | 要求 |
|-------------|------|
| API 26 (8.0) | startForegroundService() + 5 秒内 startForeground() |
| API 29 (10) | 必须声明 foregroundServiceType |
| API 34 (14) | 必须声明 FOREGROUND_SERVICE_MICROPHONE 权限 |

---

## 总结：推荐技术栈

| 层级 | 技术选型 | 依据 |
|------|---------|------|
| 录音 | AudioRecord + UNPROCESSED 源 | 实时 PCM 访问，无 DSP 干扰 |
| 采样 | 16kHz, 16bit, Mono | 鼾声频率范围最佳平衡 |
| 前台服务 | Service + microphone type | API 29+ 强制要求 |
| CPU保持 | PARTIAL_WAKE_LOCK | 熄屏保持 CPU |
| 音频分析 | TarsosDSP (FFT + 能量) | 成熟 Java DSP 库 |
| 鼾声检测 | RMS → ZCR → MFCC 级联 | 渐进式，节能 + 精准 |
| 文件存储 | PCM 分段 + OPUS 编码 | 分析与存档分离 |
| 电池优化 | 充电检测 + 厂商白名单 + 低电量保护 | 防止过度耗电 |

### 主要风险点

1. **国产手机进程杀死**：需引导用户添加白名单
2. **OPUS 编码实时性**：MediaCodec 编码器低端机延迟较大
3. **8 小时文件完整性**：分段存储 + 异常恢复机制必不可少
