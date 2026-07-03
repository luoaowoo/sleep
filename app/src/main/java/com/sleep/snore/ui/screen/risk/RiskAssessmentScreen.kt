package com.sleep.snore.ui.screen.risk

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.sleep.snore.ui.theme.HeroCardShape
import com.sleep.snore.ui.theme.PillShape
import com.sleep.snore.ui.theme.Spacing
import com.sleep.snore.ui.theme.snoreScoreColor

data class StopBangQuestion(
    val id: String,
    val question: String,
    val detail: String
)

val stopBangQuestions = listOf(
    StopBangQuestion("S", "打鼾 (Snoring)", "您打鼾声音大吗（比说话声音大，或者隔着门都能听到）？"),
    StopBangQuestion("T", "疲倦 (Tired)", "您白天经常感到疲倦、乏力或想睡觉吗？"),
    StopBangQuestion("O", "观察呼吸暂停 (Observed)", "是否有人观察到您睡觉时呼吸停止？"),
    StopBangQuestion("P", "血压 (Pressure)", "您是否正在接受高血压治疗？"),
    StopBangQuestion("B", "BMI", "您的 BMI 是否大于 35 kg/m²？"),
    StopBangQuestion("A", "年龄 (Age)", "您的年龄是否超过 50 岁？"),
    StopBangQuestion("N", "颈围 (Neck)", "您的颈围是否超过 40cm？"),
    StopBangQuestion("G", "性别 (Gender)", "您是男性吗？")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskAssessmentScreen(navController: NavHostController) {
    val answers = remember { mutableStateMapOf<String, Boolean>() }
    var showResult by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OSA 风险评估") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) { Text("← 返回") }
                }
            )
        }
    ) { padding ->
        if (showResult) {
            val score = answers.values.count { it }
            RiskResultContent(score = score, padding = padding, navController = navController)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    "STOP-BANG 问卷",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "请根据您的实际情况回答以下问题。这是国际通用的 OSA 筛查工具。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                stopBangQuestions.forEach { q ->
                    Card(shape = HeroCardShape) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Text("${q.id}) ${q.question}", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(Spacing.xs))
                            Text(q.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(Spacing.sm))
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                FilterChip(
                                    selected = answers[q.id] == true,
                                    onClick = { answers[q.id] = true },
                                    label = { Text("是") },
                                    shape = PillShape
                                )
                                FilterChip(
                                    selected = answers[q.id] == false,
                                    onClick = { answers[q.id] = false },
                                    label = { Text("否") },
                                    shape = PillShape
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { showResult = true },
                        shape = PillShape,
                        enabled = answers.size >= 5,
                        modifier = Modifier.padding(vertical = Spacing.lg)
                    ) {
                        Text("查看结果")
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskResultContent(score: Int, padding: PaddingValues, navController: NavHostController) {
    val riskLevel = when {
        score <= 2 -> "低风险"
        score in 3..4 -> "中风险"
        else -> "高风险"
    }
    val riskColor = when {
        score <= 2 -> snoreScoreColor(20)
        score in 3..4 -> snoreScoreColor(55)
        else -> snoreScoreColor(85)
    }
    val advice = when {
        score <= 2 -> "您的 OSA 风险较低。保持健康生活方式，定期体检即可。"
        score in 3..4 -> "您有中等 OSA 风险。建议咨询医生，考虑进行睡眠监测。"
        else -> "您有较高 OSA 风险。强烈建议尽快就医，进行多导睡眠监测 (PSG)。"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "评分: $score / 8",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = riskColor
        )
        Spacer(Modifier.height(Spacing.md))
        Text(riskLevel, style = MaterialTheme.typography.headlineMedium, color = riskColor)
        Spacer(Modifier.height(Spacing.xl))

        Card(
            shape = HeroCardShape,
            colors = CardDefaults.cardColors(containerColor = riskColor.copy(alpha = 0.1f))
        ) {
            Text(
                advice,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(Spacing.lg)
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        OutlinedButton(
            onClick = {
                // 回到首页
                navController.popBackStack()
            },
            shape = PillShape
        ) {
            Text("返回首页")
        }

        Spacer(Modifier.height(Spacing.xs))

        Text(
            "⚠️ 本问卷仅供参考，不能替代专业医疗诊断",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
