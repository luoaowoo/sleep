package com.sleep.snore.ui.screen.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleep.snore.domain.DataExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exporter: DataExporter
) : ViewModel() {

    private val _exportFile = MutableStateFlow<File?>(null)
    val exportFile: StateFlow<File?> = _exportFile

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage

    fun exportRecords() {
        viewModelScope.launch {
            _isExporting.value = true
            runCatching {
                exporter.exportRecordsCsv()
            }.onSuccess { file ->
                if (file == null) {
                    _exportMessage.value = "暂无可导出的睡眠记录"
                } else {
                    _exportFile.value = file
                }
            }.onFailure {
                _exportMessage.value = "导出失败，请稍后重试"
            }
            _isExporting.value = false
        }
    }

    fun exportEvents() {
        viewModelScope.launch {
            _isExporting.value = true
            runCatching {
                exporter.exportAllEventsCsv()
            }.onSuccess { file ->
                if (file == null) {
                    _exportMessage.value = "暂无可导出的鼾声片段"
                } else {
                    _exportFile.value = file
                }
            }.onFailure {
                _exportMessage.value = "导出失败，请稍后重试"
            }
            _isExporting.value = false
        }
    }

    fun clearExportMessage() {
        _exportMessage.value = null
    }

    fun clearExportFile() {
        _exportFile.value = null
    }
}
