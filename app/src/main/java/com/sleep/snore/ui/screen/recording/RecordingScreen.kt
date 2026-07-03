package com.sleep.snore.ui.screen.recording

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sleep.snore.service.SleepRecordingService
import com.sleep.snore.ui.theme.PillShape
import com.sleep.snore.ui.theme.Spacing
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun RecordingScreen(navController: NavHostController) {
    val context = LocalContext.current
    val recordingState by SleepRecordingService.recordingState.collectAsStateWithLifecycle()
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var hasAudioPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun startRecording() {
        if (!recordingState.isActive && hasAudioPermission) {
            ContextCompat.startForegroundService(context, SleepRecordingService.startIntent(context))
        }
    }

    fun stopRecording() {
        context.startService(SleepRecordingService.stopIntent(context))
        navController.popBackStack()
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAudioPermission = granted
        if (granted) startRecording()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotificationPermission = granted
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) startRecording()
    }

    LaunchedEffect(recordingState.isActive, recordingState.startTime) {
        while (recordingState.isActive) {
            elapsedSeconds = ((System.currentTimeMillis() - recordingState.startTime) / 1000L).toInt().coerceAtLeast(0)
            delay(1000)
        }
    }

    BackHandler(enabled = recordingState.isActive) { stopRecording() }

    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    if (!hasAudioPermission) {
        PermissionContent(
            onGrantAudio = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceDim),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val hours = elapsedSeconds / 3600
            val minutes = (elapsedSeconds % 3600) / 60
            val seconds = elapsedSeconds % 60
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds),
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Light),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(Spacing.xxl))
            Surface(
                modifier = Modifier.size(120.dp).alpha(pulseAlpha),
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
            Text(TEXT_MONITORING, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    TEXT_NOTIFICATION_HINT,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.sm))
                Button(onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }, shape = PillShape) {
                    Text(TEXT_GRANT_NOTIFICATION)
                }
            }
            Spacer(Modifier.height(Spacing.xxl))
            OutlinedButton(
                onClick = { stopRecording() },
                shape = PillShape,
                modifier = Modifier.widthIn(min = 200.dp)
            ) {
                Text(TEXT_STOP_SLEEP, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun PermissionContent(
    onGrantAudio: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(TEXT_RECORD, style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(Spacing.lg))
        Text(TEXT_NEED_AUDIO_PERMISSION, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(Spacing.md))
        Text(TEXT_PERMISSION_HINT, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xl))
        Button(onClick = onGrantAudio, shape = PillShape) { Text(TEXT_GRANT_AUDIO) }
    }
}

private const val TEXT_RECORD = "\u5f55"
private const val TEXT_MONITORING = "\u6b63\u5728\u76d1\u6d4b\u9f3e\u58f0"
private const val TEXT_STOP_SLEEP = "\u7ed3\u675f\u7761\u7720"
private const val TEXT_NEED_AUDIO_PERMISSION = "\u9700\u8981\u5f55\u97f3\u6743\u9650"
private const val TEXT_PERMISSION_HINT = "\u6388\u6743\u540e\u5373\u53ef\u5f00\u59cb\u9f3e\u58f0\u76d1\u6d4b"
private const val TEXT_GRANT_AUDIO = "\u6388\u6743\u5f55\u97f3"
private const val TEXT_GRANT_NOTIFICATION = "\u6388\u6743\u901a\u77e5"
private const val TEXT_NOTIFICATION_HINT = "\u5df2\u5f00\u59cb\u76d1\u6d4b\uff0c\u6388\u6743\u901a\u77e5\u540e\u53ef\u5728\u901a\u77e5\u680f\u5feb\u901f\u7ed3\u675f"
