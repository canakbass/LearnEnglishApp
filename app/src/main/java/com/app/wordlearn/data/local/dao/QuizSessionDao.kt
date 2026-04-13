package com.app.wordlearn.data.local.dao

import androidx.room.*
import com.app.wordlearn.data.local.entity.QuizSessionEntity

@Dao
interface QuizSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: QuizSessionEntity): Long

    @Query("SELECT * FROM quiz_sessions WHERE sessionId = :id")
    suspend fun getSessionById(id: Int): QuizSessionEntity?

    @Query("SELECT * FROM quiz_sessions ORDER BY sessionDate DESC")
    suspend fun getAllSessions(): List<QuizSessionEntity>

    @Update
    suspend fun updateSession(session: QuizSessionEntity)
}
