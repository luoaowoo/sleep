package com.sleep.snore.ui.screen.playback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
@Composable
fun PlaybackScreen(
    navController: NavHostController,
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val grouped = events.groupBy {
        SimpleDateFormat("M月d日", Locale.CHINESE).format(Date(it.startTimestamp))
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("鼾声回放") })
        }
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
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { },
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
                                        "${String.format("%.0f", event.peakDb)}dB · ${event.aiTypeLabel} · ${event.durationMs / 1000.0}s",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text("▶", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
