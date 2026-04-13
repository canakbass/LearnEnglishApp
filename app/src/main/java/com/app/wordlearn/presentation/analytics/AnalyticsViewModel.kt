package com.app.wordlearn.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.wordlearn.domain.model.AnalyticsData
import com.app.wordlearn.domain.usecase.GetAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AnalyticsUiState {
    data object Loading : AnalyticsUiState()
    data class Success(val data: AnalyticsData) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getAnalyticsUseCase: GetAnalyticsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    fun loadAnalytics() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = AnalyticsUiState.Loading
            try {
                val data = getAnalyticsUseCase.execute()
                _uiState.value = AnalyticsUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = AnalyticsUiState.Error(
                    e.message ?: "Analiz yüklenirken hata oluştu"
                )
            }
        }
    }
}
