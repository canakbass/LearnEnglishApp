package com.app.wordlearn.domain.usecase

import com.app.wordlearn.domain.model.Word
import com.app.wordlearn.domain.model.WordProgress
import com.app.wordlearn.domain.repository.ProgressRepository
import com.app.wordlearn.domain.repository.WordRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SyncWordsUseCaseTest {

    private lateinit var wordRepository: WordRepository
    private lateinit var progressRepository: ProgressRepository
    private lateinit var useCase: SyncWordsUseCase

    @Before
    fun setup() {
        wordRepository = mockk(relaxed = true)
        progressRepository = mockk(relaxed = true)
        useCase = SyncWordsUseCase(wordRepository, progressRepository)
    }

    @Test
    fun `ilk acilista sistem kelimelerini yukler`() = runTest {
        val systemWords = listOf(
            Word(wordId = 1, engWord = "apple", turWord = "elma"),
            Word(wordId = 2, engWord = "book", turWord = "kitap")
        )

        coEvery { wordRepository.getWordCount() } returns 0
        coEvery { wordRepository.getAllWords() } returns systemWords
        coEvery { progressRepository.getProgress(any()) } returns null

        useCase.execute(systemWords)

        coVerify { wordRepository.insertWords(systemWords) }
        coVerify(exactly = 2) { progressRepository.createProgress(any()) }
    }

    @Test
    fun `kelimeler varsa tekrar yuklemez`() = runTest {
        val words = listOf(Word(wordId = 1, engWord = "apple", turWord = "elma"))

        coEvery { wordRepository.getWordCount() } returns 50
        coEvery { wordRepository.getAllWords() } returns words
        coEvery { progressRepository.getProgress(1) } returns WordProgress(wordId = 1)

        useCase.execute(words)

        coVerify(exactly = 0) { wordRepository.insertWords(any()) }
        coVerify(exactly = 0) { progressRepository.createProgress(any()) }
    }

    @Test
    fun `progress eksik olan kelimeler icin progress olusturur`() = runTest {
        val words = listOf(
            Word(wordId = 1, engWord = "apple", turWord = "elma"),
            Word(wordId = 2, engWord = "book", turWord = "kitap")
        )

        coEvery { wordRepository.getWordCount() } returns 2
        coEvery { wordRepository.getAllWords() } returns words
        coEvery { progressRepository.getProgress(1) } returns WordProgress(wordId = 1)
        coEvery { progressRepository.getProgress(2) } returns null

        useCase.execute(words)

        coVerify(exactly = 1) { progressRepository.createProgress(match { it.wordId == 2 }) }
    }
}
