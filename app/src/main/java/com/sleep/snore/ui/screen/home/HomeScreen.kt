package com.sleep.snore.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.navigation.Route
import com.sleep.snore.ui.components.SnoreScoreRing
import com.sleep.snore.ui.theme.LocalUiPreferences
import com.sleep.snore.ui.theme.PillShape
import com.sleep.snore.ui.theme.Spacing
import com.sleep.snore.ui.theme.snoreScoreColor
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val latestRecord by viewModel.latestRecord.collectAsStateWithLifecycle()
    val recentRecords by viewModel.recentRecords.collectAsStateWithLifecycle()
    val weeklyReportState by viewModel.weeklyReportState.collectAsStateWithLifecycle()
    val uiPreferences = LocalUiPreferences.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("睡眠概览") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Route.Recording.route) },
                icon = { Text("睡") },
                text = { Text("开始睡眠") },
                shape = PillShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(horizontal = uiPreferences.pageHorizontalPadding)
                .padding(top = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(uiPreferences.sectionSpacing)
        ) {
            latestRecord?.let { record ->
                SleepOverviewCard(record = record) {
                    navController.navigate(Route.Result.createRoute(record.id))
                }
            } ?: EmptyStateCard()

            if (recentRecords.isNotEmpty()) {
                WeeklyTrendCard(records = recentRecords)
            }

            WeeklySummaryCard(
                state = weeklyReportState,
                onGenerateAi = viewModel::generateDeepSeekWeeklyReport,
                onOpenSettings = { navController.navigate(Route.Settings.route) }
            )

            latestRecord?.let { record ->
                if (record.aiSummary.isNotBlank()) {
                    AIQuickCard(summary = record.aiSummary) {
                        navController.navigate(Route.Result.createRoute(record.id))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SleepOverviewCard(record: SleepRecordEntity, onClick: () -> Unit) {
    val uiPreferences = LocalUiPreferences.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(uiPreferences.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("昨晚睡眠", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(Spacing.sm))
            SnoreScoreRing(score = record.snoreScore, size = 160.dp)
            Spacer(Modifier.height(Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("睡眠时长", "${record.sleepDurationMin / 60}h ${record.sleepDurationMin % 60}m")
                MetricItem("AHI估算", String.format(Locale.getDefault(), "%.1f", record.estAHI))
                MetricItem("峰值响度", "${String.format(Locale.getDefault(), "%.0f", record.maxDb)}dB")
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyStateCard() {
    val uiPreferences = LocalUiPreferences.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (uiPreferences.compactModeEnabled) Spacing.xl else Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("睡", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(Spacing.md))
            Text("还没有睡眠记录", style = MaterialTheme.typography.titleLarge)
            Text("点击下方按钮开始第一次录音", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeeklyTrendCard(records: List<SleepRecordEntity>) {
    val uiPreferences = LocalUiPreferences.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(uiPreferences.cardPadding)) {
            Text("本周趋势", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.Bottom
            ) {
                records.reversed().forEach { record ->
                    val heightFraction = (record.snoreScore / 100f).coerceIn(0.05f, 1f)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(heightFraction),
                        shape = PillShape,
                        color = snoreScoreColor(record.snoreScore)
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun WeeklySummaryCard(
    state: WeeklyReportUiState,
    onGenerateAi: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val uiPreferences = LocalUiPreferences.current
    val report = state.report
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(uiPreferences.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("本周总结", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (state.usesRemoteAi) {
                    Text("DeepSeek", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (report != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    MetricItem("记录", "${report.recordCount}晚")
                    MetricItem("均分", "${report.averageScore}")
                    MetricItem("鼾声", "${report.totalSnoreMinutes}m")
                }
                Text(report.aiSummary, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("完成几晚录音后，这里会生成一周睡眠鼾声总结。", style = MaterialTheme.typography.bodyMedium)
            }
            state.errorMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(onClick = onGenerateAi, enabled = !state.isGenerating) {
                    if (state.isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(Spacing.xs))
                    }
                    Text("DeepSeek 分析")
                }
                TextButton(onClick = onOpenSettings) {
                    Text("配置 API")
                }
            }
        }
    }
}

@Composable
private fun AIQuickCard(summary: String, onClick: () -> Unit) {
    val uiPreferences = LocalUiPreferences.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(uiPreferences.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AI", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text("AI 评价", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Text(">", color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}
