package com.app.wordlearn.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.wordlearn.data.backup.BackupRepository
import com.app.wordlearn.domain.model.Settings
import com.app.wordlearn.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Yedek işlemi sırasında / sonrasında kullanıcıya gösterilecek geri bildirim. */
sealed class BackupUiEvent {
    data object Idle : BackupUiEvent()
    data object InProgress : BackupUiEvent()
    data class ExportSuccess(val wordCount: Int) : BackupUiEvent()
    /** Import başarılı — [needsRestart] true ise Activity yeniden başlatılmalı. */
    data class ImportSuccess(val wordCount: Int, val needsRestart: Boolean = true) : BackupUiEvent()
    data class Error(val message: String) : BackupUiEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _backupEvent = MutableStateFlow<BackupUiEvent>(BackupUiEvent.Idle)
    val backupEvent: StateFlow<BackupUiEvent> = _backupEvent.asStateFlow()

    fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            _settings.value = settingsRepository.getSettings()
        }
    }

    fun updateDailyWordCount(count: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setDailyNewWordCount(count)
            _settings.value = _settings.value.copy(dailyNewWordCount = count)
        }
    }

    fun updateUserLevel(level: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setUserLevel(level)
            _settings.value = _settings.value.copy(userLevel = level)
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = _settings.value.copy(
                displayName = name,
                updatedAt = System.currentTimeMillis()
            )
            settingsRepository.saveSettings(updated)
            _settings.value = updated
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            _backupEvent.value = BackupUiEvent.InProgress
            backupRepository.exportTo(uri)
                .onSuccess { _backupEvent.value = BackupUiEvent.ExportSuccess(it) }
                .onFailure { _backupEvent.value = BackupUiEvent.Error(it.message ?: "Dışa aktarma başarısız") }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            _backupEvent.value = BackupUiEvent.InProgress
            backupRepository.importFrom(uri)
                .onSuccess {
                    _backupEvent.value = BackupUiEvent.ImportSuccess(it)
                    // Ayarlar restore edilmiş olabilir — bellekteki kopyayı tazele.
                    loadSettings()
                }
                .onFailure { _backupEvent.value = BackupUiEvent.Error(it.message ?: "İçe aktarma başarısız") }
        }
    }

    fun clearBackupEvent() {
        _backupEvent.value = BackupUiEvent.Idle
    }
}
