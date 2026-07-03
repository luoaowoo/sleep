# 睡眠鼾声记录 App — 技术骨架设计文档 v2

> 核心需求：智能触发录音 → 鼾声片段回放 → AI 评价 → 技术指标展示
> 设计依据：Material Design 3 + Android 官方架构指南

---

## 一、核心设计理念：智能触发，而非全程录制

```
全程监听 (AudioRecord) —— 极低功耗，只做能量分析
    │
    ├── 静音 (< 阈值) → 丢弃，不写盘，不编码
    │
    ├── 声音事件 (> 阈值) → ZCR 快速判断是否疑似鼾声
    │       │
    │       ├── 非鼾声 (语音/车声/翻身) → 丢弃
    │       │
    │       └── 疑似鼾声 ——→ 触发录音片段 (前后各延长1秒缓冲)
    │               │
    │               ├── PCM → OPUS 编码 → 保存 .ogg (仅保存疑似片段!)
    │               └── MFCC + TFLite → 二次确认 → 标记 snoreType
    │
    └── 连续鼾声 ≥ 10分钟 → 中间每5分钟采样一段即可 (节省空间)
```

### 存储预估

| 场景 | 估算 |
|------|------|
| 8小时睡眠, 30% 时间打鼾 (重度) | ~120 MB PCM 等价 → OPUS 编码后 ~8-12 MB |
| 8小时睡眠, 10% 时间打鼾 (中度) | OPUS 编码后 ~3-5 MB |
| 全程录制 (对比) | OPUS 编码后 ~56 MB |

**结论：智能触发 + OPUS 编码，一晚仅 3-12 MB，远优于全程录制。**

---

## 二、录音保存策略

### 片段模型 vs 整段模型

```
方案 A (推荐)：鼾声事件片段
    每次检测到鼾声 → 录音 3-10 秒 → 单独 .ogg 文件
    优点：回放精准、存储小、方便标注
    缺点：文件数量多

方案 B：会话分段
    每30分钟一个录音文件，但仅编码检测到鼾声的区间
    优点：文件少，连续性好
    缺点：回放不够精准

✅ 采用方案 A + B 混合：
    - 每个鼾声片段独立保存 (3-10秒，.ogg)
    - 同时在 Room 中记录 SnoreEvent (时间戳、时长、峰值dB、类型)
    - 可选：合成整晚鼾声集锦
```

### 文件清理策略

```
- 每晚录制后，清理 > 30 天前的片段文件
- 保留汇总数据 (SleepRecord) 不受影响
- 用户可以手动"收藏"某晚的片段，标记为永久保留
```

---

## 三、AI 评价体系

### 3.1 AI 评价模型

```
输入：
├── 鼾声技术指标 (snoreScore, AHI估算, avgDb, snoreRatio等)
├── 鼾声类型分布 (软腭/舌根/混合)
├── 整夜时间线特征 (早/中/晚各时段表现)
└── 影响因素 (可选: 饮酒、睡姿等)

        ▼
   ┌─────────────┐
   │ 评价引擎     │  —— 规则引擎 + 轻量 LLM (可选)
   │ SnoreEvaluator│
   └──────┬──────┘
          │
          ▼
   生成评价文本 (结构化)
```

### 3.2 评价维度

| 维度 | 说明 | 输出示例 |
|------|------|---------|
| **总体评价** | 一句话总结 | "昨晚打鼾程度中等，比上周平均好15%" |
| **严重程度** | 轻/中/重 + 参考范围 | "AHI 估算 3.2，属正常范围 (< 5)" |
| **时间分布** | 哪个时间段最严重 | "凌晨 2-3 点打鼾最频繁，建议关注睡姿" |
| **响度分析** | 峰值/平均 + 对比 | "峰值 58dB，接近吸尘器音量" |
| **类型特征** | 鼾声类型占比 | "以软腭型为主 (72%)，仰卧时明显" |
| **变化趋势** | 与历史对比 | "连续 3 天改善中，继续坚持侧卧" |
| **健康建议** | 基于指标的建议 | "AHI > 5 持续一周，建议咨询医生" |

### 3.3 评价生成策略

```kotlin
// 规则引擎为 MVP，后续可接 LLM API
class SnoreEvaluator {
    fun evaluate(record: SleepRecord, history: List<SleepRecord>): SnoreEvaluation {
        val builder = StringBuilder()

        // 1. 总体评价 (规则)
        builder.append(evaluateOverall(record.snoreScore))

        // 2. AHI 风险
        builder.append(evaluateAHI(record.estAHI))

        // 3. 趋势对比
        builder.append(evaluateTrend(record, history))

        // 4. 类型归因
        builder.append(evaluateType(record.snoreTypeDistribution))

        // 5. 个性化建议
        builder.append(evaluateAdvice(record))

        return SnoreEvaluation(
            score = record.snoreScore,
            severity = getSeverity(record.snoreScore),
            summary = builder.toString(),
            highlights = extractHighlights(record),
            suggestions = generateSuggestions(record, history)
        )
    }
}
```

