package com.app.wordlearn.presentation.wordle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.wordlearn.domain.model.WordleGameState
import com.app.wordlearn.domain.usecase.WordleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WordleViewModel @Inject constructor(
    private val wordleUseCase: WordleUseCase
) : ViewModel() {

    private val _gameState = MutableStateFlow(WordleGameState())
    val gameState: StateFlow<WordleGameState> = _gameState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun startNewGame() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _gameState.value = wordleUseCase.startNewGame()
            _isLoading.value = false
        }
    }

    fun onKeyPress(char: Char) {
        val current = _gameState.value
        if (current.isGameOver) return
        if (current.currentGuess.length >= 5) return

        _gameState.value = current.copy(
            currentGuess = current.currentGuess + char.uppercaseChar()
        )
    }

    fun onBackspace() {
        val current = _gameState.value
        if (current.isGameOver) return
        if (current.currentGuess.isEmpty()) return

        _gameState.value = current.copy(
            currentGuess = current.currentGuess.dropLast(1)
        )
    }

    fun onSubmit() {
        val current = _gameState.value
        if (current.isGameOver) return
        if (current.currentGuess.length != 5) return

        _gameState.value = wordleUseCase.submitGuess(current, current.currentGuess)
    }
}
