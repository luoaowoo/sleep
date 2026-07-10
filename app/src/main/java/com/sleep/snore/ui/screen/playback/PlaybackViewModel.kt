package com.sleep.snore.ui.screen.playback

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep.snore.data.db.entity.SnoreEventEntity
import com.sleep.snore.data.repository.SleepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val events: StateFlow<List<SnoreEventEntity>> = repository.getAllEvents()
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
                _currentlyPlayingEventId.value = event.id
                setOnPreparedListener { preparedPlayer ->
                    if (mediaPlayer === preparedPlayer) {
                        preparedPlayer.start()
                    } else {
                        preparedPlayer.release()
                    }
                }
                setOnCompletionListener { completedPlayer ->
                    if (mediaPlayer === completedPlayer) {
                        stopPlayback()
                    } else {
                        completedPlayer.release()
                    }
                }
                setOnErrorListener { errorPlayer, _, _ ->
                    if (mediaPlayer === errorPlayer) {
                        _playbackError.value = "无法播放该片段"
                        stopPlayback()
                    } else {
                        errorPlayer.release()
                    }
                    true
                }
                prepareAsync()
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
        val player = mediaPlayer ?: run {
            _currentlyPlayingEventId.value = null
            return
        }
        mediaPlayer = null
        _currentlyPlayingEventId.value = null
        runCatching {
            player.setOnPreparedListener(null)
            player.setOnCompletionListener(null)
            player.setOnErrorListener(null)
            if (player.isPlaying) player.stop()
        }
        runCatching { player.release() }
    }

    override fun onCleared() {
        stopPlayback()
        super.onCleared()
    }
}