---

## 四、技术指标面板 (ResultScreen 核心)

### 展示指标

| 指标 | 计算方式 | 级别 |
|------|---------|------|
| **SnoreScore** | 加权综合 (0-100)，见下方公式 | P0 |
| **AHI估算** | 沉默 ≥10秒 次数 / 睡眠小时 | P0 |
| **打鼾时长** | 累积鼾声事件时长 (分钟) | P0 |
| **打鼾占比** | 打鼾时长 / 总睡眠时长 | P0 |
| **平均响度** | 所有鼾声事件 dB 均值 | P0 |
| **峰值响度** | 单次事件最高 dB | P0 |
| **鼾声次数** | 事件总数 | P0 |
| **鼾声类型分布** | 软腭/舌根/混合 百分比 | P1 |
| **时段分布** | 每小时鼾声事件数柱状图 | P1 |
| **最长呼吸暂停** | 最长连续无声时长 (秒) | P1 |
| **睡眠时长** | (start → end) 减去清醒间隔 | P0 |

### SnoreScore 公式 (MVP)

```
SnoreScore = clamp(
    0.30 * snoreRatioScore     // 打鼾占比: 0-30分
  + 0.25 * loudnessScore      // 响度: 0-25分
  + 0.25 * ahiScore           // AHI估算: 0-25分 (越高越差)
  + 0.20 * eventDensityScore  // 事件密度: 0-20分
, 0, 100)

解释：
- SnoreScore 0-30: 良好，几乎不打鼾
- SnoreScore 31-60: 轻度，偶有打鼾
- SnoreScore 61-80: 中度，频繁打鼾，建议关注
- SnoreScore 81-100: 重度，建议就医评估OSA风险
```

---

## 五、精简后的页面结构

```
Bottom Navigation Bar
├── 🏠 首页 (Dashboard)
│   ├── 昨晚睡眠概览卡片 (SnoreScore大圆环 + 核心指标)
│   ├── AI 每日评价卡片 (一句话 + "查看详情"入口)
│   ├── 7天 SnoreScore 趋势迷你图
│   ├── 最近鼾声片段快捷回放入口
│   └── FAB: "开始睡眠"
│
├── 🎵 回放 (Playback)
│   ├── 按日期分组的历史鼾声片段列表
│   ├── 每个片段: 时间 + 响度标签 + AI类型标签 + 播放按钮
│   ├── 播放器: 波形图 + 播放/暂停 + 进度条
│   └── "收藏"功能，标记重要片段
│
└── ⚙️ 设置 (Settings)
    ├── 录音灵敏度 (静音阈值 dB)
    ├── 鼾声检测灵敏度 (触发阈值)
    ├── 存储管理 (清理旧片段 / 已用空间)
    ├── 数据导出 (CSV 汇总 + 音频集锦)
    ├── OSA 风险评估问卷
    └── 关于 / 隐私政策

点击首页卡片 → 详情页 (ResultScreen)
    ├── 完整指标面板 (所有指标一览)
    ├── AI 详细评价 (完整评价文本 + 建议)
    ├── 整夜时间线柱状图 (每小时鼾声分布)
    ├── 鼾声类型饼图
    └── "播放鼾声集锦" 按钮
```

---

## 六、数据库设计 (精简版)

```kotlin
@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,           // epoch ms
    val endTime: Long,
    val sleepDurationMin: Int,     // 实际睡眠时长(分)
    val snoreScore: Int,           // 0-100
    val snoreScoreSeverity: String,// good | mild | moderate | severe
    val estAHI: Float,
    val snoreDurationMin: Int,     // 累积打鼾时长(分)
    val snoreRatio: Float,         // 打鼾占比 0.0-1.0
    val avgDb: Float,
    val maxDb: Float,
    val snoreEventCount: Int,
    val apneaEventCount: Int,      // 沉默≥10秒事件
    val longestApneaSec: Int,      // 最长呼吸暂停(秒)
    val snoreTypeDistribution: String, // JSON: {"softPalate":0.6,...}
    val hourlyDistribution: String,    // JSON: [5,12,8,...] 每小时事件数
    // AI 评价 (生成后存储，离线可用)
    val aiSummary: String,             // AI 一句话总结
    val aiEvaluation: String,          // AI 完整评价
    val aiSuggestions: String,         // AI 建议列表 (JSON数组)
    val isFavorited: Boolean = false,
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
    val startTimestamp: Long,      // epoch ms
    val durationMs: Int,           // 鼾声持续毫秒
    val peakDb: Float,
    val avgDb: Float,
    val dominantFreq: Float,       // 主导频率 Hz
    val snoreType: String,         // soft_palate | tongue_root | epiglottis | mixed
    val audioFilePath: String,     // 片段 .ogg 文件路径 (绝对路径)
    val audioFileSizeBytes: Long,  // 文件大小
    val aiTypeLabel: String,       // AI 简短标签 e.g. "典型软腭鼾声"
    val isFavorited: Boolean = false
)

@Entity(tableName = "factor_logs")
data class FactorLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,            // 关联 sleep_record，可为空
    val date: String,              // yyyy-MM-dd
    val alcoholDrinks: Int = 0,
    val sleepPosition: String = "unknown", // supine|left_side|right_side|prone
    val weightKg: Float? = null,
    val nasalCongestion: Boolean = false,
    val notes: String? = null
)
```

