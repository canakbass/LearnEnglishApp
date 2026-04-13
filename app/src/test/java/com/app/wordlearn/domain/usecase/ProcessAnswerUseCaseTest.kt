package com.app.wordlearn.domain.usecase

import com.app.wordlearn.domain.model.WordProgress
import com.app.wordlearn.domain.repository.ProgressRepository
import com.app.wordlearn.domain.repository.SessionRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProcessAnswerUseCaseTest {

    private lateinit var progressRepository: ProgressRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var useCase: ProcessAnswerUseCase

    @Before
    fun setup() {
        progressRepository = mockk(relaxed = true)
        sessionRepository = mockk(relaxed = true)
        useCase = ProcessAnswerUseCase(progressRepository, sessionRepository)
    }

    @Test
    fun `dogru cevap streak arttirir`() = runTest {
        // Arrange: 3 doğru streak'i olan kelime
        val progress = WordProgress(
            progressId = 1, wordId = 1, correctStreak = 3,
            reviewStage = 0, totalCorrect = 3, totalAttempts = 3
        )
        coEvery { progressRepository.getProgress(1) } returns progress

        // Act
        val result = useCase.execute(1, 1, "elma", "elma")

        // Assert
        assertTrue(result.isCorrect)
        assertEquals(4, result.newStreak)
        assertEquals(0, result.newStage) // Stage değişmemeli
    }

    @Test
    fun `alti dogru ust uste stage atlatir ve streak sifirlar`() = runTest {
        // Arrange: 5 doğru streak'i olan kelime (6. doğru stage atlayacak)
        val progress = WordProgress(
            progressId = 1, wordId = 1, correctStreak = 5,
            reviewStage = 0, totalCorrect = 5, totalAttempts = 5
        )
        coEvery { progressRepository.getProgress(1) } returns progress

        // Act
        val result = useCase.execute(1, 1, "elma", "elma")

        // Assert
        assertTrue(result.isCorrect)
        assertEquals(0, result.newStreak) // Streak sıfırlanmalı
        assertEquals(1, result.newStage) // Stage 0 -> 1
    }

    @Test
    fun `yanlis cevap streak ve stage sifirlar`() = runTest {
        // Arrange: 4 doğru streak'i ve stage 2'de olan kelime
        val progress = WordProgress(
            progressId = 1, wordId = 1, correctStreak = 4,
            reviewStage = 2, totalCorrect = 16, totalAttempts = 20
        )
        coEvery { progressRepository.getProgress(1) } returns progress

        // Act
        val result = useCase.execute(1, 1, "yanlış", "doğru")

        // Assert
        assertFalse(result.isCorrect)
        assertEquals(0, result.newStreak)  // Streak sıfırlanmalı
        assertEquals(0, result.newStage)   // Stage sıfırlanmalı
    }

    @Test
    fun `stage 5 ulasinca isLearned true olur`() = runTest {
        // Arrange: Stage 4, 5. streak (6. doğruda stage 5'e çıkacak)
        val progress = WordProgress(
            progressId = 1, wordId = 1, correctStreak = 5,
            reviewStage = 4, totalCorrect = 29, totalAttempts = 29
        )
        coEvery { progressRepository.getProgress(1) } returns progress

        // Act
        val result = useCase.execute(1, 1, "elma", "elma")

        // Assert
        assertTrue(result.isCorrect)
        assertEquals(5, result.newStage) // Stage 5 = öğrenildi

        // İlerleme kaydedilmiş mi kontrol et
        coVerify {
            progressRepository.updateProgress(match { it.isLearned })
        }
    }

    @Test
    fun `cevap sessionRepository a kaydedilir`() = runTest {
        val progress = WordProgress(progressId = 1, wordId = 1)
        coEvery { progressRepository.getProgress(1) } returns progress

        useCase.execute(1, 5, "elma", "elma")

        coVerify { sessionRepository.saveAnswer(match { it.sessionId == 5 }) }
    }

    @Test
    fun `progress yoksa yeni progress olusturulur`() = runTest {
        coEvery { progressRepository.getProgress(1) } returns null

        val result = useCase.execute(1, 1, "elma", "elma")

        assertTrue(result.isCorrect)
        assertEquals(1, result.newStreak)
    }
}
