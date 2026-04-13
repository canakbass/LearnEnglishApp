package com.app.wordlearn.presentation.wordchain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.wordlearn.domain.model.ChainResult
import com.app.wordlearn.domain.usecase.WordChainUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class WordChainUiState {
    data object Idle : WordChainUiState()
    data object Loading : WordChainUiState()
    data class Success(val result: ChainResult) : WordChainUiState()
    data class Error(val message: String) : WordChainUiState()
}

@HiltViewModel
class WordChainViewModel @Inject constructor(
    private val wordChainUseCase: WordChainUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<WordChainUiState>(WordChainUiState.Idle)
    val uiState: StateFlow<WordChainUiState> = _uiState.asStateFlow()

    fun generateChain(words: List<String>? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = WordChainUiState.Loading
            try {
                val result = wordChainUseCase.execute(words)
                _uiState.value = WordChainUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = WordChainUiState.Error(
                    e.message ?: "Bir hata oluştu"
                )
            }
        }
    }
}
