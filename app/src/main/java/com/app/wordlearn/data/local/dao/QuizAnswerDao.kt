package com.app.wordlearn.data.local.dao

import androidx.room.*
import com.app.wordlearn.data.local.entity.QuizAnswerEntity

@Dao
interface QuizAnswerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: QuizAnswerEntity)

    @Query("SELECT * FROM quiz_answers WHERE sessionId = :sessionId")
    suspend fun getAnswersBySessionId(sessionId: Int): List<QuizAnswerEntity>

    @Query("SELECT * FROM quiz_answers")
    suspend fun getAllAnswers(): List<QuizAnswerEntity>
}
