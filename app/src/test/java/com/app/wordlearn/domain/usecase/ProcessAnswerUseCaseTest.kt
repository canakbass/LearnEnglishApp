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

    // ---- BUG #4: Bugün zaten cevaplanmış kelime için duplicate answer atılmamalı ----
    @Test
    fun `bugun cevaplanmis kelime icin duplicate answer kaydedilmez`() = runTest {
        val now = System.currentTimeMillis()
        val startOfDay = startOfDayFor(now)
        val progress = WordProgress(
            progressId = 1, wordId = 1, correctStreak = 1,
            reviewStage = 0, totalCorrect = 1, totalAttempts = 1,
            lastAnsweredDate = startOfDay + 1000L // bugün cevaplandı
        )
        coEvery { progressRepository.getProgress(1) } returns progress

        useCase.execute(1, 1, "elma", "elma")

        // İstatistik güncellenmemeli (mevcut davranış)
        coVerify(exactly = 0) { progressRepository.updateProgress(any()) }
        // ANSWER DA KAYDEDİLMEMELİ — score çift sayım önleme
        coVerify(exactly = 0) { sessionRepository.saveAnswer(any()) }
    }

    private fun startOfDayFor(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    // ============================================================================
    // SPACED REPETITION white-box: stage geçişleri, yarınki review zamanlaması
    // ============================================================================
    //
    // Algoritma (Constants.STREAK_THRESHOLD = 6):
    //   stage 0 → 1 gün, 1 → 7 gün, 2 → 30 gün, 3 → 90 gün, 4 → 180 gün, 5+ → sonsuz
    //   6 doğru üst üste → stage atlar; yanlış → stage 0'a reset, +1 gün
    // ============================================================================

    private val DAY = com.app.wordlearn.domain.util.Constants.DAY_IN_MS

    @Test
    fun `stage 0 da ilk dogru 1 gun sonra tekrar`() = runTest {
        val now = System.currentTimeMillis()
        val capture = slot<WordProgress>()
        coEvery { progressRepository.getProgress(1) } returns WordProgress(
            progressId = 1, wordId = 1, correctStreak = 0, reviewStage = 0,
            totalCorrect = 0, totalAttempts = 0
        )
        coEvery { progressRepository.updateProgress(capture(capture)) } just Runs

        useCase.execute(1, 1, "elma", "elma")

        val updated = capture.captured
        val daysUntilReview = (updated.nextReviewDate - now) / DAY
        // Stage 0'da 1 doğru: yine stage 0, nextReview ~1 gün sonra
        assertEquals(0, updated.reviewStage)
        assertEquals(1, updated.correctStreak)
        assertTrue("Stage 0'da 1 gün sonra tekrar bekleniyor, gerçek=${daysUntilReview}",
            daysUntilReview in 0..1)
    }

    @Test
    fun `stage 1 e gecince yarinki kelime havuzunda olmaz (7 gun sonra due)`() = runTest {
        val now = System.currentTimeMillis()
        val capture = slot<WordProgress>()
        // 5 doğru streak → 6. doğru stage atlatır
        coEvery { progressRepository.getProgress(1) } returns WordProgress(
            progressId = 1, wordId = 1, correctStreak = 5, reviewStage = 0,
            totalCorrect = 5, totalAttempts = 5
        )
        coEvery { progressRepository.updateProgress(capture(capture)) } just Runs

        useCase.execute(1, 1, "elma", "elma")

        val updated = capture.captured
        val daysUntilReview = (updated.nextReviewDate - now) / DAY
        assertEquals(1, updated.reviewStage)
        assertEquals(0, updated.correctStreak)
        assertFalse("Stage 1'e atlayan kelime yarın gösterilmemeli", updated.isLearned)
        assertTrue("Stage 1: ~7 gün sonra due bekleniyor, gerçek=$daysUntilReview",
            daysUntilReview in 6..7)
    }

    @Test
    fun `stage 2 ye gecen kelime 30 gun sonra due`() = runTest {
        val now = System.currentTimeMillis()
        val capture = slot<WordProgress>()
        coEvery { progressRepository.getProgress(1) } returns WordProgress(
            progressId = 1, wordId = 1, correctStreak = 5, reviewStage = 1,
            totalCorrect = 11, totalAttempts = 11
        )
        coEvery { progressRepository.updateProgress(capture(capture)) } just Runs

        useCase.execute(1, 1, "elma", "elma")

        val updated = capture.captured
        val daysUntilReview = (updated.nextReviewDate - now) / DAY
        assertEquals(2, updated.reviewStage)
        assertTrue("Stage 2: ~30 gün sonra due bekleniyor, gerçek=$daysUntilReview",
            daysUntilReview in 29..30)
    }

    @Test
    fun `stage 3 ten 4 e gecen kelime 180 gun sonra due`() = runTest {
        val now = System.currentTimeMillis()
        val capture = slot<WordProgress>()
        coEvery { progressRepository.getProgress(1) } returns WordProgress(
            progressId = 1, wordId = 1, correctStreak = 5, reviewStage = 3,
            totalCorrect = 23, totalAttempts = 23
        )
        coEvery { progressRepository.updateProgress(capture(capture)) } just Runs

        useCase.execute(1, 1, "elma", "elma")

        val updated = capture.captured
        val daysUntilReview = (updated.nextReviewDate - now) / DAY
        assertEquals(4, updated.reviewStage)
        assertFalse(updated.isLearned)
        assertTrue("Stage 4: ~180 gün sonra due, gerçek=$daysUntilReview",
            daysUntilReview in 179..180)
    }

    @Test
    fun `stage 4 ten 5 e gecen kelime 1 yil sonra tekrar sorulur (henuz ogrenilmedi)`() = runTest {
        val now = System.currentTimeMillis()
        val capture = slot<WordProgress>()
        coEvery { progressRepository.getProgress(1) } returns WordProgress(
            progressId = 1, wordId = 1, correctStreak = 5, reviewStage = 4,
            totalCorrect = 29, totalAttempts = 29
        )
        coEvery { progressRepository.updateProgress(capture(capture)) } just Runs

        useCase.execute(1, 1, "elma", "elma")

        val updated = capture.captured
        val daysUntilReview = (updated.nextReviewDate - now) / DAY
        assertEquals(5, updated.reviewStage)
        assertFalse("Spec'e göre 1 yıl tekrarı yapılmadan öğrenildi sayılamaz",
            updated.isLearned)
        assertTrue("Stage 5: ~365 gün sonra due, gerçek=$daysUntilReview",
            daysUntilReview in 364..365)
    }

    @Test
    fun `stage 5 ten 6 ya gecen kelime ogrenildi olur ve bir daha asla due olmaz`() = runTest {
        val capture = slot<WordProgress>()
        coEvery { progressRepository.getProgress(1) } returns WordProgress(
            progressId = 1, wordId = 1, correctStreak = 5, reviewStage = 5,
            totalCorrect = 35, totalAttempts = 35
        )
        coEvery { progressRepository.updateProgress(capture(capture)) } just Runs

        useCase.execute(1, 1, "elma", "elma")

        val updated = capture.captured
        assertEquals("Spec: 6. zaman aralığı sonrası öğrenildi havuzuna",
            6, updated.reviewStage)
        assertTrue("Stage 6 = öğrenildi", updated.isLearned)
        assertEquals("Sonsuz nextReviewDate bekleniyor", Long.MAX_VALUE, updated.nextReviewDate)
    }

    @Test
    fun `bir stage de 6 dogru cevap birikene kadar stage atlamaz`() = runTest {
        val captures = mutableListOf<WordProgress>()
        coEvery { progressRepository.updateProgress(capture(captures)) } just Runs

        var current = WordProgress(
            progressId = 1, wordId = 1, correctStreak = 0, reviewStage = 1,
            totalCorrect = 10, totalAttempts = 10
        )
        // 5 doğru cevap arka arkaya — hâlâ stage 1
        repeat(5) { i ->
            coEvery { progressRepository.getProgress(1) } returns current.copy(
                lastAnsweredDate = 0L
            )
            useCase.execute(1, 1, "elma", "elma")
            current = captures.last()
            assertEquals("$i. cevaptan sonra hâlâ stage 1 olmalı", 1, current.reviewStage)
            assertEquals(i + 1, current.correctStreak)
        }
        // 6. doğru cevap — şimdi stage 2'ye atlamalı
        coEvery { progressRepository.getProgress(1) } returns current.copy(lastAnsweredDate = 0L)
        useCase.execute(1, 1, "elma", "elma")
        val finalState = captures.last()
        assertEquals(2, finalState.reviewStage)
        assertEquals(0, finalState.correctStreak)
    }

    @Test
    fun `stage 3 te yanlis cevap stage i 0 a dusurur ve yarin tekrar`() = runTest {
        val now = System.currentTimeMillis()
        val capture = slot<WordProgress>()
        coEvery { progressRepository.getProgress(1) } returns WordProgress(
            progressId = 1, wordId = 1, correctStreak = 4, reviewStage = 3,
            totalCorrect = 18, totalAttempts = 22
        )
        coEvery { progressRepository.updateProgress(capture(capture)) } just Runs

        useCase.execute(1, 1, "yanlis", "elma")

        val updated = capture.captured
        val daysUntilReview = (updated.nextReviewDate - now) / DAY
        assertEquals("Yanlış cevap stage'i 0'a sıfırlamalı", 0, updated.reviewStage)
        assertEquals(0, updated.correctStreak)
        assertTrue("Yanlış sonrası kelime yarın tekrar sorulmalı, gerçek=$daysUntilReview",
            daysUntilReview in 0..1)
    }

    @Test
    fun `dogru cevap totalCorrect ve totalAttempts i artirir, yanlis sadece totalAttempts i`() = runTest {
        val capture = slot<WordProgress>()
        coEvery { progressRepository.updateProgress(capture(capture)) } just Runs

        // Doğru cevap
        coEvery { progressRepository.getProgress(1) } returns WordProgress(
            progressId = 1, wordId = 1, totalCorrect = 5, totalAttempts = 10
        )
        useCase.execute(1, 1, "elma", "elma")
        assertEquals(6, capture.captured.totalCorrect)
        assertEquals(11, capture.captured.totalAttempts)

        // Yanlış cevap
        coEvery { progressRepository.getProgress(1) } returns WordProgress(
            progressId = 1, wordId = 1, totalCorrect = 5, totalAttempts = 10
        )
        useCase.execute(1, 1, "yanlis", "elma")
        assertEquals("Yanlış totalCorrect'i artırmaz", 5, capture.captured.totalCorrect)
        assertEquals(11, capture.captured.totalAttempts)
    }

    @Test
    fun `stage 6 ogrenilmis kelimeye dogru cevap no-op (defansif return)`() = runTest {
        // Öğrenilmiş kelime quiz'e zaten alınmamalı; alındıysa bile defansif return yapılmalı.
        coEvery { progressRepository.getProgress(1) } returns WordProgress(
            progressId = 1, wordId = 1, reviewStage = 6, isLearned = true,
            totalCorrect = 36, totalAttempts = 36,
            nextReviewDate = Long.MAX_VALUE,
            lastAnsweredDate = startOfDayFor(System.currentTimeMillis()) + 1000L
        )

        useCase.execute(1, 1, "elma", "elma")

        // lastAnsweredDate >= startOfDay → early return, hiçbir şey yazılmaz
        coVerify(exactly = 0) { progressRepository.updateProgress(any()) }
        coVerify(exactly = 0) { sessionRepository.saveAnswer(any()) }
    }
}
