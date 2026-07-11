# 小米手环睡眠触发接入骨架

## 结论

- 不直接依赖民间小米手环私有协议作为主链路；不同固件和 App 版本容易失效。
- 当前优先走 Android 官方生态：Mi Fitness/小米运动健康同步睡眠到 Health Connect，本应用读取睡眠会话。
- Health Connect 睡眠记录通常不是秒级实时数据，适合自动停止、校准和辅助提示；可靠的整晚录音方式是用户睡前开启前台检测，入口会先启动前台鼾声检测，再用手环/Health Connect 记录做自动停止和校准。
- Android 对麦克风属于 while-in-use 权限，后台 Worker/广播不能可靠、合规地在用户睡着后再启动麦克风前台服务；因此当前版本明确不做“纯后台睡眠开始即开麦”，而是做“睡前合法前台开麦 + 手环睡眠结束自动停录”。

## 当前实现

- `sleeptrigger/HealthConnectSleepTriggerSource`：读取最近 Health Connect 睡眠会话，主要使用已同步的 `SleepEnded`；`SleepStarted` 不作为可靠实时开麦依据。
- `sleeptrigger/HealthConnectSleepEventInterpreter`：过滤未来/非法睡眠记录，再选择最新有效记录，避免错误事件抢占。
- `sleeptrigger/HealthConnectSleepTriggerWorker`：15 分钟周期轮询；立即检查只要求前台睡眠读取权限，后台轮询要求后台读取权限。Worker 不会直接启动麦克风，只处理已结束睡眠和状态提示，避免触发 Android 后台麦克风前台服务限制。
- Worker、录音服务兜底轮询和兼容待命服务都会按当前手环触发录音的开始时间过滤旧 `SleepEnded`，避免用户睡前刚开启检测时被上一晚已结束记录误停。
- `sleeptrigger/RecordingSleepEndFallbackPoller`：当前手环触发录音运行期间，由前台麦克风服务低频读取 Health Connect；只处理 `SleepEnded`，不会因 `SleepStarted` 再次自触发启动录音。
- `sleeptrigger/WearableSleepStandbyService`：保留为兼容的前台待命/停止入口；Android 15 对 `dataSync` 前台服务有 6 小时/24 小时额度，因此主链路不再依赖它整晚轮询，也不再用它在睡眠开始后拉起麦克风。
- `ui/screen/settings/SettingsViewModel`：启动睡前待命前会硬性检查麦克风、通知、Health Connect 睡眠/后台读取权限；权限齐全后通过 `RecordingController` 合法启动前台麦克风检测，由录音服务承担睡眠结束兜底轮询。
- `ui/screen/settings/SettingsScreen`：检测并打开 Mi Fitness（`com.xiaomi.wearable`）或 Zepp Life（`com.xiaomi.hm.health`），方便用户到小米伴侣 App 中开启 Health Connect 睡眠同步。
- `sleeptrigger/WearableSleepPollResultHandler`：Worker 和待命服务共用事件处理逻辑；只有录音确认成功后才记住事件 key，失败时保留重试机会。
- `recording/RecordingController`：统一外部触发的录音启动/停止入口，预检麦克风权限并等待前台录音服务确认。
- `recording/ActiveRecordingFinalizerWorker`：睡眠结束停止录音时会排一个延迟兜底任务；如果前台录音服务已被系统杀掉或停止请求没能完成，Worker 会优先读取 Health Connect 的真实睡眠结束时间，再结算数据库中的手环触发 active record，避免留下半截记录。
- `service/SleepRecordingService`：前台麦克风服务；只保存疑似鼾声片段，睡眠结束或服务恢复时结算记录。
- `sleeptrigger/WearableSleepTriggerBootReceiver`：开机/应用更新后恢复 WorkManager 检查；如果发现手环触发的未完成录音记录，会安排兜底结算，而不是违反系统限制在后台强行开麦。
- `ui/screen/settings`：提供 Health Connect 授权、立即检查、睡前前台检测、麦克风/通知/电池优化引导。

## 小米接入步骤