---

## 七、M3 UI 关键页面设计

### 7.1 HomeScreen (Dashboard)

```
┌──────────────────────────────────┐
│  TopAppBar                       │  56dp
│  🌙 睡眠    7月3日 ▼            │
├──────────────────────────────────┤
│  ┌──────────────────────────────┐│
│  │          昨晚睡眠            ││
│  │                              ││
│  │      ╭──────────╮           ││  outer 160dp
│  │     ╱    72     ╲           ││  大圆环 SnoreScore
│  │    │    中等     │          ││
│  │     ╲           ╱           ││  颜色随分数渐变
│  │      ╰──────────╯           ││  绿色→黄→橙→红
│  │                              ││
│  │  7h 23m  │ AHI 3.2  │58dB峰 ││  三格指标
│  │  睡眠时长│ 呼吸暂停  │峰值响度││
│  │                              ││
│  └──────────────────────────────┘│  圆角 28dp
│                                  │
│  ┌──────────────────────────────┐│
│  │ 🤖 AI 评价                  ││
│  │ "昨晚打鼾中等，凌晨2点较严重││
│  │  比前日改善15%。侧卧时打鼾  ││
│  │  明显减少，建议保持..."     ││
│  │                      [详情>]││
│  └──────────────────────────────┘│
│                                  │
│  📈 本周趋势 (迷你折线)          │
│   80│    ╭╮                      │
│   60│╭╮╭╯╰╮╭╮                  │  SnoreScore 走势
│   40│╰╯   ╰╯╰──                 │  可点击查看详情
│      ├─┬─┬─┬─┬─┬─┬─┤            │
│                                  │
│  🎵 最近鼾声片段                 │
│   [02:15 58dB 软腭]  ▶          │  Chips + 播放
│   [03:42 62dB 混合]  ▶          │
│                                  │
│              ┌──────────┐        │
│              │ 🌙 开始睡眠│       │  FAB (Extended)
│              └──────────┘        │
└──────────────────────────────────┘
│  🏠首页  │  🎵回放  │  ⚙️设置  │  NavBar
└──────────────────────────────────┘
```

### 7.2 RecordingScreen (暗色沉浸)

```
┌──────────────────────────────────┐
│                                  │
│  ○ 正在录音 (呼吸灯脉动动画)     │  TopBar 透明
│                                  │
│                                  │
│            01:23                 │  Display Large
│                                  │  数字时钟
│                                  │
│     ╭──────────────────╮        │
│    ╱ ▂▃▅▃▂ ▂▃▅▃ ▂▃▅▃ ╲       │  实时能量条
│   │ ▃▅█▅▃ ▅█▅▃▅ ▃▅▃▂▃ │      │  鼾声出现时变红
│    ╲ ▂▃▂▃▂ ▂▃▅▃▂ ▅▃▂ ╱       │  静默时接近平
│     ╰──────────────────╯        │
│                                  │
│    鼾声片段已保存: 23 个         │  Body Medium
│    预估文件大小: ~3.2 MB          │
│                                  │
│                                  │
│        ┌──────────────┐          │
│        │  ⏹ 结束睡眠   │          │  FilledButton
│        └──────────────┘          │  primaryContainer
│                                  │
└──────────────────────────────────┘
```

### 7.3 ResultScreen (醒来后)

