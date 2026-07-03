# 睡眠鼾声记录 App — 技术骨架设计文档

> 版本：v1.0 | 日期：2026-07-03
> 设计依据：Material Design 3 (Material You) + Android 官方架构指南

---

## 一、整体技术架构（分层设计）

```
┌─────────────────────────────────────────────────────────┐
│                     UI Layer (Jetpack Compose + M3)      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐ │
│  │  Dashboard│ │Recording │ │ History  │ │  Settings   │ │
│  │  Screen  │ │  Screen  │ │  Screen  │ │   Screen    │ │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └─────┬──────┘ │
│       │             │            │              │        │
│  ┌────┴─────────────┴────────────┴──────────────┴─────┐ │
│  │              ViewModels (per-screen)               │ │
│  └───────────────────────┬───────────────────────────┘ │
├──────────────────────────┼─────────────────────────────┤
│                   Domain Layer                          │
│  ┌───────────────────────┼───────────────────────────┐ │
│  │  SnoreDetector │ SleepAnalyzer │ FactorTracker   │ │
│  │  AudioProcessor│ AHIEstimator  │ ScoreCalculator │ │
│  └───────────────────────┼───────────────────────────┘ │
├──────────────────────────┼─────────────────────────────┤
│                    Data Layer                           │
│  ┌───────────────────────┼───────────────────────────┐ │
│  │  Room DB        │  FileRepository  │  Preferences  │ │
│  │  (SleepRecord,  │  (AudioSegment,  │  (DataStore)  │ │
│  │   SnoreEvent,   │   OPUS encode,   │               │ │
│  │   FactorLog)    │   File cleanup)  │               │ │
│  └───────────────────────┼───────────────────────────┘ │
├──────────────────────────┼─────────────────────────────┤
│                 Platform Layer                          │
│  ┌───────────────────────┼───────────────────────────┐ │
│  │  AudioRecord   │ ForegroundService │ WakeLock     │ │
│  │  (UNPROCESSED) │ (microphone type) │ (PARTIAL)    │ │
│  │  TFLite Runtime│ BatteryManager    │ AlarmManager │ │
│  └───────────────────────┴───────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### 1.1 各层职责

| 层 | 职责 | 关键技术 |
|---|------|---------|
| **UI Layer** | M3 界面渲染、导航、用户交互 | Jetpack Compose + Navigation + M3 |
| **Domain Layer** | 鼾声检测算法、睡眠分析、评分计算 | 纯 Kotlin，不依赖 Android Framework |
| **Data Layer** | 数据库操作、文件管理、偏好设置 | Room + DataStore + File I/O |
| **Platform Layer** | 音频采集、后台服务、系统权限 | AudioRecord + ForegroundService + TFLite |

---

## 二、数据流设计

### 2.1 录制流程

```
用户点击"开始睡眠"
    │
    ▼
SleepRecordingService (Foreground)
    │
    ├── acquireWakeLock(PARTIAL)
    ├── 显示通知 "正在录制…"
    │
    └── AudioRecord 启动
            │
            ▼
        PCM Buffer (16kHz, 16bit, Mono)
            │
            ├──────────────────────────┐
            ▼                          ▼
     实时分析线程                  归档线程
            │                          │
    ┌───────┴───────┐          ┌──────┴──────┐
    │ RMS 能量计算   │          │ 每30分钟    │
    │ (过滤静音)     │          │ 攒一批PCM   │
    └───────┬───────┘          │ → OPUS编码  │
            │                  │ → 存 .ogg    │
     RMS > 阈值?               └─────────────┘
            │
      YES ──┼── NO → 丢弃
            │
            ▼
    ZCR + Spectral Centroid
    区分鼾声/语音/环境音
            │
     是鼾声?── NO → 丢弃
            │
      YES
            │
            ▼
    MFCC 特征提取
    → TFLite CNN 分类器
    → 判定: 软腭型/舌根型/混合型
            │
            ▼
    写入 Room: SnoreEvent
    (timestamp, dB, duration, type)
```

### 2.2 每日汇总流程

```
用户醒来 / 手动停止
    │
    ▼
计算 Snore Score：
    ├── 鼾声总时长 / 睡眠总时长 → SnoreRatio
    ├── 平均 dB → AvgLoudness
    ├── 鼾声事件密度 → EventDensity
    ├── 呼吸暂停估算(AHI) → EstAHI
    └── 加权公式 → SnoreScore (0-100)
            │
            ▼
    Room: SleepRecord (汇总)
    (date, startTime, endTime, snoreScore,
     estAHI, avgDb, maxDb, snoreRatio,
     snoreTypeDistribution, sleepStages)
```

---

## 三、数据库设计 (Room)

```kotlin
// === 核心表 ===

