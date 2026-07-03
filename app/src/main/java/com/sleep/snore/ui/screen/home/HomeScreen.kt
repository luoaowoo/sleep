package com.sleep.snore.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.sleep.snore.navigation.Route
import com.sleep.snore.ui.components.SnoreScoreRing
import com.sleep.snore.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val latestRecord by viewModel.latestRecord.collectAsStateWithLifecycle()
    val recentRecords by viewModel.recentRecords.collectAsStateWithLifecycle()

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
                icon = { Text("🌙") },
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
                .padding(horizontal = Spacing.lg)
                .padding(top = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // 昨晚睡眠概览卡片
            if (latestRecord != null) {
                SleepOverviewCard(record = latestRecord!!) {
                    navController.navigate(Route.Result.createRoute(latestRecord!!.id))
                }
            } else {
                EmptyStateCard()
            }

            // 最近七天趋势迷你图
            if (recentRecords.isNotEmpty()) {
                WeeklyTrendCard(records = recentRecords)
            }

            // AI 快速评价 (如果有)
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
private fun SleepOverviewCard(record: com.sleep.snore.data.db.entity.SleepRecordEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = HeroCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("昨晚睡眠", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)

            Spacer(Modifier.height(Spacing.sm))

            SnoreScoreRing(score = record.snoreScore, size = 160.dp)

            Spacer(Modifier.height(Spacing.md))

            // 指标行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("睡眠时长", "${record.sleepDurationMin / 60}h ${record.sleepDurationMin % 60}m")
                MetricItem("AHI估算", String.format("%.1f", record.estAHI))
                MetricItem("峰值响度", "${String.format("%.0f", record.maxDb)}dB")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = HeroCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🌙", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(Spacing.md))
            Text("还没有睡眠记录", style = MaterialTheme.typography.titleLarge)
            Text("点击下方按钮开始第一次录音", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeeklyTrendCard(records: List<com.sleep.snore.data.db.entity.SleepRecordEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = HeroCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text("📈 本周趋势", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.sm))
            // 简化版：横条图
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
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
                        color = com.sleep.snore.ui.theme.snoreScoreColor(record.snoreScore)
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun AIQuickCard(summary: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = HeroCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🤖", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text("AI 评价", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Text(">", color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}
