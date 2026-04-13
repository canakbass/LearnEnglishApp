package com.app.wordlearn.domain.model

data class QuizSession(
    val sessionId: Int = 0,
    val sessionDate: Long = System.currentTimeMillis(),
    val totalQuestions: Int = 0,
    val correctCount: Int = 0,
    val durationSeconds: Int = 0
)
