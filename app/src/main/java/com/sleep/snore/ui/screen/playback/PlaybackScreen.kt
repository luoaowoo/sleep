package com.sleep.snore.ui.screen.playback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.sleep.snore.ui.theme.HeroCardShape
import com.sleep.snore.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun PlaybackScreen(
    navController: NavHostController,
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val currentlyPlayingEventId by viewModel.currentlyPlayingEventId.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val grouped = events.groupBy {
        SimpleDateFormat("M月d日", Locale.CHINESE).format(Date(it.startTimestamp))
    }

    LaunchedEffect(playbackError) {
        playbackError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearPlaybackError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("鼾声回放") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (events.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无鼾声片段", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                grouped.forEach { (date, dayEvents) ->
                    item {
                        Text(date, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = Spacing.sm))
                    }
                    items(dayEvents) { event ->
                        val isPlaying = currentlyPlayingEventId == event.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.togglePlayback(event) },
                            shape = HeroCardShape
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.startTimestamp)),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "${String.format(Locale.getDefault(), "%.0f", event.peakDb)}dB · ${String.format(Locale.getDefault(), "%.0f", event.dominantFreq)}Hz · ${event.aiTypeLabel} · ${String.format(Locale.getDefault(), "%.1f", event.durationMs / 1000.0)}s",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    if (isPlaying) "停止" else "播放",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
