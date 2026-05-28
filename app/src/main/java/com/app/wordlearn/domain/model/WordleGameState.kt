package com.app.wordlearn.domain.model

data class WordleGameState(
    val targetWord: String = "",
    val attempts: List<String> = emptyList(),
    val maxAttempts: Int = 6,
    val currentGuess: String = "",
    val guessResults: List<List<LetterResult>> = emptyList(),
    val keyboardState: Map<Char, LetterResult> = emptyMap(),
    val isGameOver: Boolean = false,
    val isWon: Boolean = false,
    /**
     * Hedef kelime "öğrenilmiş kelimeler" havuzundan değil, fallback olarak
     * tüm sistem havuzundan seçildiyse `true`. Wordle ekranı kullanıcıya
     * küçük bir bilgilendirme yazısı gösterir, oyun normal çalışmaya devam eder.
     */
    val isFallback: Boolean = false
)

enum class LetterResult {
    CORRECT,
    PRESENT,
    ABSENT,
    UNUSED
}
