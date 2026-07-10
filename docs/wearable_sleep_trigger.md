# 小米手环睡眠触发接入骨架

## 结论

- 不直接依赖民间小米手环私有协议作为主链路；不同固件和 App 版本容易失效。
- 当前优先走 Android 官方生态：Mi Fitness/小米运动健康同步睡眠到 Health Connect，本应用读取睡眠会话。
- Health Connect 睡眠记录通常不是秒级实时数据，适合自动停止、校准和辅助触发；要提高“睡着后自动开录”的成功率，需要用户睡前开启前台待命。

## 当前实现

- `sleeptrigger/HealthConnectSleepTriggerSource`：读取最近 Health Connect 睡眠会话，并转换为 `SleepStarted` / `SleepEnded`。
- `sleeptrigger/HealthConnectSleepEventInterpreter`：过滤未来/非法睡眠记录，再选择最新有效记录，避免错误事件抢占。
- `sleeptrigger/HealthConnectSleepTriggerWorker`：15 分钟周期轮询；立即检查只要求前台睡眠读取权限，后台轮询要求后台读取权限。
- `sleeptrigger/WearableSleepStandbyService`：睡前前台待命服务，保持可见通知，按 5 分钟间隔前台检查睡眠记录；检测到已确认的睡眠开始后退出待命。
- `sleeptrigger/WearableSleepPollResultHandler`：Worker 和待命服务共用事件处理逻辑；只有录音确认成功后才记住事件 key，失败时保留重试机会。
- `recording/RecordingController`：统一外部触发的录音启动/停止入口，预检麦克风权限并等待前台录音服务确认。
- `service/SleepRecordingService`：前台麦克风服务；只保存疑似鼾声片段，睡眠结束或服务恢复时结算记录。
- `ui/screen/settings`：提供 Health Connect 授权、立即检查、睡前手环待命、麦克风/通知/电池优化引导。

## 小米接入步骤

1. 在 Mi Fitness/小米运动健康中打开 Health Connect 同步，并允许同步睡眠数据。
2. 在本应用设置页授权 Health Connect 睡眠读取和后台读取权限。
3. 睡前授予麦克风、通知权限，并将应用电池策略设为不受限制/允许后台运行。
4. 推荐点击“睡前开启手环待命”；如果设备/系统仍限制后台开麦，则改用“手动前台检测”。

## 后台保活策略

- 周期轮询使用 WorkManager，适合恢复和辅助自动化，不承诺实时。
- 睡前待命使用前台服务和常驻通知，降低 MIUI/Android 后台限制导致的启动失败。
- 真正录音仍由 `SleepRecordingService` 以前台麦克风服务运行，并持有有限时长 WakeLock。
- 进程或服务状态丢失时，睡眠结束事件会尝试恢复数据库中的 active record 并完成结算，避免留下半截记录。

## 已知限制

- Health Connect 同步延迟取决于小米运动健康和系统调度；如果没有进行中的睡眠会话，应用只能在同步后处理已结束记录。
- Android 12+ / 14+ / MIUI 可能限制纯后台启动麦克风服务；待命模式能提高成功率，但不能绕过系统策略。
- 小米如果发布稳定官方 SDK，可新增 `XiaomiSleepTriggerSource`，保持上层 `SleepTriggerEvent` / `AutoSnoreDetectionCoordinator` 不变。

## 隐私边界

- 手环/Health Connect 只作为睡眠触发信号，不上传原始音频。
- 鼾声音频仍由本应用按片段保存在本地。
- DeepSeek 只发送周摘要指标和用户自填信息，不发送原始音频。