@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,           // epoch ms
    val endTime: Long,
    val durationMinutes: Int,
    val snoreScore: Int,           // 0-100
    val estAHI: Float,             // 估算AHI
    val avgDb: Float,              // 平均鼾声分贝
    val maxDb: Float,
    val snoreRatio: Float,         // 打鼾时间占比 0.0-1.0
    val snoreEventCount: Int,
    val apneaEventCount: Int,      // ≥10秒无声事件
    val snoreTypeDistribution: String, // JSON: {"softPalate":0.6, "tongue":0.3,...}
    val createdAt: Long
)

@Entity(
    tableName = "snore_events",
    foreignKeys = [ForeignKey(
        entity = SleepRecord::class,
        parentColumns = ["id"],
        childColumns = ["recordId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("recordId")]
)
data class SnoreEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val timestamp: Long,           // epoch ms
    val durationMs: Int,           // 持续毫秒
    val peakDb: Float,
    val dominantFreq: Float,       // 主导频率 Hz
    val snoreType: String,         // soft_palate | tongue | epiglottis | mixed
    val audioSegmentPath: String?  // 可选的音频片段路径
)

@Entity(tableName = "factor_logs")
data class FactorLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,              // yyyy-MM-dd
    val alcoholDrinks: Int,        // 饮酒杯数
    val caffeineAfter: Int?,       // 下午几点的咖啡
    val sleepPosition: String,     // supine | left_side | right_side | prone
    val weightKg: Float?,
    val exerciseMinutes: Int?,
    val nasalCongestion: Boolean,  // 鼻塞
    val notes: String?
)
```

---

## 四、UI 信息架构 (M3 标准)

### 4.1 导航结构

```
Bottom Navigation Bar
├── 🏠 首页 (Dashboard)
│   ├── 昨晚睡眠概览卡片
│   ├── 鼾声趋势图 (7天/30天)
│   └── FAB: 开始睡眠
│
├── 📊 分析 (Analysis)
│   ├── 鼾声类型分布饼图
│   ├── 分贝时间线
│   ├── AHI 估算趋势
│   └── 影响因素关联分析
│
└── ⚙️ 设置 (Settings)
    ├── 录音灵敏度
    ├── 目标睡眠时长
    ├── 通知偏好
    ├── 数据导出
    ├── OSA 风险评估问卷
    └── 关于 / 隐私政策
```

### 4.2 页面树

```
App
├── HomeScreen (Dashboard)
│   ├── TopAppBar: "睡眠概览" + 日期选择器
│   ├── SleepSummaryCard         — 昨晚 SnoreScore + AHI + 时长
│   ├── WeeklyTrendChart         — 7天 SnoreScore 趋势
│   ├── QuickInsightChips        — "比昨天好12%" "侧卧减少打鼾"
│   └── FloatingActionButton     — "开始睡眠" → RecordingScreen
│
├── RecordingScreen (全屏沉浸)
│   ├── DarkBackground           — 暗色背景保护睡眠
│   ├── ClockDisplay             — 当前时间 (Display Large)
│   ├── RecordingIndicator       — 脉动波形动画
│   ├── ElapsedTime              — 已录制时长
│   └── StopButton               — 药丸形 "停止并查看结果"
│
├── ResultScreen (醒来后展示)
│   ├── SnoreScoreHero           — 大圆环分数动画
│   ├── KeyMetricsRow            — AHI / avgDB / snoreTime / events
│   ├── SnoreTimeline            — 整夜鼾声时间线柱状图
│   ├── SnoreTypePieChart        — 鼾声类型分布
│   └── FactorInputSheet         — 底部弹窗: 填写昨晚影响因素
│
├── AnalysisScreen (深度分析)
│   ├── DateRangeSelector        — 周/月/自定义
│   ├── TrendLineChart           — SnoreScore 走势
│   ├── FactorCorrelationCard    — 饮酒vs打鼾 相关性热力图
│   └── ListeningHistoryList     — 历史录音回放入口
│
├── SettingScreen
│   ├── PreferencesSection       — 录音参数、目标、单位
│   ├── DataSection              — 导出CSV/PDF、清理数据
│   ├── RiskAssessmentCard       — STOP-BANG 问卷入口
│   └── AboutSection             — 版本、隐私、开源协议
│
├── RiskAssessmentScreen
│   └── STOP-BANG 问卷 (8题) → 风险等级结果
│
└── AudioPlaybackScreen
    └── 播放选中的鼾声片段 + 波形图
```

### 4.3 关键页面 Wireframe (M3 规范)

#### HomeScreen 布局

```
┌──────────────────────────────┐
│  TopAppBar                   │  56dp
│  睡眠概览        📅 7月3日   │
├──────────────────────────────┤
│  ┌──────────────────────────┐│
│  │  昨晚睡眠  7h 23min      ││  padding 24dp
│  │                          ││
│  │    ╭──────╮              ││
│  │   ╱  72  ╲   SnoreScore ││  Hero 大圆环
│  │  │  良好  │              ││
│  │   ╲      ╱               ││
│  │    ╰──────╯              ││
│  │                          ││
│  │  AHI 2.3  avg 42dB  18次 ││  指标行
│  └──────────────────────────┘│  圆角 28dp
│                              │
│  ┌──────────────────────────┐│
│  │  📈 本周趋势             ││
│  │  [══╗  ╔═══╗]           ││  折线图卡片
│  │  [  ╚══╝   ╚══]          ││  圆角 28dp
│  └──────────────────────────┘│
│                              │
│  💡 侧卧时打鼾减少 23%      │  SuggestionChip
│                              │
│              ┌──────┐        │
│              │ 🌙   │        │  FAB (56dp)
│              │ 开始  │        │  药丸形
│              └──────┘        │
└──────────────────────────────┘
│  🏠首页  │  📊分析  │  ⚙️设置 │  NavBar 80dp
└──────────────────────────────┘
```

#### RecordingScreen 布局（暗色沉浸）

```
┌──────────────────────────────┐
│                              │
│                              │
│         01:23:45             │  Display Large
│                              │  居中、低亮度
│                              │
│     ╭─────────────────╮     │
│    ╱ ■■■■  ■■  ■■■■■■ ╲    │  脉动波形动画
│   │  ■■  ■■■■  ■■  ■■ │    │  (实时音量可视化)
│    ╲ ■■■  ■■  ■■■■  ■╱     │
│     ╰─────────────────╯     │
│                              │
│     已录制 5h 23m            │  Body Large
│                              │
│     [ 停止录制 ]             │  FilledButton 圆角 28dp
│                              │
└──────────────────────────────┘
```

---

## 五、M3 设计 Token 定义

### 5.1 色彩系统 (Tonal Palette)

```kotlin
// 主题色：靛蓝-紫色系 (传达安静、睡眠、专业感)
// Seed Color: #6750A4 (M3 默认 Primary)

// Light Theme
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    surface = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFFE7E0EC),
    background = Color(0xFFFFFBFE),
    error = Color(0xFFB3261E),
    // ... 完整 token
)

