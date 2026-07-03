package com.sleep.snore.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.repository.SleepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SleepRepository
) : ViewModel() {

    val latestRecord: StateFlow<SleepRecordEntity?> = repository.getAllRecords()
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentRecords: StateFlow<List<SleepRecordEntity>> = repository.getAllRecords()
        .map { it.take(7) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