1. 在 Mi Fitness/小米运动健康中打开 Health Connect 同步，并允许同步睡眠数据；是否提供该入口取决于设备、地区和 App 版本，旧手环或旧账号生态可能使用 Zepp Life。
2. 在本应用设置页授权 Health Connect 睡眠读取和后台读取权限。
3. 睡前授予麦克风、通知、Health Connect 睡眠/后台读取权限，并将应用电池策略设为不受限制/允许后台运行；设置页“小米/MIUI 后台权限”会在小米/红米/POCO 设备上优先尝试打开 MIUI 自启动/省电入口，找不到或启动失败时继续回退到应用详情、电池优化和系统设置。
4. 推荐点击“睡前开启前台检测”；它会先开始前台鼾声检测，再等待 Health Connect 睡眠记录用于自动停止/校准。单独打开“Health Connect 周期检查”只负责系统调度读取记录，不等同于整晚前台录音。

## 后台保活策略

- 周期轮询使用 WorkManager，适合恢复、自动停止和辅助状态提示，不承诺实时，也不会在后台直接开麦。
- 睡前前台检测会先启动前台麦克风检测，降低 MIUI/Android 限制纯后台麦克风启动导致的失败和误报成功。
- 当前录音如果由睡前前台检测以手环/Health Connect 来源启动，`SleepRecordingService` 会在前台麦克风服务内低频轮询 Health Connect 睡眠结束事件；这样不依赖 `dataSync` 待命服务整晚占用前台服务额度。
- 用户点击“停止睡前前台检测”时，会同时请求停止由睡前入口预开启的前台鼾声检测，避免用户误以为只停了轮询但录音仍在运行。
- `WearableSleepStandbyService` 不是无限后台服务：Android 15 会限制 `dataSync` 前台服务累计时长，因此该兼容服务会在接近 6 小时限制前自停并提示用户重新开启。
- 真正录音仍由 `SleepRecordingService` 以前台麦克风服务运行，并持有有限时长 WakeLock。
- 进程或服务状态丢失时，睡眠结束事件会尝试恢复数据库中的 active record 并完成结算，避免留下半截记录。
- 睡眠结束停止请求会携带期望来源，录音服务会再次确认当前 active record 仍来自 Health Connect，避免旧的自动停止请求误停用户后来手动开启的录音。
- 睡眠结束停止请求会额外排一个延迟兜底结算 Worker；若服务已经正常结算并清除 active record，Worker 会自动空跑；若仍有 active record，则优先按 Health Connect 睡眠结束时间结算，拿不到时才按当前时间估算。
- 设备重启或应用更新后，如果系统不允许后台恢复麦克风前台服务，应用会优先结算未完成的手环触发记录并提示状态，避免数据长期卡在 active 状态。

## 已知限制

- Health Connect 同步延迟取决于小米运动健康和系统调度；多数场景下应用只能在同步后处理已结束记录，因此“睡着后开始录音”的可靠方案是睡前先开启前台检测。
- Android 12+ / 14+ / MIUI 可能限制纯后台启动麦克风服务；Android 14+ 对 while-in-use 权限下的麦克风前台服务限制更严格，待命模式只能提高用户可见前台流程的稳定性，不能绕过系统策略。
- 当前未接入、也不承诺存在公开的小米实时睡眠 API；如果小米后续发布稳定官方 SDK 或系统级可授权睡眠事件 API，可新增 `XiaomiSleepTriggerSource`，保持上层 `SleepTriggerEvent` / `AutoSnoreDetectionCoordinator` 不变；在没有这类官方实时事件前，不应把私有蓝牙协议作为默认主链路。

## 隐私边界

- 手环/Health Connect 只作为睡眠自动停止和校准信号，不上传原始音频。
- 鼾声音频仍由本应用按片段保存在本地。
- DeepSeek 只发送周摘要指标和用户自填信息，不发送原始音频。

## 参考资料

- Android Health Connect：用户授权后的健康数据读取与写入入口，可用于读取睡眠数据：https://developer.android.com/health-and-fitness/health-connect
- Health Connect `SleepSessionRecord`：官方睡眠会话数据结构：https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/SleepSessionRecord
- Android 后台启动前台服务限制：Android 14+ 对需要 while-in-use 权限的前台服务有额外限制：https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Android 14 前台服务类型：麦克风服务必须声明对应类型和权限：https://developer.android.com/about/versions/14/changes/fgs-types-required
