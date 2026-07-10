# 小米手环睡眠触发接入骨架

## 结论

- 不直接依赖“民间小米手环私有协议”作为主链路；不同固件和 App 版本容易失效。
- 优先走官方 Android 生态的 Health Connect：让小米运动健康 / Mi Fitness 同步睡眠记录后，本应用读取睡眠会话。
- Health Connect 睡眠记录通常不是秒级实时数据，适合校准、自动停止和自动化辅助；如果要“睡着立刻开录”，仍建议用户睡前开启前台检测，保持可见通知和麦克风前台服务。

## 当前代码骨架

- `recording/RecordingController`：统一外部触发的开始/停止录音入口，最终复用 `SleepRecordingService.startIntent()` / `stopIntent()`。
- `sleeptrigger/SleepTriggerEvent`：抽象睡眠开始、睡眠结束事件。
- `sleeptrigger/SleepTriggerSource`：后续 Health Connect、小米 SDK、手动预备模式都实现这个接口。
- `sleeptrigger/AutoSnoreDetectionCoordinator`：判断设置、置信度和睡眠结束策略，再调用录音控制器。
- `SettingsPreferencesRepository`：已预留手环睡眠触发开关和睡眠结束自动停止开关。

## 后续实现路线

1. 设置页已增加“手环自动检测”区域：可开启 Health Connect 睡眠触发、睡眠结束自动停止，并提供 Health Connect 授权与睡前前台检测入口。
2. 接入 Health Connect：
   - 申请读取睡眠会话权限和后台读取权限。
   - 读取最近睡眠记录，转换为 `SleepTriggerEvent.SleepStarted/SleepEnded`。
   - 对非实时同步加 UI 说明，避免用户误以为是即时唤醒。
   - 读取失败或授权缺失时安全退出 Worker，避免后台静默崩溃。
   - 确认由睡眠触发开启录音后再持久化睡眠事件 key，避免异步启动误报成功；未确认时保留重试机会。
   - 设置页展示最近轮询状态和检查时间，便于排查 Health Connect 授权/同步问题。
   - 前台录音服务在检测真正启动后持久化触发来源，进程重启后仍可让睡眠结束事件停止手环自动开启的录音，同时避免误停用户手动录音。
   - 开机或应用更新后通过 `WearableSleepTriggerBootReceiver` 恢复周期轮询，并立即执行一次检查。
   - 自动开麦前预检麦克风权限；缺少麦克风权限时不尝试后台启动，并在设置页状态中提示原因。通知权限作为 Android 13+ 稳定性建议展示。
   - 当前工程使用已验证可构建的 `androidx.health.connect:connect-client:1.1.0-alpha11`；后续网络/缓存稳定后可按 AndroidX 发布节奏升级。
3. 如果小米官方发布可用 SDK，再新增 `XiaomiSleepTriggerSource`，保持对上层接口不变。
4. 后台保活策略：
   - 录音必须通过前台麦克风服务运行。
   - 用户睡前完成录音、通知、电池优化等授权。
   - 纯后台收到手环事件后直接拉起麦克风服务可能受 Android 后台限制，需以前台通知/预备模式降低失败率。

## 隐私边界

- 手环/Health Connect 只作为触发信号，不上传原始音频。
- 鼾声音频仍由本应用按片段保存。
- DeepSeek 只发送周摘要指标和用户自填信息，不发送原始音频。
