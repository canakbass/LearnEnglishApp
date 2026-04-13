package com.app.wordlearn.domain.usecase

import com.app.wordlearn.domain.model.Word
import com.app.wordlearn.domain.model.WordProgress
import com.app.wordlearn.domain.repository.ProgressRepository
import com.app.wordlearn.domain.repository.SettingsRepository
import com.app.wordlearn.domain.repository.WordRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BuildDailyQuizUseCaseTest {

    private lateinit var wordRepository: WordRepository
    private lateinit var progressRepository: ProgressRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: BuildDailyQuizUseCase

    @Before
    fun setup() {
        wordRepository = mockk()
        progressRepository = mockk()
        settingsRepository = mockk()
        useCase = BuildDailyQuizUseCase(wordRepository, progressRepository, settingsRepository)
    }

    @Test
    fun `vadesi gelen ve yeni kelimeleri birlestirerek quiz olusturur`() = runTest {
        // Arrange: 3 vadesi gelen + 2 yeni kelime
        val dueWords = listOf(
            WordProgress(wordId = 1, correctStreak = 3, reviewStage = 1),
            WordProgress(wordId = 2, correctStreak = 1, reviewStage = 0),
            WordProgress(wordId = 3, correctStreak = 5, reviewStage = 2)
        )
        val newWords = listOf(
            WordProgress(wordId = 4, correctStreak = 0),
            WordProgress(wordId = 5, correctStreak = 0)
        )

        val words = listOf(
            Word(wordId = 1, engWord = "apple", turWord = "elma"),
            Word(wordId = 2, engWord = "book", turWord = "kitap"),
            Word(wordId = 3, engWord = "cat", turWord = "kedi"),
            Word(wordId = 4, engWord = "dog", turWord = "köpek"),
            Word(wordId = 5, engWord = "fish", turWord = "balık"),
            Word(wordId = 6, engWord = "car", turWord = "araba"),
            Word(wordId = 7, engWord = "pen", turWord = "kalem"),
            Word(wordId = 8, engWord = "sun", turWord = "güneş")
        )

        coEvery { progressRepository.getDueWords(any()) } returns dueWords
        coEvery { settingsRepository.getDailyNewWordCount() } returns 10
        coEvery { progressRepository.getNewWords(10) } returns newWords
        words.forEach { word ->
            coEvery { wordRepository.getWordById(word.wordId) } returns word
        }
        coEvery { wordRepository.getRandomWords(3, any()) } returns
                listOf(words[5], words[6], words[7])

        // Act
        val questions = useCase.execute()

        // Assert
        assertEquals(5, questions.size)
        questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertTrue(q.options.contains(q.correctAnswer))
        }
    }

    @Test
    fun `kelime havuzu bossa bos liste dondurur`() = runTest {
        coEvery { progressRepository.getDueWords(any()) } returns emptyList()
        coEvery { settingsRepository.getDailyNewWordCount() } returns 10
        coEvery { progressRepository.getNewWords(10) } returns emptyList()

        val questions = useCase.execute()

        assertTrue(questions.isEmpty())
    }

    @Test
    fun `her soru icin 4 sik olusturur`() = runTest {
        val dueWords = listOf(WordProgress(wordId = 1))
        val word = Word(wordId = 1, engWord = "test", turWord = "sınav")
        val wrongWords = listOf(
            Word(wordId = 2, engWord = "a", turWord = "x"),
            Word(wordId = 3, engWord = "b", turWord = "y"),
            Word(wordId = 4, engWord = "c", turWord = "z")
        )

        coEvery { progressRepository.getDueWords(any()) } returns dueWords
        coEvery { settingsRepository.getDailyNewWordCount() } returns 10
        coEvery { progressRepository.getNewWords(10) } returns emptyList()
        coEvery { wordRepository.getWordById(1) } returns word
        coEvery { wordRepository.getRandomWords(3, 1) } returns wrongWords

        val questions = useCase.execute()

        assertEquals(1, questions.size)
        assertEquals(4, questions[0].options.size)
        assertTrue(questions[0].options.contains("sınav"))
    }
}
