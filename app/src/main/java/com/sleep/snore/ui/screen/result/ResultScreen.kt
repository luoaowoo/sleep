package com.sleep.snore.ui.screen.result

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sleep.snore.ui.components.SnoreScoreRing
import com.sleep.snore.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    navController: NavHostController,
    recordId: Long,
    viewModel: ResultViewModel = hiltViewModel()
) {
    val record by viewModel.record.collectAsStateWithLifecycle()

    LaunchedEffect(recordId) {
        viewModel.loadRecord(recordId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("睡眠报告") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) { Text("← 返回") }
                }
            )
        }
    ) { padding ->
        record?.let { r ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Spacer(Modifier.height(Spacing.md))

                // Hero SnoreScore 大圆环
                SnoreScoreRing(score = r.snoreScore, size = 180.dp)

                Spacer(Modifier.height(Spacing.md))

                // 核心指标网格
                Card(shape = HeroCardShape) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        MetricsRow(
                            Metric("睡眠时长", "${r.sleepDurationMin / 60}h ${r.sleepDurationMin % 60}m"),
                            Metric("AHI 估算", String.format("%.1f", r.estAHI)),
                            Metric("峰值响度", "${String.format("%.0f", r.maxDb)}dB")
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                        MetricsRow(
                            Metric("打鼾时长", "${r.snoreDurationMin}min"),
                            Metric("鼾声次数", "${r.snoreEventCount}次"),
                            Metric("打鼾占比", "${(r.snoreRatio * 100).toInt()}%")
                        )
                        if (r.longestApneaSec >= 10) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                            MetricsRow(
                                Metric("最长呼吸暂停", "${r.longestApneaSec}秒")
                            )
                        }
                    }
                }

                // AI 评价卡片
                if (r.aiEvaluation.isNotBlank()) {
                    Card(
                        shape = HeroCardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🤖", style = MaterialTheme.typography.headlineSmall)
                                Spacer(Modifier.width(Spacing.sm))
                                Text("AI 详细评价", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(Spacing.sm))
                            Text(r.aiEvaluation, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // 鼾声回放入口
                Card(
                    shape = HeroCardShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎵", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(Spacing.sm))
                        Column(Modifier.weight(1f)) {
                            Text("鼾声集锦", style = MaterialTheme.typography.titleMedium)
                            Text("${r.snoreEventCount} 个片段", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.lg))
            }
        }
    }
}

@Composable
private fun MetricsRow(vararg metrics: Metric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        metrics.forEach { m ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(m.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(m.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

data class Metric(val label: String, val value: String)
