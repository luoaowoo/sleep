package com.sleep.snore.ui.screen.recording

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.sleep.snore.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordingScreen(navController: NavHostController) {
    val context = LocalContext.current
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isRecording by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) isRecording = true
    }

    // 计时器
    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000)
            elapsedSeconds++
        }
    }

    // 脉动动画
    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
    )

    // 权限检查
    if (!hasPermission && !isRecording) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.xxl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎤", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(Spacing.lg))
            Text("需要录音权限", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(Spacing.md))
            Text("请授权录音权限以开始鼾声监测", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.xl))
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                shape = PillShape
            ) { Text("授权") }
        }
        return
    }

    // 录音中界面 — 沉浸式暗色
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceDim),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 时钟
            val hours = elapsedSeconds / 3600
            val minutes = (elapsedSeconds % 3600) / 60
            val seconds = elapsedSeconds % 60
            Text(
                text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Light),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(Spacing.xxl))

            // 脉动指示器
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .alpha(pulseAlpha),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    ) {}
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            Text("正在监测鼾声", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

            Spacer(Modifier.height(Spacing.xxl))

            // 停止按钮
            OutlinedButton(
                onClick = { isRecording = false; navController.popBackStack() },
                shape = PillShape,
                modifier = Modifier.widthIn(min = 200.dp)
            ) {
                Text("⏹ 结束睡眠", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