// Dark Theme (RecordingScreen 关键)
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    surface = Color(0xFF1C1B1F),       // 暗色背景
    surfaceVariant = Color(0xFF49454F),
    background = Color(0xFF1C1B1F),
    // Recording 场景额外降低亮度
    // surfaceDim = Color(0xFF141316) for 极度暗场景
)
```

### 5.2 间距系统

```kotlin
// 基于 8dp grid
object Spacing {
    val xs = 4.dp    // 微小元素
    val sm = 8.dp    // 基础单位
    val md = 16.dp   // 标准间距
    val lg = 24.dp   // 区块间距
    val xl = 32.dp   // 大区块
    val xxl = 48.dp  // 页面级留白
}
```

### 5.3 圆角系统

```kotlin
object Shapes {
    val extraSmall = RoundedCornerShape(4.dp)
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)    // 常用卡片
    val large = RoundedCornerShape(16.dp)     // 输入框
    val extraLarge = RoundedCornerShape(28.dp) // Hero 卡片、FAB
    val full = RoundedCornerShape(50)          // 药丸形按钮
}
```

### 5.4 排版层级

```kotlin
// 对应 M3 五阶：Display → Headline → Title → Body → Label
object Typography {
    // Display Large: SnoreScore 大数字 (57sp, Regular)
    // Headline Medium: 页面主标题 (28sp, Regular)
    // Title Large: 卡片标题 (22sp, Regular)
    // Title Medium: 列表项标题 (16sp, Medium)
    // Body Large: 正文 (16sp, Regular)
    // Body Medium: 辅助信息 (14sp, Regular)
    // Label Large: 按钮文字 (14sp, Medium)
    // Label Small: 最小标注 (11sp, Medium)
}
```

---

## 六、组件规格

### 6.1 核心组件一览

| 组件 | M3 组件 | 规格 |
|------|---------|------|
| **SnoreScore 环** | 自定义 Canvas + CircularProgressIndicator | 外径 160dp，描边 12dp，动画 |
| **趋势图** | 自定义 Canvas (Compose) | 高度 180dp，圆角 28dp 卡片 |
| **录音按钮** | ExtendedFloatingActionButton | 56dp 高度，28dp 圆角，药丸形 |
| **停止按钮** | FilledButton | 48dp 高，28dp 圆角，满宽 |
| **指标卡片** | Card (Elevated) | 16dp padding, 28dp 圆角 |
| **导航栏** | NavigationBar | 80dp 高, 3 items |
| **时间线图** | 自定义 Canvas | 柱状图，按小时分桶 |
| **影响因子输入** | ModalBottomSheet | 含 Slider、Switch、Chip |
| **日期选择** | DatePicker (M3) | 标准 M3 DatePickerDialog |
| **Audio 回放** | Slider + IconButton | 自定义波形图 + 播放控制 |

### 6.2 交互反馈规范

- **Ripple**：所有可点击元素使用 `rememberRipple()`，颜色取 `primary.copy(alpha=0.12)`
- **SnoreScore 动画**：进入页面时大圆环从 0 滚动到目标值 (spring animation, 800ms)
- **录音状态**：RecordingScreen 波形实时脉动，颜色从 primary 渐变到 tertiary
- **页面切换**：Navigation Compose `fadeIn` + `slideInHorizontally` (M3 标准 shared axis transition)

---

## 七、模块划分 (Gradle)

```
app/
├── ui/
│   ├── theme/          — M3 Theme, Color, Typography, Shape
│   ├── navigation/     — NavGraph, Routes
│   ├── home/           — HomeScreen + HomeViewModel
│   ├── recording/      — RecordingScreen + RecordingViewModel
│   ├── result/         — ResultScreen + ResultViewModel
│   ├── analysis/       — AnalysisScreen + AnalysisViewModel
│   ├── settings/       — SettingsScreen + SettingsViewModel
│   ├── risk/           — 风险评估问卷
│   ├── playback/       — 音频回放
│   └── components/     — 共享组件 (SnoreScoreRing, TrendChart, etc.)
│
├── domain/
│   ├── detector/       — SnoreDetector 接口 + 实现
│   ├── analyzer/       — SleepAnalyzer, AHIEstimator
│   └── model/          — 领域模型
│
├── data/
│   ├── db/             — Room DAO, Entities, Converters
│   ├── repository/     — SleepRepository, AudioRepository
│   └── preferences/    — UserPreferences (DataStore)
│
├── service/
│   └── SleepRecordingService.kt  — Foreground Service
│
├── audio/
│   ├── AudioRecorder.kt          — AudioRecord 封装
│   ├── AudioEncoder.kt           — OPUS 编码
│   └── AudioAnalyzer.kt          — RMS/ZCR/MFCC 提取
│
└── ml/
    └── SnoreClassifier.kt         — TFLite 推理封装
