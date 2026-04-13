package com.app.wordlearn.domain.usecase

import com.app.wordlearn.domain.model.LetterResult
import com.app.wordlearn.domain.model.WordleGameState
import com.app.wordlearn.domain.repository.ProgressRepository
import com.app.wordlearn.domain.repository.WordRepository
import javax.inject.Inject

class WordleUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val progressRepository: ProgressRepository
) {
    suspend fun startNewGame(): WordleGameState {
        // 1. Önce öğrenilen kelimelerden 5 harfli seç
        val learnedWords = progressRepository.getLearnedWords()
        val allWords = wordRepository.getAllWords()

        val learnedWordMap = learnedWords.associateBy { it.wordId }

        // Öğrenilmiş 5 harfli kelimeleri filtrele
        var fiveLetterWords = allWords.filter { word ->
            word.engWord.length == 5 && learnedWordMap.containsKey(word.wordId)
        }

        // Öğrenilen yeterli değilse tüm havuzdan seç
        if (fiveLetterWords.isEmpty()) {
            fiveLetterWords = allWords.filter { it.engWord.length == 5 }
        }

        val targetWord = if (fiveLetterWords.isNotEmpty()) {
            fiveLetterWords.random().engWord.uppercase()
        } else {
            // Fallback: havuzda 5 harfli kelime yoksa
            "BRAIN"
        }

        return WordleGameState(targetWord = targetWord)
    }

    fun submitGuess(state: WordleGameState, guess: String): WordleGameState {
        if (state.isGameOver || guess.length != 5) return state

        val upperGuess = guess.uppercase()
        val target = state.targetWord.uppercase()

        // Harf kontrolü
        val result = checkGuess(upperGuess, target)

        val newAttempts = state.attempts + upperGuess
        val newResults = state.guessResults + listOf(result)
        val isWon = upperGuess == target
        val isGameOver = isWon || newAttempts.size >= state.maxAttempts

        // Klavye durumunu güncelle
        val newKeyboardState = state.keyboardState.toMutableMap()
        upperGuess.forEachIndexed { index, char ->
            val currentState = newKeyboardState[char]
            val newState = result[index]

            // Daha iyi sonuç varsa güncelle (CORRECT > PRESENT > ABSENT)
            if (currentState == null || newState.ordinal < (currentState.ordinal)) {
                newKeyboardState[char] = newState
            }
        }

        return state.copy(
            attempts = newAttempts,
            currentGuess = "",
            guessResults = newResults,
            keyboardState = newKeyboardState,
            isGameOver = isGameOver,
            isWon = isWon
        )
    }

    fun checkGuess(guess: String, target: String): List<LetterResult> {
        val result = MutableList(5) { LetterResult.ABSENT }
        val targetChars = target.toMutableList()
        val usedInTarget = BooleanArray(5)

        // İlk geçiş: doğru yerdeki harfler (CORRECT)
        for (i in 0 until 5) {
            if (guess[i] == target[i]) {
                result[i] = LetterResult.CORRECT
                usedInTarget[i] = true
            }
        }

        // İkinci geçiş: yanlış yerdeki harfler (PRESENT)
        for (i in 0 until 5) {
            if (result[i] != LetterResult.CORRECT) {
                for (j in 0 until 5) {
                    if (!usedInTarget[j] && guess[i] == target[j]) {
                        result[i] = LetterResult.PRESENT
                        usedInTarget[j] = true
                        break
                    }
                }
            }
        }

        return result
    }
}
