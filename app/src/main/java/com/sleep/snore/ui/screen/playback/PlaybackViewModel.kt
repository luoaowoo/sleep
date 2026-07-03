package com.sleep.snore.ui.screen.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.db.entity.SnoreEventEntity
import com.sleep.snore.data.repository.SleepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    repository: SleepRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val events: StateFlow<List<SnoreEventEntity>> = repository.getAllRecords()
        .mapLatest { records ->
            val latest = records.firstOrNull()
            if (latest != null) {
                repository.getEventsByRecordId(latest.id).first()
            } else emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
