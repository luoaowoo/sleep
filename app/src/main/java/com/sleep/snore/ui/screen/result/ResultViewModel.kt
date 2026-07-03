package com.sleep.snore.ui.screen.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.db.entity.SnoreEventEntity
import com.sleep.snore.data.repository.SleepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: SleepRepository
) : ViewModel() {

    private val _record = MutableStateFlow<SleepRecordEntity?>(null)
    val record: StateFlow<SleepRecordEntity?> = _record

    private val _events = MutableStateFlow<List<SnoreEventEntity>>(emptyList())
    val events: StateFlow<List<SnoreEventEntity>> = _events

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _deleteCompleted = MutableStateFlow(false)
    val deleteCompleted: StateFlow<Boolean> = _deleteCompleted.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    private var eventsJob: Job? = null

    fun loadRecord(recordId: Long) {
        _record.value = null
        _events.value = emptyList()
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                repository.getRecordById(recordId)
            }.onSuccess { record ->
                _record.value = record
            }.onFailure {
                _record.value = null
            }
            _isLoading.value = false
        }
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            repository.getEventsByRecordId(recordId).collect { eventList ->
                _events.value = eventList
            }
        }
    }

    fun deleteCurrentRecord() {
        val recordId = _record.value?.id ?: return
        viewModelScope.launch {
            runCatching {
                repository.deleteRecordWithAudio(recordId)
            }.onSuccess {
                _deleteCompleted.value = true
            }.onFailure {
                _deleteError.value = "删除失败，请稍后重试"
            }
        }
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }
}