```

---

## 八、技术选型汇总

| 类别 | 选型 | 版本/说明 |
|------|------|----------|
| **语言** | Kotlin | 100% Kotlin |
| **UI 框架** | Jetpack Compose | + Material 3 |
| **导航** | Navigation Compose | Type-safe routes |
| **数据库** | Room | + Kotlin Coroutines |
| **偏好存储** | DataStore | Preferences |
| **异步** | Kotlin Coroutines + Flow | — |
| **依赖注入** | Hilt | — |
| **音频录制** | AudioRecord | 16kHz, UNPROCESSED |
| **音频分析** | TarsosDSP (移植到Kotlin) 或 自实现 | RMS, ZCR, MFCC |
| **ML 推理** | TensorFlow Lite | Audio Classifier Task API |
| **编码** | MediaCodec (OPUS) | — |
| **图表** | 自定义 Compose Canvas | 无需外部库 |
| **最低 SDK** | API 26 (Android 8.0) | — |
| **目标 SDK** | API 35 (Android 15) | — |

---

## 九、开发路线图

| 阶段 | 内容 | 优先级 |
|------|------|--------|
| **Phase 1** | 项目骨架搭建：Gradle + Hilt + Navigation + M3 Theme | P0 |
| **Phase 2** | AudioRecord 封装 + Foreground Service + WakeLock | P0 |
| **Phase 3** | RMS 能量检测 + 静音过滤 + 基础录音 | P0 |
| **Phase 4** | Room 数据库 + 基础 HomeScreen UI | P0 |
| **Phase 5** | 鼾声检测算法 (ZCR → MFCC → TFLite) | P1 |
| **Phase 6** | SnoreScore 计算 + ResultScreen | P1 |
| **Phase 7** | 趋势图表 + AnalysisScreen | P2 |
| **Phase 8** | 影响因素追踪 + SettingsScreen | P2 |
| **Phase 9** | 音频回放 + 数据导出 | P3 |
| **Phase 10** | OSA 风险评估问卷 | P3 |

---

*本文档为技术骨架设计，后续将根据具体开发进度迭代更新。*
