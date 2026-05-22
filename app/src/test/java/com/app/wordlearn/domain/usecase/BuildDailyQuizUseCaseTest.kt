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

/**
 * BuildDailyQuizUseCase white-box testleri.
 *
 * Yeni tek-liste seçim modelinde kullanılan ProgressRepository API:
 *  - getAllProgress() — adayların ana kaynağı
 *  - getAnsweredTodayCount(startOfDay) — practice mode kararı
 *  - createProgress / updateProgress — yan etkiler
 *
 * Eski sürümde olan getPendingNewWords / getPendingDueWords / getDueWords / getNewWords
 * artık kullanılmıyor (mock'lamak gereksiz).
 */
class BuildDailyQuizUseCaseTest {

    private lateinit var wordRepository: WordRepository
    private lateinit var progressRepository: ProgressRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: BuildDailyQuizUseCase

    private val words = listOf(
        Word(wordId = 1, engWord = "apple", turWord = "elma"),
        Word(wordId = 2, engWord = "book", turWord = "kitap"),
        Word(wordId = 3, engWord = "cat", turWord = "kedi"),
        Word(wordId = 4, engWord = "dog", turWord = "köpek"),
        Word(wordId = 5, engWord = "fish", turWord = "balık"),
        Word(wordId = 6, engWord = "car", turWord = "araba"),
        Word(wordId = 7, engWord = "pen", turWord = "kalem"),
        Word(wordId = 8, engWord = "sun", turWord = "güneş")
    )

    private val startOfDay: Long get() {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    @Before
    fun setup() {
        wordRepository = mockk()
        progressRepository = mockk()
        settingsRepository = mockk()
        useCase = BuildDailyQuizUseCase(wordRepository, progressRepository, settingsRepository)

        coEvery { settingsRepository.getEffectiveDailyCount(any()) } returns 10
        coEvery { progressRepository.getAnsweredTodayCount(any()) } returns 0
        coEvery { wordRepository.getAllWords() } returns words
        coEvery { progressRepository.createProgress(any()) } just Runs
        coEvery { progressRepository.updateProgress(any()) } just Runs
        words.forEach { word ->
            coEvery { wordRepository.getWordById(word.wordId) } returns word
        }
        coEvery { wordRepository.getRandomWords(any(), any()) } returns words.drop(2)
    }

    @Test
    fun `cevaplanmamis kelimelerden quiz olusturur`() = runTest {
        val progress = listOf(
            WordProgress(progressId = 1, wordId = 1, totalAttempts = 5, reviewStage = 1, lastAnsweredDate = startOfDay - 86_400_000L * 2),
            WordProgress(progressId = 2, wordId = 2, totalAttempts = 3, reviewStage = 0, lastAnsweredDate = startOfDay - 86_400_000L),
            WordProgress(progressId = 3, wordId = 3, totalAttempts = 0),
            WordProgress(progressId = 4, wordId = 4, totalAttempts = 0),
            WordProgress(progressId = 5, wordId = 5, totalAttempts = 0)
        )
        coEvery { progressRepository.getAllProgress() } returns progress

        val questions = useCase.execute()

        assertEquals(5, questions.size)
        // Hiç duplicate olmamalı
        assertEquals(questions.size, questions.map { it.wordId }.toSet().size)
        questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertTrue(q.options.contains(q.correctAnswer))
        }
    }

    @Test
    fun `kelime havuzu kucukse bos liste dondurur`() = runTest {
        coEvery { wordRepository.getAllWords() } returns words.take(3) // 4'ten az
        coEvery { progressRepository.getAllProgress() } returns emptyList()

        val questions = useCase.execute()

        assertTrue(questions.isEmpty())
    }

