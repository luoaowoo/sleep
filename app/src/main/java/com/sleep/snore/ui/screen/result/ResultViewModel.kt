package com.sleep.snore.ui.screen.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep.snore.data.db.entity.SleepRecordEntity
import com.sleep.snore.data.repository.SleepRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: SleepRepository
) : ViewModel() {

    private val _record = MutableStateFlow<SleepRecordEntity?>(null)
    val record: StateFlow<SleepRecordEntity?> = _record

    fun loadRecord(recordId: Long) {
        viewModelScope.launch {
            _record.value = repository.getRecordById(recordId)
        }
    }
}
