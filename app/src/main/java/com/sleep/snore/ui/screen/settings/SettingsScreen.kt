package com.sleep.snore.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sleep.snore.ui.theme.HeroCardShape
import com.sleep.snore.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    var silenceThreshold by remember { mutableFloatStateOf(-40f) }
    var autoClean by remember { mutableStateOf(true) }

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
            // 录音设置
            Text("录音设置", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(shape = HeroCardShape) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text("静音阈值: ${silenceThreshold.toInt()}dB", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(Spacing.xs))
                    Slider(
                        value = silenceThreshold,
                        onValueChange = { silenceThreshold = it },
                        valueRange = -60f..-20f,
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
                        Switch(checked = autoClean, onCheckedChange = { autoClean = it })
                    }
                    Text("当前已用: -- MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 数据
            Text("数据", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card(shape = HeroCardShape) {
                Column {
                    ListItem(headlineContent = { Text("导出数据 (CSV)") }, modifier = Modifier.clickable {})
                    HorizontalDivider()
                    ListItem(headlineContent = { Text("OSA 风险评估") }, modifier = Modifier.clickable {
                        navController.navigate("risk_assessment")
                    })
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