    @Test
    fun `bugun cevaplanmis kelime quiz havuzuna alinmaz`() = runTest {
        val progress = listOf(
            // Bugün cevaplandı — hard rule ile dışlanır
            WordProgress(progressId = 1, wordId = 1, totalAttempts = 1, lastAnsweredDate = startOfDay + 1000L),
            // Bugün yanlış cevaplandı
            WordProgress(progressId = 2, wordId = 2, totalAttempts = 1, lastAnsweredDate = startOfDay + 5000L),
            // Aday
            WordProgress(progressId = 3, wordId = 3, totalAttempts = 0),
            WordProgress(progressId = 4, wordId = 4, totalAttempts = 0)
        )
        coEvery { progressRepository.getAllProgress() } returns progress

        val questions = useCase.execute()

        val ids = questions.map { it.wordId }.toSet()
        assertFalse("Bugün cevaplanan wordId=1 quiz'de olmamalı", 1 in ids)
        assertFalse("Bugün cevaplanan wordId=2 quiz'de olmamalı", 2 in ids)
        assertTrue(3 in ids || 4 in ids)
    }

    @Test
    fun `ogrenilmis kelime quiz havuzuna alinmaz`() = runTest {
        val progress = listOf(
            WordProgress(progressId = 1, wordId = 1, totalAttempts = 30, isLearned = true),
            WordProgress(progressId = 2, wordId = 2, totalAttempts = 0),
            WordProgress(progressId = 3, wordId = 3, totalAttempts = 0)
        )
        coEvery { progressRepository.getAllProgress() } returns progress

        val questions = useCase.execute()

        val ids = questions.map { it.wordId }.toSet()
        assertFalse("Öğrenilmiş kelime quiz'de olmamalı", 1 in ids)
    }

    @Test
    fun `practice modda bugun cevaplanan kelimeler karisik sirayla tekrar sorulur`() = runTest {
        val today = System.currentTimeMillis()
        val answeredToday = (1..5).map { id ->
            WordProgress(progressId = id, wordId = id, totalAttempts = 1,
                lastAnsweredDate = today, lastShownDate = today)
        }
        coEvery { progressRepository.getAllProgress() } returns answeredToday
        coEvery { settingsRepository.getEffectiveDailyCount(any()) } returns 5
        coEvery { progressRepository.getAnsweredTodayCount(any()) } returns 5

        val questions = useCase.execute()

        // Practice mod davranışı: günlük yeni kelime kotasını korumak için yeni kelime YOK,
        // sadece bugün cevaplanan 5 kelime tekrar geliyor.
        val ids = questions.map { it.wordId }.toSet()
        assertEquals("Practice modda bugünkü 5 kelimenin hepsi tekrar gelmeli", 5, questions.size)
        assertEquals(setOf(1, 2, 3, 4, 5), ids)
    }

    @Test
    fun `pending kelimeler ve fresh kelimeler bir arada distinct olur`() = runTest {
        val today = System.currentTimeMillis()
        val progress = listOf(
            // Pending: bugün gösterildi, cevaplanmadı (rank 0)
            WordProgress(progressId = 1, wordId = 1, totalAttempts = 0, lastShownDate = today),
            // Due: dünden
            WordProgress(progressId = 2, wordId = 2, totalAttempts = 3, lastAnsweredDate = startOfDay - 86_400_000L,
                nextReviewDate = today - 1000L),
            // Yeni
            WordProgress(progressId = 3, wordId = 3, totalAttempts = 0),
            WordProgress(progressId = 4, wordId = 4, totalAttempts = 0),
            WordProgress(progressId = 5, wordId = 5, totalAttempts = 0)
        )
        coEvery { progressRepository.getAllProgress() } returns progress

        val questions = useCase.execute()

        val ids = questions.map { it.wordId }
        assertEquals("Hiç duplicate olmamalı", ids.size, ids.toSet().size)
        assertTrue("Pending kelime quiz'de olmalı", 1 in ids)
    }

    @Test
    fun `her soru icin 4 sik olusturur`() = runTest {
        val progress = listOf(
            WordProgress(progressId = 1, wordId = 1, totalAttempts = 2, reviewStage = 1,
                lastAnsweredDate = startOfDay - 86_400_000L)
        )
        coEvery { progressRepository.getAllProgress() } returns progress

        val questions = useCase.execute()

        assertEquals(1, questions.size)
        assertEquals(4, questions[0].options.size)
        assertTrue(questions[0].options.contains("elma"))
    }

