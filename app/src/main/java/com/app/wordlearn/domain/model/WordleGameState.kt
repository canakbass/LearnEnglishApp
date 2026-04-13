package com.app.wordlearn.domain.model

data class WordleGameState(
    val targetWord: String = "",
    val attempts: List<String> = emptyList(),
    val maxAttempts: Int = 6,
    val currentGuess: String = "",
    val guessResults: List<List<LetterResult>> = emptyList(),
    val keyboardState: Map<Char, LetterResult> = emptyMap(),
    val isGameOver: Boolean = false,
    val isWon: Boolean = false
)

enum class LetterResult {
    CORRECT,
    PRESENT,
    ABSENT,
    UNUSED
}
