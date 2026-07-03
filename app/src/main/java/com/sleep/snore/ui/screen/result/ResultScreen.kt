package com.sleep.snore.ui.screen.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sleep.snore.data.db.entity.SnoreEventEntity
import com.sleep.snore.data.model.SnoreType
import com.sleep.snore.ui.components.PieSlice
import com.sleep.snore.ui.components.SnoreScoreRing
import com.sleep.snore.ui.components.SnoreTimeline
import com.sleep.snore.ui.components.SnoreTypePieChart
import com.sleep.snore.ui.theme.HeroCardShape
import com.sleep.snore.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    navController: NavHostController,
    recordId: Long,
    viewModel: ResultViewModel = hiltViewModel()
) {
    val record by viewModel.record.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()

    LaunchedEffect(recordId) {
        viewModel.loadRecord(recordId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("睡眠报告") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) { Text("返回") }
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
                SnoreScoreRing(score = r.snoreScore, size = 180.dp)
                Spacer(Modifier.height(Spacing.md))

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
                            MetricsRow(Metric("最长呼吸暂停", "${r.longestApneaSec}秒"))
                        }
                    }
                }

                if (events.isNotEmpty()) {
                    Card(shape = HeroCardShape, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            SnoreTimeline(
                                hourlyData = buildHourlyData(events),
                                maxValue = events.groupingBy {
                                    SimpleDateFormat("HH", Locale.getDefault()).format(Date(it.startTimestamp))
                                }.eachCount().values.maxOrNull() ?: 1
                            )
                        }
                    }

                    Card(shape = HeroCardShape, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            SnoreTypePieChart(slices = buildTypeSlices(events))
                        }
                    }
                }

                if (r.aiEvaluation.isNotBlank()) {
                    Card(
                        shape = HeroCardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("AI", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(Spacing.sm))
                                Text("AI 详细评价", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(Spacing.sm))
                            Text(r.aiEvaluation, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Card(
                    shape = HeroCardShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("音频", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(Spacing.sm))
                            Column(Modifier.weight(1f)) {
                                Text("鼾声集锦", style = MaterialTheme.typography.titleMedium)
                                Text("${r.snoreEventCount} 个片段", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (events.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                            Text("片段技术信息", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(Spacing.sm))
                            events.sortedWith(
                                compareByDescending<SnoreEventEntity> { it.peakDb }
                                    .thenByDescending { it.durationMs }
                            ).take(5).forEach { event ->
                                EventTechRow(event)
                                Spacer(Modifier.height(Spacing.xs))
                            }
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

@Composable
private fun EventTechRow(event: SnoreEventEntity) {
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.startTimestamp))
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(time, style = MaterialTheme.typography.labelLarge)
        Text(
            "${event.aiTypeLabel} · 主频 ${String.format("%.0f", event.dominantFreq)}Hz · 峰值 ${String.format("%.0f", event.peakDb)}dB · ${String.format("%.1f", event.durationMs / 1000.0)}s",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildHourlyData(events: List<SnoreEventEntity>): List<Pair<String, Int>> {
    val counts = events.groupingBy {
        SimpleDateFormat("HH", Locale.getDefault()).format(Date(it.startTimestamp))
    }.eachCount()
    return counts.keys.sorted().map { hour -> "${hour}时" to (counts[hour] ?: 0) }
}

private fun buildTypeSlices(events: List<SnoreEventEntity>): List<PieSlice> {
    return events.groupingBy { it.snoreType }
        .eachCount()
        .map { (typeName, count) ->
            val type = runCatching { SnoreType.valueOf(typeName) }.getOrDefault(SnoreType.UNKNOWN)
            PieSlice(type.label, count.toFloat(), typeColor(type))
        }
}

private fun typeColor(type: SnoreType): Color = when (type) {
    SnoreType.SOFT_PALATE -> Color(0xFF6750A4)
    SnoreType.TONGUE_ROOT -> Color(0xFF006D3B)
    SnoreType.EPIGLOTTIS -> Color(0xFFB3261E)
    SnoreType.MIXED -> Color(0xFF7D5260)
    SnoreType.UNKNOWN -> Color(0xFF79747E)
}