    @Test
    fun `yeni gosterilen kelimelerin lastShownDate i isaretlenir`() = runTest {
        val progress = listOf(
            WordProgress(progressId = 1, wordId = 4, totalAttempts = 0, lastShownDate = 0L),
            WordProgress(progressId = 2, wordId = 5, totalAttempts = 0, lastShownDate = 0L)
        )
        coEvery { progressRepository.getAllProgress() } returns progress

        useCase.execute()

        // Her iki yeni kelime için updateProgress çağrılmalı (lastShownDate set)
        coVerify(atLeast = 2) { progressRepository.updateProgress(any()) }
    }

    // ============================================================================
    // SPACED REPETITION havuza dahil etme — yarınki kelime havuzu
    // ============================================================================

    private val DAY = com.app.wordlearn.domain.util.Constants.DAY_IN_MS

    @Test
    fun `1 gun once dogru bilinen kelime bugun due olarak havuza girer`() = runTest {
        val today = System.currentTimeMillis()
        val yesterday = startOfDay - DAY
        val progress = listOf(
            // Stage 0'da 1 doğru cevap dün → nextReviewDate = dün + 1 gün = bugün
            WordProgress(progressId = 1, wordId = 1, totalAttempts = 1, totalCorrect = 1,
                correctStreak = 1, reviewStage = 0,
                lastAnsweredDate = yesterday + 1000L,
                nextReviewDate = today - 1000L) // bugün due
        )
        coEvery { progressRepository.getAllProgress() } returns progress

        val questions = useCase.execute()

        assertTrue("Dün cevaplanan ve bugün due olan kelime havuzda olmalı",
            questions.any { it.wordId == 1 })
    }

    @Test
    fun `7 gun once stage 1 e gecen kelime bugun tekrar sorulur`() = runTest {
        val today = System.currentTimeMillis()
        val sevenDaysAgo = today - 7 * DAY
        val progress = listOf(
            WordProgress(progressId = 1, wordId = 1, totalAttempts = 6, totalCorrect = 6,
                correctStreak = 0, reviewStage = 1,
                lastAnsweredDate = sevenDaysAgo,
                nextReviewDate = today - 1000L) // tam 7 gün sonra due
        )
        coEvery { progressRepository.getAllProgress() } returns progress

        val questions = useCase.execute()

        assertTrue("Stage 1'den 7 gün sonra kelime due olmalı",
            questions.any { it.wordId == 1 })
    }

    @Test
    fun `next review tarihi gelecekte olan kelime bugun sorulmaz`() = runTest {
        val today = System.currentTimeMillis()
        val progress = listOf(
            // Henüz due değil — 5 gün sonra
            WordProgress(progressId = 1, wordId = 1, totalAttempts = 2, totalCorrect = 2,
                correctStreak = 2, reviewStage = 0,
                lastAnsweredDate = today - DAY,
                nextReviewDate = today + 5 * DAY),
            WordProgress(progressId = 2, wordId = 2, totalAttempts = 0) // bu seçilebilir
        )
        coEvery { progressRepository.getAllProgress() } returns progress

        val questions = useCase.execute()

        assertFalse("Gelecek tarihli kelime havuzda olmamalı",
            questions.any { it.wordId == 1 })
    }

    @Test
    fun `stage 5 ogrenilmis kelime asla quiz havuzuna girmez`() = runTest {
        val today = System.currentTimeMillis()
        val progress = listOf(
            WordProgress(progressId = 1, wordId = 1, totalAttempts = 30, totalCorrect = 30,
                reviewStage = 5, isLearned = true,
                lastAnsweredDate = today - 365 * DAY,
                nextReviewDate = Long.MAX_VALUE),
            WordProgress(progressId = 2, wordId = 2, totalAttempts = 0)
        )
        coEvery { progressRepository.getAllProgress() } returns progress

        val questions = useCase.execute()

        assertFalse("Öğrenilmiş kelime asla havuza girmemeli",
            questions.any { it.wordId == 1 })
    }

