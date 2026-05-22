package com.app.wordlearn.domain.repository

import com.app.wordlearn.domain.model.WordProgress

interface ProgressRepository {
    suspend fun getProgress(wordId: Int): WordProgress?
    suspend fun getDueWords(today: Long, startOfDay: Long): List<WordProgress>
    suspend fun getNewWords(count: Int, startOfDay: Long): List<WordProgress>
    suspend fun getPendingNewWords(startOfDay: Long): List<WordProgress>
    suspend fun getPendingDueWords(startOfDay: Long): List<WordProgress>
    suspend fun updateProgress(progress: WordProgress)
    suspend fun getLearnedWords(): List<WordProgress>
    suspend fun markAsLearned(wordId: Int)
    suspend fun createProgress(progress: WordProgress)
    suspend fun getAllProgress(): List<WordProgress>
    suspend fun getProgressCount(): Int
    suspend fun getAnsweredTodayCount(startOfDay: Long): Int
}
