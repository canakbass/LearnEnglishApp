package com.app.wordlearn.data.local.dao

import androidx.room.*
import com.app.wordlearn.data.local.entity.WordProgressEntity

@Dao
interface WordProgressDao {
    @Query("SELECT * FROM word_progress WHERE wordId = :wordId")
    suspend fun getProgressByWordId(wordId: Int): WordProgressEntity?

    @Query("SELECT * FROM word_progress WHERE nextReviewDate <= :today AND isLearned = 0 AND totalAttempts > 0 AND lastAnsweredDate < :startOfDay AND lastShownDate < :startOfDay")
    suspend fun getDueWords(today: Long, startOfDay: Long): List<WordProgressEntity>

    @Query("SELECT * FROM word_progress WHERE totalAttempts = 0 AND isLearned = 0 AND lastShownDate < :startOfDay ORDER BY RANDOM() LIMIT :count")
    suspend fun getNewWords(count: Int, startOfDay: Long): List<WordProgressEntity>

    @Query("SELECT * FROM word_progress WHERE totalAttempts = 0 AND isLearned = 0 AND lastShownDate >= :startOfDay")
    suspend fun getPendingNewWords(startOfDay: Long): List<WordProgressEntity>

    @Query("SELECT * FROM word_progress WHERE totalAttempts > 0 AND isLearned = 0 AND lastAnsweredDate < :startOfDay AND lastShownDate >= :startOfDay")
    suspend fun getPendingDueWords(startOfDay: Long): List<WordProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: WordProgressEntity)

    @Update
    suspend fun updateProgress(progress: WordProgressEntity)

    @Query("SELECT * FROM word_progress WHERE isLearned = 1")
    suspend fun getLearnedWords(): List<WordProgressEntity>

    @Query("UPDATE word_progress SET isLearned = 1 WHERE wordId = :wordId")
    suspend fun markAsLearned(wordId: Int)

    @Query("SELECT * FROM word_progress")
    suspend fun getAllProgress(): List<WordProgressEntity>

    @Query("SELECT COUNT(*) FROM word_progress")
    suspend fun getProgressCount(): Int

    @Query("SELECT COUNT(*) FROM word_progress WHERE lastAnsweredDate >= :startOfDay AND totalAttempts > 0")
    suspend fun getAnsweredTodayCount(startOfDay: Long): Int

    @Query("DELETE FROM word_progress")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(progress: List<WordProgressEntity>)
}