    @Test
    fun `pending devam kelimeleri her zaman ilk siralanir (rank 0)`() = runTest {
        val today = System.currentTimeMillis()
        val progress = listOf(
            // Pending devam — bugün gösterildi, cevaplanmadı (rank 0)
            WordProgress(progressId = 1, wordId = 1, totalAttempts = 0,
                lastShownDate = today, lastAnsweredDate = 0L),
            // Due dünden (rank 1)
            WordProgress(progressId = 2, wordId = 2, totalAttempts = 3,
                lastAnsweredDate = today - 2 * DAY,
                nextReviewDate = today - 1000L),
            // Yeni (rank 2)
            WordProgress(progressId = 3, wordId = 3, totalAttempts = 0)
        )
        coEvery { progressRepository.getAllProgress() } returns progress
        coEvery { settingsRepository.getEffectiveDailyCount(any()) } returns 1

        val questions = useCase.execute()

        assertEquals(1, questions.size)
        assertEquals("Pending devam kelimesi (rank 0) önce seçilmeli", 1, questions[0].wordId)
    }

    // ============================================================================
    // RESTORE → QUIZ entegrasyonu: import edilmiş progress'in havuza etkisi
    // ============================================================================

    @Test
    fun `import sonrasi restore edilmis stage 2 kelimesi 30 gun sonra havuza girer`() = runTest {
        val today = System.currentTimeMillis()
        val thirtyDaysAgo = today - 30 * DAY
        // Backup'tan gelmiş: 30 gün önce stage 2'ye geçen, bugün due olan kelime
        val progress = listOf(
            WordProgress(progressId = 1, wordId = 1, totalAttempts = 12, totalCorrect = 12,
                reviewStage = 2, correctStreak = 0,
                lastAnsweredDate = thirtyDaysAgo,
                nextReviewDate = today - 1000L) // bugün due
        )
        coEvery { progressRepository.getAllProgress() } returns progress

        val questions = useCase.execute()

        assertTrue("Restore edilmiş stage 2 kelime, due tarihinde havuzda olmalı",
            questions.any { it.wordId == 1 })
    }

    @Test
    fun `import sonrasi henuz vadesi gelmemis stage 3 kelimesi havuzda gorunmez`() = runTest {
        val today = System.currentTimeMillis()
        val progress = listOf(
            // Stage 3 — 90 gün sonra due; daha 60 gün var
            WordProgress(progressId = 1, wordId = 1, totalAttempts = 18, totalCorrect = 18,
                reviewStage = 3, lastAnsweredDate = today - 30 * DAY,
                nextReviewDate = today + 60 * DAY),
            WordProgress(progressId = 2, wordId = 2, totalAttempts = 0) // sadece bu seçilebilir
        )
        coEvery { progressRepository.getAllProgress() } returns progress

        val questions = useCase.execute()

        val ids = questions.map { it.wordId }.toSet()
        assertFalse("Vade gelmeden stage 3 kelime havuzda olmamalı", 1 in ids)
        assertTrue(2 in ids)
    }

    @Test
    fun `import sonrasi ogrenilmis kelimeler quiz havuzunda asla yer almaz`() = runTest {
        val today = System.currentTimeMillis()
        // Backup'tan 50 öğrenilmiş kelime + 5 öğrenilmemiş kelime gelmiş
        val learned = (1..5).map { id ->
            WordProgress(progressId = id, wordId = id, totalAttempts = 30, totalCorrect = 30,
                reviewStage = 5, isLearned = true,
                lastAnsweredDate = today - 100 * DAY,
                nextReviewDate = Long.MAX_VALUE)
        }
        val unlearned = (6..8).map { id ->
            WordProgress(progressId = id, wordId = id, totalAttempts = 0)
        }
        coEvery { progressRepository.getAllProgress() } returns (learned + unlearned)

        val questions = useCase.execute()

        learned.forEach { p ->
            assertFalse("Öğrenilmiş kelime ${p.wordId} asla havuzda olmamalı",
                questions.any { it.wordId == p.wordId })
        }
    }

