package com.app.wordlearn.domain.usecase

import com.app.wordlearn.domain.model.QuizQuestion
import com.app.wordlearn.domain.model.WordProgress
import com.app.wordlearn.domain.repository.ProgressRepository
import com.app.wordlearn.domain.repository.SettingsRepository
import com.app.wordlearn.domain.repository.WordRepository
import javax.inject.Inject

class BuildDailyQuizUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend fun execute(): List<QuizQuestion> {
        val today = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = today
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        // Bugünün günlük kotasını kilitle:
        // Eğer bugün zaten quiz başlatıldıysa (önceden cevap verildiyse) o an ayarlanan
        // dailyCount'u kullan; böylece kullanıcı sayıyı değiştirince bugünü etkilemez.
        val dailyCount = settingsRepository.getEffectiveDailyCount(startOfDay)
        val answeredToday = progressRepository.getAnsweredTodayCount(startOfDay)
        val remainingCount = dailyCount - answeredToday

        val isPracticeMode = remainingCount <= 0
        val targetCount = if (isPracticeMode) dailyCount else remainingCount

        // 1. Tüm kelimeleri kontrol et
        val allWords = wordRepository.getAllWords()
        if (allWords.size < 4) return emptyList() // En az 4 kelime lazım (1 doğru + 3 yanlış şık)

        // 2. Her kelime için progress kaydını oluştur (yoksa)
        val existingProgress = progressRepository.getAllProgress()
        val existingWordIds = existingProgress.map { it.wordId }.toSet()

        val untrackedWords = allWords.filter { it.wordId !in existingWordIds }
        untrackedWords.forEach { word ->
            progressRepository.createProgress(
                WordProgress(
                    wordId = word.wordId,
                    correctStreak = 0,
                    reviewStage = 0,
                    totalCorrect = 0,
                    totalAttempts = 0,
                    nextReviewDate = 0L,
                    lastAnsweredDate = 0L,
                    isLearned = false
                )
            )
        }

        // 3. Tekrar vadesi gelen kelimeleri al (bugün cevaplanmış olanları hariç tut)
        val dueWords = progressRepository.getDueWords(today, startOfDay)

        // 4. Henüz hiç denenmemiş yeni kelimeler
        val newWords = progressRepository.getNewWords(targetCount)

        // 5. Birleştir ve sınırla
        var quizWords = (dueWords + newWords).distinctBy { it.wordId }.take(targetCount)

        // 6. Eğer hala kota dolmadıysa ve veritabanında başka öğrenilmemiş kelime varsa ekle
        if (quizWords.size < targetCount) {
            val existingIds = quizWords.map { it.wordId }.toSet()
            val extraWords = progressRepository.getAllProgress()
                .filter { !it.isLearned && it.wordId !in existingIds }
                .shuffled()
                .take(targetCount - quizWords.size)
            quizWords = quizWords + extraWords
        }

        // 6.5. Practice mode: Eğer quizWords boşsa, rastgele kelimeler getir
        if (quizWords.isEmpty() && isPracticeMode) {
            quizWords = progressRepository.getAllProgress()
                .shuffled()
                .take(targetCount)
        }

        if (quizWords.isEmpty()) return emptyList()

        // 7. Her kelime için 4 şıklı soru oluştur
        val questions = quizWords.mapNotNull { progress ->
            val word = wordRepository.getWordById(progress.wordId) ?: return@mapNotNull null

            // 3 rastgele yanlış şık al (garanti olması için 10 tane çekip filtreliyoruz)
            val wrongOptions = wordRepository.getRandomWords(10, word.wordId)
                .map { it.turWord }
                .filter { it != word.turWord }
                .distinct()
                .take(3)

            if (wrongOptions.size < 3) return@mapNotNull null

            // Doğru cevap + yanlış şıkları karıştır
            val options = (wrongOptions + word.turWord).distinct().shuffled()

            QuizQuestion(
                wordId = word.wordId,
                questionText = word.engWord,
                correctAnswer = word.turWord,
                options = options,
                picturePath = word.picturePath
            )
        }

        return questions.shuffled()
    }
}
