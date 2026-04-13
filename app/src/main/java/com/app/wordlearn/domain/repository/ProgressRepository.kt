package com.app.wordlearn.domain.repository

import com.app.wordlearn.domain.model.WordProgress

interface ProgressRepository {
    suspend fun getProgress(wordId: Int): WordProgress?
    suspend fun getDueWords(today: Long): List<WordProgress>
    suspend fun getNewWords(count: Int): List<WordProgress>
    suspend fun updateProgress(progress: WordProgress)
    suspend fun getLearnedWords(): List<WordProgress>
    suspend fun markAsLearned(wordId: Int)
    suspend fun createProgress(progress: WordProgress)
    suspend fun getAllProgress(): List<WordProgress>
    suspend fun getProgressCount(): Int
}