```
┌──────────────────────────────────┐
│  ← 返回        7月3日 睡眠报告   │  TopAppBar
├──────────────────────────────────┤
│                                  │
│        ╭──────────────╮          │
│       ╱      72       ╲         │  Hero SnoreScore
│      │      中等       │        │
│       ╲               ╱         │
│        ╰──────────────╯          │
│                                  │
│  ┌────────┬────────┬────────┐   │
│  │ 7h23m  │ AHI3.2 │ 58dB峰  │   │  指标卡片行
│  │ 睡眠时长│ 呼吸暂停│ 峰值响度│   │
│  ├────────┼────────┼────────┤   │
│  │ 23min  │  142次 │ 20s    │   │
│  │ 打鼾时长│ 鼾声次数│ 最长暂停│   │
│  └────────┴────────┴────────┘   │
│                                  │
│  ┌──────────────────────────────┐│
│  │ 整夜时间线                   ││
│  │ ▓▓▓░░▓▓▓▓▓░░░▓▓▓▓░▓▓▓▓     ││  柱状图 (每小时)
│  │ 23 00 01 02 03 04 05 06     ││
│  └──────────────────────────────┘│
│                                  │
│  ┌──────────────────────────────┐│
│  │ 🤖 AI 详细评价               ││
│  │                              ││
│  │ 总体: 打鼾程度中等，SnoreSc. ││
│  │ AHI: 3.2，在正常范围(<5)    ││
│  │ 趋势: 比前日改善15%，连续3天.││
│  │ 时段: 凌晨2-3点最严重       ││
│  │ 建议: 继续保持侧卧睡眠姿势   ││
│  │                              ││
│  │ [🏷 软腭型72%] [🏷 混合28%] ││  Chips
│  └──────────────────────────────┘│
│                                  │
│  ┌──────────────────────────────┐│
│  │ 🎵 鼾声集锦 (142个片段)     ││
│  │ [▶ 播放全部] [☆ 收藏]      ││
│  └──────────────────────────────┘│
│                                  │
│  💡 建议: 睡前避免饮酒，尝试侧卧 │  SuggestionChip
│                                  │
└──────────────────────────────────┘
```

### 7.4 PlaybackScreen (回放)

```
┌──────────────────────────────────┐
│  鼾声回放                        │  TopAppBar
├──────────────────────────────────┤
│  ┌─ 7月3日 ────────────────────┐ │
│  │ ▸ 02:15:32  58dB  软腭型    │ │  ListItem
│  │   ═══════════════ 3.2s     │ │  迷你波形
│  ├──────────────────────────────┤ │
│  │ ▸ 02:18:05  62dB  混合型    │ │
│  │   ══════════ 2.1s          │ │
│  ├──────────────────────────────┤ │
│  │ ▸ 02:23:41  55dB  软腭型    │ │
│  │   ═════════════════ 4.0s   │ │
│  ├──────────────────────────────┤ │
│  │ ...                         │ │
│  └──────────────────────────────┘ │
│                                  │
│  ┌─ 7月2日 ────────────────────┐ │
│  │   已收藏 ★ 56 个片段        │ │
│  └──────────────────────────────┘ │
│                                  │
│  ┌──────────────────────────────┐ │  底部播放条
│  │ ═══════════●════════ 02:15   │ │  (当前播放中时显示)
│  │ ▶ ⏸  ▸▸  🔊               │ │
│  └──────────────────────────────┘ │
└──────────────────────────────────┘
```

---

## 八、技术栈 (精简聚焦)

| 类别 | 选型 | 说明 |
|------|------|------|
| **UI** | Jetpack Compose + M3 | 全 Compose |
| **导航** | Navigation Compose | 3 Tab + 详情页 |
| **DB** | Room | sleep_records + snore_events + factor_logs |
| **DI** | Hilt | — |
| **录音** | AudioRecord, 16k/16bit/Mono, UNPROCESSED | 仅低功耗监听 |
| **编码** | MediaCodec OPUS → .ogg | 仅编码鼾声片段 |
| **音频分析** | 自实现 (RMS + ZCR) + TFLite | Kotlin 纯实现 |
| **AI 评价** | 规则引擎 (MVP) + 可选 LLM API | — |
| **图表** | Compose Canvas 自绘 | SnoreScore 环、趋势图、时间线 |
| **最低 SDK** | 26 (Android 8.0) | — |
| **目标 SDK** | 35 | — |
| **架构** | MVVM + Repository | — |

---

## 九、开发路线图 (精简)

| 阶段 | 内容 |
|------|------|
| **P0** | 项目搭建 + M3 Theme + Navigation + Room + Hilt |
| **P0** | AudioRecord 监听 + RMS 能量检测 + 静音过滤 |
| **P0** | 鼾声触发录音 + OPUS 编码保存 + SnoreEvent 入库 |
| **P1** | HomeScreen + RecordingScreen + ResultScreen UI |
| **P1** | SnoreScore 计算 + 规则引擎 AI 评价 |
| **P1** | PlaybackScreen 回放列表 + 播放器 |
| **P2** | TFLite 鼾声分类 (替换 ZCR 规则) |
| **P2** | 趋势图表 + Analysis 深度分析 |
| **P3** | 影响因素追踪 + 数据导出 |
| **P3** | 可选 LLM API 接入 (增强 AI 评价) |

---

*本文档为 v2 精化版本，聚焦智能触发录音 + 片段回放 + AI 评价三大核心需求。*
