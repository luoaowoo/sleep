package com.sleep.snore.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sleep.snore.data.preferences.SettingsPreferencesRepository
import com.sleep.snore.navigation.Route
import com.sleep.snore.ui.theme.HeroCardShape
import com.sleep.snore.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text("外观", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(shape = HeroCardShape) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Material You 动态色")
                        Switch(
                            checked = uiState.dynamicColorEnabled,
                            onCheckedChange = viewModel::onDynamicColorChange
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf(
                            SettingsPreferencesRepository.THEME_MODE_SYSTEM to "跟随",
                            SettingsPreferencesRepository.THEME_MODE_LIGHT to "浅色",
                            SettingsPreferencesRepository.THEME_MODE_DARK to "深色"
                        )
                        options.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = uiState.themeMode == mode,
                                onClick = { viewModel.onThemeModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                            ) {
                                Text(label)
                            }
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
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

            // 录音设置
            Text("录音设置", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(shape = HeroCardShape) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text("静音阈值: ${uiState.silenceThresholdDb.toInt()}dB", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(Spacing.xs))
                    Slider(
                        value = uiState.silenceThresholdDb,
                        onValueChange = viewModel::onSilenceThresholdChange,
                        valueRange = SettingsPreferencesRepository.MIN_SILENCE_THRESHOLD_DB..SettingsPreferencesRepository.MAX_SILENCE_THRESHOLD_DB,
                        steps = 7
                    )
                    Text("低于此音量的声音不会被录制", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 存储设置
            Text("存储管理", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(shape = HeroCardShape) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("自动清理 30 天前的片段")
                        Switch(
                            checked = uiState.autoCleanEnabled,
                            onCheckedChange = viewModel::onAutoCleanChange
                        )
                    }
                    Text("当前已用: ${uiState.storageUsageText}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 数据
            Text("数据", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(shape = HeroCardShape) {
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

            // 关于
            Text("关于", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(shape = HeroCardShape) {
                Column(modifier = Modifier.padding(Spacing.md)) {
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
            .heightIn(min = Spacing.touchTargetMin),
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
