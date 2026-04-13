package com.app.wordlearn.domain.usecase

import com.app.wordlearn.domain.model.Word
import com.app.wordlearn.domain.model.WordProgress
import com.app.wordlearn.domain.repository.ProgressRepository
import com.app.wordlearn.domain.repository.WordRepository
import javax.inject.Inject

class SyncWordsUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val progressRepository: ProgressRepository
) {
    suspend fun execute(systemWords: List<Word>) {
        // 1. Mevcut kelime sayısını kontrol et
        val existingCount = wordRepository.getWordCount()

        if (existingCount == 0) {
            // İlk açılış: tüm sistem kelimelerini Room'a kaydet
            wordRepository.insertWords(systemWords)
        }

        // 2. Her kelime için progress kaydı oluştur (yoksa)
        val allWords = wordRepository.getAllWords()
        val now = System.currentTimeMillis()

        allWords.forEach { word ->
            val existingProgress = progressRepository.getProgress(word.wordId)
            if (existingProgress == null) {
                progressRepository.createProgress(
                    WordProgress(
                        wordId = word.wordId,
                        correctStreak = 0,
                        reviewStage = 0,
                        nextReviewDate = now,
                        lastAnsweredDate = 0L,
                        isLearned = false
                    )
                )
            }
        }
    }
}
