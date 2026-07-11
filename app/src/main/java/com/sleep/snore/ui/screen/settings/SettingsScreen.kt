package com.sleep.snore.ui.screen.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.sleep.snore.data.model.AccentColor
import com.sleep.snore.data.model.CardCornerStyle
import com.sleep.snore.data.model.FontScale
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.data.preferences.defaultArgb
import com.sleep.snore.navigation.Route
import com.sleep.snore.sleeptrigger.HealthConnectSleepTriggerSource
import com.sleep.snore.sleeptrigger.HealthConnectSleepTriggerWorker
import com.sleep.snore.sleeptrigger.WearableSleepStandbyService
import com.sleep.snore.ui.theme.LocalUiPreferences
import com.sleep.snore.ui.theme.Spacing
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()
    val customAccentColorArgb by viewModel.customAccentColorArgb.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val cardCornerStyle by viewModel.cardCornerStyle.collectAsStateWithLifecycle()
    val standbyState by WearableSleepStandbyService.standbyState.collectAsStateWithLifecycle()
    val wearableSleepDetectionActive = standbyState.isActive ||
        uiState.activeRecordingTriggerSource == HealthConnectSleepTriggerSource.SOURCE
    val uiPreferences = LocalUiPreferences.current
    val powerManager = remember(context) { context.getSystemService(PowerManager::class.java) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionRefreshTick by remember { mutableStateOf(0) }
    var healthConnectPermissionRefreshTick by remember { mutableStateOf(0) }
    var powerStateRefreshTick by remember { mutableStateOf(0) }
    var companionAppRefreshTick by remember { mutableStateOf(0) }
    val installedXiaomiCompanion = remember(context, companionAppRefreshTick) {
        findInstalledXiaomiCompanion(context)
    }
    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        healthConnectPermissionRefreshTick++
        viewModel.onHealthConnectPermissionsResult(grantedPermissions)
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionRefreshTick++
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionRefreshTick++
    }
    val isIgnoringBatteryOptimizations = remember(context, powerStateRefreshTick) {
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }
    val hasRecordAudioPermission = remember(context, permissionRefreshTick) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    val hasNotificationPermission = remember(context, permissionRefreshTick) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }
    var hasHealthConnectPermission by remember(context) { mutableStateOf(false) }
    LaunchedEffect(context, healthConnectPermissionRefreshTick) {
        hasHealthConnectPermission = runCatching {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE &&
                HealthConnectClient.getOrCreate(context)
                    .permissionController
                    .getGrantedPermissions()
                    .containsAll(HealthConnectSleepTriggerSource.BACKGROUND_REQUIRED_PERMISSIONS)
        }.getOrDefault(false)
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                healthConnectPermissionRefreshTick++
                powerStateRefreshTick++
                companionAppRefreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = uiPreferences.pageHorizontalPadding, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(uiPreferences.sectionSpacing)
        ) {
            Text("外观", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(shape = MaterialTheme.shapes.extraLarge) {
                Column(modifier = Modifier.padding(uiPreferences.cardPadding)) {
                    SettingSwitchRow(
                        title = "Material You 动态色",
                        supportingText = "开启后跟随系统壁纸；关闭后使用下方自定义 RGB 主题色",
                        checked = uiState.dynamicColorEnabled,
                        onCheckedChange = viewModel::onDynamicColorChange
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    ThemeModeSelector(
                        selected = uiState.themeMode,
                        onSelect = viewModel::onThemeModeChange
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text("主题强调色", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(Spacing.xs))
                    AccentPresetSelector(
                        selected = accentColor,
                        onSelect = viewModel::setAccentColor
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    CustomAccentColorSelector(
                        selectedArgb = customAccentColorArgb,
                        dynamicColorEnabled = uiState.dynamicColorEnabled,
                        onColorChange = viewModel::setCustomAccentColorArgb
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text("字号缩放", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(Spacing.xs))
                    FontScaleSelector(
                        selected = fontScale,
                        onSelect = viewModel::setFontScale
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text("卡片圆角风格", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(Spacing.xs))
                    CardCornerStyleSelector(
                        selected = cardCornerStyle,
                        onSelect = viewModel::setCardCornerStyle
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                    SettingSwitchRow(
                        title = "紧凑布局",
                        supportingText = "减少卡片留白，适合小屏或单手快速查看",
                        checked = uiState.compactModeEnabled,
                        onCheckedChange = viewModel::onCompactModeChange
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                    SettingSwitchRow(
                        title = "显示专业技术指标",
                        supportingText = "在报告中展示片段主频、峰值响度和时长",
                        checked = uiState.showTechnicalDetails,
                        onCheckedChange = viewModel::onShowTechnicalDetailsChange
                    )
                }
            }

            Text("录音设置", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(shape = MaterialTheme.shapes.extraLarge) {
                Column(modifier = Modifier.padding(uiPreferences.cardPadding)) {
                    Text("静音阈值: ${uiState.silenceThresholdDb.toInt()}dB", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(Spacing.xs))
                    Slider(
                        value = uiState.silenceThresholdDb,
                        onValueChange = viewModel::onSilenceThresholdChange,
                        valueRange = SettingsPreferencesRepository.MIN_SILENCE_THRESHOLD_DB..SettingsPreferencesRepository.MAX_SILENCE_THRESHOLD_DB,
                        steps = 7
                    )
                    Text("低于此音量的声音不会被录制", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                    Text("单个片段最长: ${uiState.maxSegmentDurationSec} 秒", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(Spacing.xs))
                    Slider(
                        value = uiState.maxSegmentDurationSec.toFloat(),
                        onValueChange = viewModel::onMaxSegmentDurationChange,
                        valueRange = SettingsPreferencesRepository.MIN_MAX_SEGMENT_DURATION_SEC.toFloat()..SettingsPreferencesRepository.MAX_MAX_SEGMENT_DURATION_SEC.toFloat(),
                        steps = 6
                    )
                    Text(
                        "达到上限会自动切分，避免长鼾声持续占用内存和存储",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text("存储管理", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(shape = MaterialTheme.shapes.extraLarge) {
                Column(modifier = Modifier.padding(uiPreferences.cardPadding)) {
                    SettingSwitchRow(
                        title = "自动清理 30 天前的片段",
                        supportingText = "释放本地音频空间，睡眠汇总仍会保留",
                        checked = uiState.autoCleanEnabled,
                        onCheckedChange = viewModel::onAutoCleanChange
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text("当前已用: ${uiState.storageUsageText}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text("后台录音", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(shape = MaterialTheme.shapes.extraLarge) {
                Column {
                    ListItem(
                        headlineContent = {
                            Text(if (isIgnoringBatteryOptimizations) "电池优化已放行" else "建议允许后台整晚运行")
                        },
                        supportingContent = {
                            Text(
                                if (isIgnoringBatteryOptimizations) {
                                    "系统已允许本应用忽略电池优化，夜间录音更不容易被中断。"
                                } else {
                                    "若夜间录音被系统中断，请将本应用设为不受限制/允许后台运行。"
                                }
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = Spacing.touchTargetMin)
                            .clickable(role = Role.Button) {
                                powerStateRefreshTick++
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                    )
                                }.onFailure {
                                    runCatching {
                                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                    }.onFailure {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                }
                            }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("小米/MIUI 后台权限") },
                        supportingContent = { Text("小米/红米/POCO 会优先打开 MIUI 自启动/省电入口；若进入列表页，请找到本应用并开启自启动、后台运行和省电“不限制”。") },
                        modifier = Modifier
                            .heightIn(min = Spacing.touchTargetMin)
                            .clickable(role = Role.Button) {
                                openFirstAvailableSettingsIntent(
                                    context = context,
                                    intents = backgroundPermissionIntents(
                                        packageName = context.packageName,
                                        packageLabel = context.applicationInfo.loadLabel(context.packageManager).toString()
                                    )
                                )
                            }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(if (hasRecordAudioPermission) "麦克风权限已授权" else "缺少麦克风权限") },
                        supportingContent = { Text("睡前前台检测必须已有麦克风权限；未授权时不会后台开麦。点此授权。") },
                        modifier = Modifier
                            .heightIn(min = Spacing.touchTargetMin)
                            .clickable(enabled = !hasRecordAudioPermission, role = Role.Button) {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(if (hasNotificationPermission) "通知权限已授权" else "建议开启通知权限") },
                        supportingContent = { Text("前台检测依赖可见通知；Android 13+ 未授权时后台稳定性会变差。点此授权。") },
                        modifier = Modifier
                            .heightIn(min = Spacing.touchTargetMin)
                            .clickable(
                                enabled = !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                                role = Role.Button
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                    )
                }
            }

            Text("Health Connect 睡眠辅助", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(shape = MaterialTheme.shapes.extraLarge) {
                Column(modifier = Modifier.padding(uiPreferences.cardPadding)) {
                    Text(
                        wearableReadinessSummary(
                            hasRecordAudioPermission = hasRecordAudioPermission,
                            hasNotificationPermission = hasNotificationPermission,
                            hasHealthConnectPermission = hasHealthConnectPermission,
                            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                            hasXiaomiCompanion = installedXiaomiCompanion != null,
                            periodicCheckEnabled = uiState.wearableSleepTriggerEnabled
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (
                            hasRecordAudioPermission &&
                            hasNotificationPermission &&
                            hasHealthConnectPermission &&
                            isIgnoringBatteryOptimizations &&
                            installedXiaomiCompanion != null &&
                            uiState.wearableSleepTriggerEnabled
                        ) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        wearableIntegrationStatusSummary(
                            hasXiaomiCompanion = installedXiaomiCompanion != null,
                            hasHealthConnectPermission = hasHealthConnectPermission,
                            periodicCheckEnabled = uiState.wearableSleepTriggerEnabled,
                            foregroundDetectionActive = wearableSleepDetectionActive
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    SettingSwitchRow(
                        title = "Health Connect 周期检查",
                        supportingText = "小米运动健康同步到 Health Connect 后，本应用按系统调度读取睡眠会话；它不是实时手环直连。此开关只负责周期检查，睡前请点击下方按钮开启前台鼾声检测。",
                        checked = uiState.wearableSleepTriggerEnabled,
                        onCheckedChange = viewModel::onWearableSleepTriggerChange
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                    SettingSwitchRow(
                        title = "睡眠结束后自动停止",
                        supportingText = "读取到同步到 Health Connect 的睡眠结束记录后，自动结束鼾声检测。",
                        checked = uiState.wearableStopOnSleepEndEnabled,
                        onCheckedChange = viewModel::onWearableStopOnSleepEndChange
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "需要授予 Health Connect 睡眠读取和后台读取权限。Android 可能阻止纯后台启动麦克风，所以睡前前台检测会先合法启动麦克风服务。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "小米接入步骤：若 Mi Fitness/小米运动健康或 Zepp Life 当前版本提供 Health Connect 入口，请开启同步并勾选睡眠；再回到本页授权本应用读取睡眠。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    ListItem(
                        headlineContent = {
                            Text(installedXiaomiCompanion?.let { "已检测到 ${it.label}" } ?: "未检测到 Mi Fitness / Zepp Life")
                        },
                        supportingContent = {
                            Text("打开小米伴侣 App 后，若该版本提供 Health Connect 入口，请在个人资料/设置中开启同步睡眠数据。旧设备可能使用 Zepp Life。")
                        },
                        modifier = Modifier
                            .heightIn(min = Spacing.touchTargetMin)
                            .clickable(role = Role.Button) {
                                companionAppRefreshTick++
                                openXiaomiCompanionOrStore(context, installedXiaomiCompanion)
                            }
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "最近状态：${uiState.wearableSleepTriggerStatus}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "最近检查：${uiState.wearableSleepTriggerLastCheckText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "最近同步睡眠：${uiState.latestWearableSleepSessionText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (wearableSleepDetectionActive) {
                        Text(
                            standbyState.statusText.takeIf { standbyState.isActive }
                                ?.let { "检测状态：运行中，$it" }
                                ?: "检测状态：录音服务正在前台检测，并低频检查睡眠结束",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Button(
                        onClick = {
                            healthConnectPermissionLauncher.launch(
                                HealthConnectSleepTriggerSource.BACKGROUND_REQUIRED_PERMISSIONS
                            )
                        }
                    ) {
                        Text("授权 Health Connect")
                    }
                    TextButton(
                        onClick = {
                            openFirstAvailableSettingsIntent(
                                context = context,
                                intents = healthConnectSettingsIntents()
                            )
                        }
                    ) {
                        Text("打开 Health Connect 设置/安装更新")
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Button(
                        onClick = viewModel::checkWearableSleepNow
                    ) {
                        Text("立即检查睡眠记录")
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Button(
                        onClick = {
                            if (wearableSleepDetectionActive) {
                                viewModel.stopWearableSleepStandby()
                            } else {
                                viewModel.startWearableSleepStandby()
                            }
                        }
                    ) {
                        Text(if (wearableSleepDetectionActive) "停止睡前前台检测" else "睡前开启前台检测")
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    TextButton(
                        onClick = { navController.navigate(Route.Recording.route) }
                    ) {
                        Text("改为手动前台检测")
                    }
                }
            }
            Text("数据", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(shape = MaterialTheme.shapes.extraLarge) {
                Column {
                    ListItem(
                        headlineContent = { Text("导出数据 (CSV)") },
                        modifier = Modifier
                            .heightIn(min = Spacing.touchTargetMin)
                            .clickable(role = Role.Button) {
                                navController.navigate(Route.Export.route)
                            }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("OSA 风险评估") },
                        modifier = Modifier
                            .heightIn(min = Spacing.touchTargetMin)
                            .clickable(role = Role.Button) {
                                navController.navigate(Route.RiskAssessment.route)
                            }
                    )
                }
            }

            Text("AI 分析", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(uiPreferences.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        "接入自己的 DeepSeek API 后，首页可生成一周总结。API Key 只保存在本机。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = uiState.deepSeekApiKey,
                        onValueChange = viewModel::onDeepSeekApiKeyChange,
                        label = { Text("DeepSeek API Key") },
                        placeholder = { Text("sk-...") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.deepSeekBaseUrl,
                        onValueChange = viewModel::onDeepSeekBaseUrlChange,
                        label = { Text("Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.deepSeekModelName,
                        onValueChange = viewModel::onDeepSeekModelNameChange,
                        label = { Text("模型名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.aiCustomInfo,
                        onValueChange = viewModel::onAiCustomInfoChange,
                        label = { Text("自定义分析信息") },
                        placeholder = { Text("例如：年龄、身高体重、是否饮酒、侧卧习惯等") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Text("关于", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(shape = MaterialTheme.shapes.extraLarge) {
                Column(modifier = Modifier.padding(uiPreferences.cardPadding)) {
                    Text("睡眠鼾声 v1.0.0", style = MaterialTheme.typography.bodyMedium)
                    Text("隐私优先 · 本地处理 · 数据属于你", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.touchTargetMin)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = Spacing.md)
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                supportingText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeModeSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        SettingsPreferencesRepository.THEME_MODE_SYSTEM to "跟随",
        SettingsPreferencesRepository.THEME_MODE_LIGHT to "浅色",
        SettingsPreferencesRepository.THEME_MODE_DARK to "深色"
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun AccentPresetSelector(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit
) {
    val options = listOf(
        AccentColor.INDIGO to "靛紫",
        AccentColor.BLUE to "蓝",
        AccentColor.GREEN to "绿",
        AccentColor.ORANGE to "橙",
        AccentColor.RED to "红",
        AccentColor.CYAN to "青",
        AccentColor.PINK to "粉"
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color = Color(value.defaultArgb), shape = CircleShape)
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(label)
                }
            }
        }
    }
}

@Composable
private fun CustomAccentColorSelector(
    selectedArgb: Int,
    dynamicColorEnabled: Boolean,
    onColorChange: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var tempArgb by remember(selectedArgb, showDialog) { mutableStateOf(selectedArgb) }
    val supportingText = if (dynamicColorEnabled) {
        "点按后关闭动态色，并使用自定义 RGB 主题"
    } else {
        "自定义主题色生效中，当前 ${selectedArgb.toHexString()}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button) {
                tempArgb = selectedArgb
                showDialog = true
            },
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("自定义 RGB 色环", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(supportingText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ColorSwatch(selectedArgb, size = 40.dp)
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("自定义 RGB 主题色") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HueColorWheel(
                            selectedArgb = tempArgb,
                            onHueChange = { hue -> tempArgb = tempArgb.withHue(hue) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tempArgb.toHexString(), style = MaterialTheme.typography.titleMedium)
                        ColorSwatch(tempArgb, size = 48.dp)
                    }
                    RgbSlider("R", AndroidColor.red(tempArgb)) { red ->
                        tempArgb = AndroidColor.argb(255, red, AndroidColor.green(tempArgb), AndroidColor.blue(tempArgb))
                    }
                    RgbSlider("G", AndroidColor.green(tempArgb)) { green ->
                        tempArgb = AndroidColor.argb(255, AndroidColor.red(tempArgb), green, AndroidColor.blue(tempArgb))
                    }
                    RgbSlider("B", AndroidColor.blue(tempArgb)) { blue ->
                        tempArgb = AndroidColor.argb(255, AndroidColor.red(tempArgb), AndroidColor.green(tempArgb), blue)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onColorChange(tempArgb)
                        showDialog = false
                    }
                ) {
                    Text("应用")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun HueColorWheel(
    selectedArgb: Int,
    onHueChange: (Float) -> Unit
) {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(selectedArgb, hsv)
    val selectedHue = hsv[0]
    val strokeWidth = 24.dp
    Canvas(
        modifier = Modifier
            .size(176.dp)
            .pointerInput(Unit) {
                fun updateHue(offset: Offset) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val degrees = Math.toDegrees(
                        atan2(
                            offset.y - center.y,
                            offset.x - center.x
                        ).toDouble()
                    ).toFloat()
                    onHueChange((degrees + 360f) % 360f)
                }
                detectTapGestures { updateHue(it) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ -> onHueChange(pointerHue(change.position, size.width, size.height)) }
            }
    ) {
        val strokePx = strokeWidth.toPx()
        for (degree in 0 until 360) {
            drawArc(
                color = Color(AndroidColor.HSVToColor(floatArrayOf(degree.toFloat(), 0.82f, 0.95f))),
                startAngle = degree.toFloat(),
                sweepAngle = 1.4f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt)
            )
        }
        val radius = (size.minDimension - strokePx) / 2f
        val angle = Math.toRadians(selectedHue.toDouble())
        val center = Offset(size.width / 2f, size.height / 2f)
        val thumb = Offset(
            x = center.x + kotlin.math.cos(angle).toFloat() * radius,
            y = center.y + kotlin.math.sin(angle).toFloat() * radius
        )
        drawCircle(color = Color.White, radius = 10.dp.toPx(), center = thumb)
        drawCircle(color = Color(selectedArgb), radius = 7.dp.toPx(), center = thumb)
    }
}

@Composable
private fun RgbSlider(
    label: String,
    value: Int,
    onChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label $value", modifier = Modifier.width(56.dp), style = MaterialTheme.typography.labelLarge)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt().coerceIn(0, 255)) },
            valueRange = 0f..255f,
            steps = 254,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ColorSwatch(argb: Int, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color(argb), CircleShape)
    )
}

@Composable
private fun FontScaleSelector(
    selected: FontScale,
    onSelect: (FontScale) -> Unit
) {
    val options = listOf(
        FontScale.SMALL to "小",
        FontScale.STANDARD to "标准",
        FontScale.LARGE to "大"
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun CardCornerStyleSelector(
    selected: CardCornerStyle,
    onSelect: (CardCornerStyle) -> Unit
) {
    val options = listOf(
        CardCornerStyle.STANDARD to "标准",
        CardCornerStyle.SOFT to "柔和",
        CardCornerStyle.SHARP to "锐利"
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(label)
            }
        }
    }
}

private fun Int.toHexString(): String {
    return "#%02X%02X%02X".format(AndroidColor.red(this), AndroidColor.green(this), AndroidColor.blue(this))
}

private fun Int.withHue(hue: Float): Int {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(this, hsv)
    hsv[0] = hue
    hsv[1] = hsv[1].coerceAtLeast(0.65f)
    hsv[2] = hsv[2].coerceAtLeast(0.75f)
    return AndroidColor.HSVToColor(hsv)
}

private fun pointerHue(position: Offset, width: Int, height: Int): Float {
    val center = Offset(width / 2f, height / 2f)
    val degrees = Math.toDegrees(
        atan2(
            position.y - center.y,
            position.x - center.x
        ).toDouble()
    ).toFloat()
    return (degrees + 360f) % 360f
}

private data class XiaomiCompanionApp(
    val label: String,
    val packageName: String
)

private val XiaomiCompanionApps = listOf(
    XiaomiCompanionApp("Mi Fitness", "com.xiaomi.wearable"),
    XiaomiCompanionApp("Zepp Life", "com.xiaomi.hm.health")
)

private fun findInstalledXiaomiCompanion(context: android.content.Context): XiaomiCompanionApp? {
    return XiaomiCompanionApps.firstOrNull { app ->
        context.packageManager.getLaunchIntentForPackage(app.packageName) != null
    }
}

private fun openXiaomiCompanionOrStore(
    context: android.content.Context,
    app: XiaomiCompanionApp?
) {
    val packageName = app?.packageName ?: XiaomiCompanionApps.first().packageName
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        context.startActivity(launchIntent)
        return
    }

    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
    runCatching {
        context.startActivity(marketIntent)
    }.onFailure {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
        )
    }
}
