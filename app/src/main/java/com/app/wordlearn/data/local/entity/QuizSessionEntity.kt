package com.app.wordlearn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_sessions")
data class QuizSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Int = 0,
    val sessionDate: Long = System.currentTimeMillis(),
    val totalQuestions: Int = 0,
    val correctCount: Int = 0,
    val durationSeconds: Int = 0
)
