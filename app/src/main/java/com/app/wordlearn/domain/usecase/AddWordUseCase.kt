package com.app.wordlearn.domain.usecase

import com.app.wordlearn.domain.model.Word
import com.app.wordlearn.domain.model.WordProgress
import com.app.wordlearn.domain.model.WordSample
import com.app.wordlearn.domain.repository.ProgressRepository
import com.app.wordlearn.domain.repository.WordRepository
import com.app.wordlearn.data.local.dao.WordSampleDao
import javax.inject.Inject

class AddWordUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val progressRepository: ProgressRepository,
    private val wordSampleDao: WordSampleDao
) {
    suspend fun execute(word: Word): Long {
        return executeWithSamples(word, emptyList())
    }

    suspend fun executeWithSamples(word: Word, samples: List<String>): Long {
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

        // 3. Örnek cümleleri kaydet
        if (samples.isNotEmpty()) {
            val sampleEntities = samples.map { 
                com.app.wordlearn.data.local.entity.WordSampleEntity(
                    wordId = wordId.toInt(),
                    sentence = it
                )
            }
            wordSampleDao.insertSamples(sampleEntities)
        }

        return wordId
    }
}
