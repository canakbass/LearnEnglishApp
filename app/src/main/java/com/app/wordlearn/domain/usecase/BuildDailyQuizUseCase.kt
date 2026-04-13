package com.app.wordlearn.domain.usecase

import com.app.wordlearn.domain.model.QuizQuestion
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

        // 1. Vadesi gelen kelimeleri al
        val dueWords = progressRepository.getDueWords(today)

        // 2. Günlük yeni kelime sayısını al
        val dailyCount = settingsRepository.getDailyNewWordCount()

        // 3. Yeni kelimeleri al
        val newWords = progressRepository.getNewWords(dailyCount)

        // 4. İki listeyi birleştir
        val allProgressWords = (dueWords + newWords).distinctBy { it.wordId }

        if (allProgressWords.isEmpty()) return emptyList()

        // 5. Her kelime için 4 şıklı soru oluştur
        val questions = allProgressWords.mapNotNull { progress ->
            val word = wordRepository.getWordById(progress.wordId) ?: return@mapNotNull null

            // 3 rastgele yanlış şık al
            val wrongOptions = wordRepository.getRandomWords(3, word.wordId)
                .map { it.turWord }

            if (wrongOptions.size < 3) return@mapNotNull null

            // Doğru cevap + yanlış şıkları karıştır
            val options = (wrongOptions + word.turWord).shuffled()

            QuizQuestion(
                wordId = word.wordId,
                questionText = word.engWord,
                correctAnswer = word.turWord,
                options = options
            )
        }

        return questions.shuffled()
    }
}
