package com.app.wordlearn.domain.usecase

import com.app.wordlearn.domain.model.LetterResult
import com.app.wordlearn.domain.model.WordleGameState
import com.app.wordlearn.domain.repository.ProgressRepository
import com.app.wordlearn.domain.repository.WordRepository
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WordleUseCaseTest {

    private lateinit var useCase: WordleUseCase

    @Before
    fun setup() {
        val wordRepository: WordRepository = mockk(relaxed = true)
        val progressRepository: ProgressRepository = mockk(relaxed = true)
        useCase = WordleUseCase(wordRepository, progressRepository)
    }

    @Test
    fun `dogru yerde dogru harf CORRECT dondurur`() {
        val result = useCase.checkGuess("BRAIN", "BRAIN")

        result.forEach { assertEquals(LetterResult.CORRECT, it) }
    }

    @Test
    fun `yanlis yerde dogru harf PRESENT dondurur`() {
        // B doğru yerde, R yanlış yerde vs.
        val result = useCase.checkGuess("BAINR", "BRAIN")

        assertEquals(LetterResult.CORRECT, result[0]) // B doğru yerde
        assertEquals(LetterResult.PRESENT, result[1]) // A - BRAIN'de var ama yanlış yerde
        assertEquals(LetterResult.PRESENT, result[2]) // I - BRAIN'de var ama yanlış yerde
        assertEquals(LetterResult.PRESENT, result[3]) // N - BRAIN'de var ama yanlış yerde
        assertEquals(LetterResult.PRESENT, result[4]) // R - BRAIN'de var ama yanlış yerde
    }

    @Test
    fun `olmayan harf ABSENT dondurur`() {
        val result = useCase.checkGuess("XXXXX", "BRAIN")

        result.forEach { assertEquals(LetterResult.ABSENT, it) }
    }

    @Test
    fun `karisik senaryo dogru calisiyor`() {
        // CRANE -> BRAIN: C=ABSENT, R=PRESENT, A=CORRECT, N=PRESENT, E=ABSENT
        val result = useCase.checkGuess("CRANE", "BRAIN")

        assertEquals(LetterResult.ABSENT, result[0])  // C yok
        assertEquals(LetterResult.PRESENT, result[1])  // R var ama yanlış yerde
        assertEquals(LetterResult.CORRECT, result[2])  // A doğru yerde
        assertEquals(LetterResult.PRESENT, result[3])  // N var ama yanlış yerde
        assertEquals(LetterResult.ABSENT, result[4])   // E yok
    }

    @Test
    fun `dogru tahmin oyunu kazandirir`() {
        val state = WordleGameState(targetWord = "BRAIN")

        val newState = useCase.submitGuess(state, "BRAIN")

        assertTrue(newState.isWon)
        assertTrue(newState.isGameOver)
    }

    @Test
    fun `6 yanlis tahmin oyunu kaybettirir`() {
        var state = WordleGameState(targetWord = "BRAIN")
        val guesses = listOf("CRANE", "STONE", "LIGHT", "DREAM", "QUEST", "FLAME")

        guesses.forEach { guess ->
            state = useCase.submitGuess(state, guess)
        }

        assertFalse(state.isWon)
        assertTrue(state.isGameOver)
        assertEquals(6, state.attempts.size)
    }

    @Test
    fun `5 harften farkli tahmin reddedilir`() {
        val state = WordleGameState(targetWord = "BRAIN")

        val newState = useCase.submitGuess(state, "HI")

        assertEquals(state, newState) // Değişmemeli
    }

    @Test
    fun `oyun bittikten sonra tahmin kabul edilmez`() {
        val state = WordleGameState(
            targetWord = "BRAIN",
            isGameOver = true,
            isWon = true
        )

        val newState = useCase.submitGuess(state, "OTHER")

        assertEquals(state, newState) // Değişmemeli
    }
}
