package com.app.wordlearn.domain.usecase

import android.util.Log
import com.app.wordlearn.domain.model.QuizQuestion
import com.app.wordlearn.domain.model.WordProgress
import com.app.wordlearn.domain.repository.ProgressRepository
import com.app.wordlearn.domain.repository.SettingsRepository
import com.app.wordlearn.domain.repository.WordRepository
import javax.inject.Inject

/**
 * Günlük quiz havuzunu hazırlar.
 *
 * Tasarım:
 *  - Tüm aday kelimeler **tek bir listede** toplanır → `distinctBy { wordId }` ile
 *    aynı kelimenin iki kez seçilmesi imkansız hâle gelir.
 *  - Bugün cevaplanmış (`lastAnsweredDate >= startOfDay`) ve öğrenilmiş
 *    (`isLearned == true`) kelimeler her durumda dışlanır.
 *  - Sıralama: önce bugün gösterilip cevaplanmamış kelimeler (devam mantığı),
 *    sonra dünden due olanlar, en son yeni kelimeler.
 *  - Practice mode quotanın dolması durumunda devreye girer; aynı filtre kuralları
 *    geçerlidir — bu yüzden bugün yapılan/yanlış yapılan kelime asla geri sorulmaz.
 */
class BuildDailyQuizUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend fun execute(): List<QuizQuestion> {
        val today = System.currentTimeMillis()
        val startOfDay = startOfDay(today)

        // --- 1. Sistem ihtiyaçları ---
        val allWords = wordRepository.getAllWords()
        if (allWords.size < 4) return emptyList()

        // Henüz progress kaydı olmayan kelimeler için iskelet kayıt oluştur.
        val existingProgress = progressRepository.getAllProgress()
        val existingWordIds = existingProgress.mapTo(mutableSetOf()) { it.wordId }
        allWords.asSequence()
            .filter { it.wordId !in existingWordIds }
            .forEach { word ->
                progressRepository.createProgress(
                    WordProgress(
                        wordId = word.wordId,
                        nextReviewDate = 0L,
                        lastAnsweredDate = 0L,
                        lastShownDate = 0L
                    )
                )
            }

        val dailyCount = settingsRepository.getEffectiveDailyCount(startOfDay)
        val answeredToday = progressRepository.getAnsweredTodayCount(startOfDay)
        val isPracticeMode = (dailyCount - answeredToday) <= 0
        val targetCount = if (isPracticeMode) answeredToday else (dailyCount - answeredToday)

        // --- 2. Aday havuzu seçimi ---
        // İki ayrı mod var; ProcessAnswerUseCase aynı gün içindeki tekrarları zaten
        // istatistiklere yansıtmıyor (lastAnsweredDate >= startOfDay → no-op).
        //
        //   NORMAL MOD (quota dolmadı):
        //     - Hard rule: bugün cevaplanmış veya öğrenilmiş kelime ASLA aday değildir.
        //     - Yeni kelime + bugün gösterilmiş ama cevaplanmamış (devam) + dünden due.
        //
        //   PRACTICE MOD (quota doldu):
        //     - Yeni kelime YOK — günlük kelime kotasını korumak için kritik.
        //     - Sadece bugün cevapladığı kelimeleri tekrar göster (rastgele sıra).
        //     - İstatistik etkilenmez (ProcessAnswer no-op).
        val allProgress = progressRepository.getAllProgress()
        val selected = if (isPracticeMode) {
            allProgress
                .filter { it.lastAnsweredDate >= startOfDay }
                .distinctBy { it.wordId }
                .shuffled()
                .take(targetCount)
        } else {
            val candidates = allProgress.filter {
                !it.isLearned && it.lastAnsweredDate < startOfDay
            }

            // Öncelik sırası (küçük rank önce):
            //  rank 0: pending — bugün gösterilmiş ama cevaplanmamış (devam)
            //  rank 1: due — dünden veya öncesinden due, henüz bugün gösterilmedi
            //  rank 2: new — hiç sorulmamış kelimeler
            //  rank 3: diğer öğrenilmemiş (en son tarihli)
            fun rank(p: WordProgress): Int = when {
                p.lastShownDate >= startOfDay -> 0
                p.totalAttempts > 0 && p.nextReviewDate <= today -> 1
                p.totalAttempts == 0 -> 2
                else -> 3
            }

            candidates
                .sortedWith(compareBy({ rank(it) }, { it.lastShownDate }, { it.wordId }))
                .distinctBy { it.wordId }
                .take(targetCount)
        }

        // --- 3. Yeni gösterilenleri işaretle (sadece bugün gösterilmemiş olanlar) ---
        val newlyMarked = selected.filter { it.lastShownDate < startOfDay }
        newlyMarked.forEach { p ->
            progressRepository.updateProgress(p.copy(lastShownDate = startOfDay))
        }

        if (selected.isEmpty()) {
            Log.d(TAG, "execute: havuzda uygun kelime yok (target=$targetCount, practice=$isPracticeMode)")
            return emptyList()
        }

        // --- 4. Sorulara dönüştür ---
        val questions = selected.mapNotNull { progress ->
            val word = wordRepository.getWordById(progress.wordId) ?: return@mapNotNull null

            val wrongOptions = wordRepository.getRandomWords(30, word.wordId)
                .map { it.turWord }
                .filter { it != word.turWord }
                .distinct()
                .take(3)

            if (wrongOptions.size < 3) return@mapNotNull null

            val options = (wrongOptions + word.turWord).distinct().shuffled()
            val samples = wordRepository.getWordSamples(word.wordId)
                .map { it.sentence.trim() }
                .filter { it.isNotEmpty() }
            QuizQuestion(
                wordId = word.wordId,
                questionText = word.engWord,
                correctAnswer = word.turWord,
                options = options,
                picturePath = word.picturePath,
                sampleSentences = samples
            )
        }

        // --- 5. Atlanan kelimelerin lastShownDate'ini geri al ---
        val questionWordIds = questions.mapTo(mutableSetOf()) { it.wordId }
        newlyMarked
            .filterNot { it.wordId in questionWordIds }
            .forEach { progressRepository.updateProgress(it) }

        // En sondaki defansif distinct — beklenmedik bir akış bile olsa
        // aynı kelime asla iki kez sorulmaz.
        val finalQuestions = questions.distinctBy { it.wordId }.shuffled()
        Log.d(
            TAG,
            "execute: target=$targetCount selected=${selected.size} " +
                "questions=${finalQuestions.size} answeredToday=$answeredToday practice=$isPracticeMode"
        )
        return finalQuestions
    }

    private fun startOfDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    companion object {
        private const val TAG = "BuildDailyQuiz"
    }
}
