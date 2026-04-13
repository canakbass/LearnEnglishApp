package com.app.wordlearn.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.wordlearn.domain.model.Settings
import com.app.wordlearn.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

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
}
