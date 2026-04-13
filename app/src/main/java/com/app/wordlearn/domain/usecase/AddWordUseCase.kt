package com.app.wordlearn.domain.usecase

import com.app.wordlearn.domain.model.Word
import com.app.wordlearn.domain.model.WordProgress
import com.app.wordlearn.domain.repository.ProgressRepository
import com.app.wordlearn.domain.repository.WordRepository
import javax.inject.Inject

class AddWordUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val progressRepository: ProgressRepository
) {
    suspend fun execute(word: Word): Long {
        // 1. Kelimeyi Room'a kaydet
        val wordId = wordRepository.addUserWord(word)

        // 2. Yeni kelime için progress kaydı oluştur
        progressRepository.createProgress(
            WordProgress(
                wordId = wordId.toInt(),
                correctStreak = 0,
                reviewStage = 0,
                nextReviewDate = System.currentTimeMillis(),
                lastAnsweredDate = 0L,
                isLearned = false
            )
        )

        return wordId
    }
}
