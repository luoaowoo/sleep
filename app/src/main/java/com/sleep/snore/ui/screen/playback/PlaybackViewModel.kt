package com.sleep.snore.ui.screen.playback

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep.snore.data.db.entity.SnoreEventEntity
import com.sleep.snore.data.repository.SleepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    repository: SleepRepository
) : ViewModel() {

    private var mediaPlayer: MediaPlayer? = null

    private val _currentlyPlayingEventId = MutableStateFlow<Long?>(null)
    val currentlyPlayingEventId: StateFlow<Long?> = _currentlyPlayingEventId.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val events: StateFlow<List<SnoreEventEntity>> = repository.getAllRecords()
        .flatMapLatest { records ->
            if (records.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(records.map { repository.getEventsByRecordId(it.id) }) { eventLists ->
                    eventLists.flatMap { it.asIterable() }
                        .sortedByDescending { it.startTimestamp }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun togglePlayback(event: SnoreEventEntity) {
        if (_currentlyPlayingEventId.value == event.id) {
            stopPlayback()
            return
        }

        stopPlayback()

        if (event.audioFilePath.isBlank() || !File(event.audioFilePath).exists()) {
            _playbackError.value = "音频文件不存在"
            return
        }

        runCatching {
            val player = MediaPlayer()
            mediaPlayer = player
            player.apply {
                setDataSource(event.audioFilePath)
                setOnCompletionListener { stopPlayback() }
                prepare()
                start()
                _currentlyPlayingEventId.value = event.id
            }
        }.onFailure {
            _playbackError.value = "无法播放该片段"
            stopPlayback()
        }
    }

    fun clearPlaybackError() {
        _playbackError.value = null
    }

    fun stopPlayback() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        _currentlyPlayingEventId.value = null
    }

    override fun onCleared() {
        stopPlayback()
        super.onCleared()
    }
}