    // ============================================================================
    // GÜNLÜK QUOTA BİTİŞİ: practice mode davranışı
    // ============================================================================

    @Test
    fun `gunluk kota bittikten sonra practice modda YENI kelime gelmez sadece bugunkuler tekrar`() = runTest {
        val today = System.currentTimeMillis()
        // Bugün 5 kelime cevaplandı (quota = 5), 3 öğrenilmemiş kelime havuzda kaldı
        val answeredToday = (1..5).map { id ->
            WordProgress(progressId = id, wordId = id, totalAttempts = 1,
                lastAnsweredDate = today, lastShownDate = today)
        }
        val freshUnlearned = (6..8).map { id ->
            WordProgress(progressId = id, wordId = id, totalAttempts = 0)
        }
        coEvery { progressRepository.getAllProgress() } returns (answeredToday + freshUnlearned)
        coEvery { settingsRepository.getEffectiveDailyCount(any()) } returns 5
        coEvery { progressRepository.getAnsweredTodayCount(any()) } returns 5

        val questions = useCase.execute()

        val ids = questions.map { it.wordId }.toSet()
        // Günlük kelime kotasını korumak için yeni kelime ASLA gelmez
        (6..8).forEach { id ->
            assertFalse("Practice modda yeni kelime gelmemeli (kotayı korur)", id in ids)
        }
        // Bugün cevaplanan 5 kelime tekrar sorulur (karışık sırayla)
        assertEquals(5, questions.size)
        assertEquals(setOf(1, 2, 3, 4, 5), ids)
    }

    @Test
    fun `kota dolu ama bugun hic cevap yoksa (edge) quiz bos`() = runTest {
        // Imkansız edge case: kota = 0
        coEvery { progressRepository.getAllProgress() } returns emptyList()
        coEvery { settingsRepository.getEffectiveDailyCount(any()) } returns 0
        coEvery { progressRepository.getAnsweredTodayCount(any()) } returns 0

        val questions = useCase.execute()

        assertTrue("Hiç kelime yok → quiz boş", questions.isEmpty())
    }

    @Test
    fun `practice modda gelecek tarihli kelimeler bile havuza girmez`() = runTest {
        val today = System.currentTimeMillis()
        val answeredToday = (1..5).map { id ->
            WordProgress(progressId = id, wordId = id, totalAttempts = 1,
                lastAnsweredDate = today, lastShownDate = today)
        }
        // Vadesi ileride olan stage 1 kelimeler — practice modda bunlar da gelmemeli
        val futureDue = (6..8).map { id ->
            WordProgress(progressId = id, wordId = id, totalAttempts = 6,
                reviewStage = 1, lastAnsweredDate = today - DAY,
                nextReviewDate = today + 7 * DAY)
        }
        coEvery { progressRepository.getAllProgress() } returns (answeredToday + futureDue)
        coEvery { settingsRepository.getEffectiveDailyCount(any()) } returns 5
        coEvery { progressRepository.getAnsweredTodayCount(any()) } returns 5

        val questions = useCase.execute()

        val ids = questions.map { it.wordId }.toSet()
        // Sadece bugün cevaplanan kelimeler — vade-belirli kelimeler bile dahil değil
        assertEquals(setOf(1, 2, 3, 4, 5), ids)
    }

    @Test
    fun `quiz olusturulamayan kelimelerin lastShownDate i geri alinir`() = runTest {
        val progress = listOf(
            WordProgress(progressId = 1, wordId = 4, totalAttempts = 0, lastShownDate = 0L)
        )
        coEvery { progressRepository.getAllProgress() } returns progress
        // Sadece 1 farklı yanlış cevap → 4 şık oluşamaz → kelime düşer
        coEvery { wordRepository.getRandomWords(any(), 4) } returns listOf(words[1])

        val questions = useCase.execute()

        assertTrue(questions.isEmpty())
        // Önce işaretlendi (lastShownDate=startOfDay), sonra revert (lastShownDate=0L)
        coVerify {
            progressRepository.updateProgress(match {
                it.wordId == 4 && it.lastShownDate == 0L
            })
        }
    }
}
