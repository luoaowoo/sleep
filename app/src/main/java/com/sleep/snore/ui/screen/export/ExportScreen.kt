package com.sleep.snore.ui.screen.export

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sleep.snore.ui.theme.HeroCardShape
import com.sleep.snore.ui.theme.PillShape
import com.sleep.snore.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    navController: NavHostController,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val exportFile by viewModel.exportFile.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val exportMessage by viewModel.exportMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exportFile) {
        val file = exportFile ?: return@LaunchedEffect
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "分享睡眠数据 CSV"))
        snackbarHostState.showSnackbar("CSV 已生成")
    }

    LaunchedEffect(exportMessage) {
        exportMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearExportMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("导出数据") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            ExportCard(
                title = "导出睡眠汇总",
                description = "生成每晚睡眠记录、SnoreScore、AHI 估算和鼾声占比等汇总 CSV。",
                buttonText = "生成并分享汇总 CSV",
                isExporting = isExporting,
                onClick = viewModel::exportRecords
            )

            ExportCard(
                title = "导出片段明细",
                description = "生成每个鼾声片段的时间、持续时长、峰值 dB、主频 Hz、AI 标签和音频路径。",
                buttonText = "生成并分享片段 CSV",
                isExporting = isExporting,
                onClick = viewModel::exportEvents
            )

            OutlinedButton(
                onClick = { navController.popBackStack() },
                shape = PillShape,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .heightIn(min = Spacing.touchTargetMin)
            ) {
                Text("返回")
            }
        }
    }
}

@Composable
private fun ExportCard(
    title: String,
    description: String,
    buttonText: String,
    isExporting: Boolean,
    onClick: () -> Unit
) {
    Card(shape = HeroCardShape) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xs))
            Button(
                onClick = onClick,
                enabled = !isExporting,
                shape = PillShape,
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.touchTargetMin)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(buttonText)
                }
            }
        }
    }
}
